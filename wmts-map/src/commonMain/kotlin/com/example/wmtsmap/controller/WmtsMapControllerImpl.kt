package com.example.wmtsmap.controller

import androidx.compose.ui.graphics.ImageBitmap
import com.example.wmtsmap.data.KtorWmtsRepository
import com.example.wmtsmap.data.WmtsTileAddress
import com.example.wmtsmap.model.GeoCoordinate
import com.example.wmtsmap.model.WmtsLayer
import com.example.wmtsmap.model.WmtsMapConfig
import com.example.wmtsmap.model.WmtsMapError
import com.example.wmtsmap.model.WmtsMapException
import com.example.wmtsmap.model.WmtsStyle
import com.example.wmtsmap.model.WmtsTileMatrixSet
import com.example.wmtsmap.projection.WmtsProjection
import com.example.wmtsmap.tile.WmtsTileCalculator
import com.example.wmtsmap.tile.WmtsTileCoordinate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.ln

internal class WmtsMapControllerImpl(
    private val config: WmtsMapConfig,
) : WmtsMapController, AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val repository = KtorWmtsRepository(config)
    private val mutableState = MutableStateFlow(
        WmtsMapState(
            isLoading = false,
            capabilities = null,
            selectedLayer = config.layer,
            selectedTileMatrixSet = config.tileMatrixSet,
            position = config.initialPosition,
            zoom = config.initialZoom,
            tiles = emptyList(),
            error = null,
        ),
    )
    override val state: StateFlow<WmtsMapState> = mutableState.asStateFlow()
    private var selectedLayer: WmtsLayer? = null
    private var selectedMatrixSet: WmtsTileMatrixSet? = null
    private var tileJob: Job? = null

    fun initialize() {
        reload()
    }

    override fun reload() {
        scope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, error = null)
            try {
                val capabilities = repository.loadCapabilities()
                val layer = config.layer?.let { WmtsTileAddress.selectLayer(capabilities, it) }
                    ?: capabilities.layers.singleOrNull()
                val matrixSet = layer?.let { selectedLayer ->
                    config.tileMatrixSet?.let { identifier ->
                        WmtsTileAddress.selectMatrixSet(capabilities, selectedLayer, identifier)
                    } ?: run {
                        val compatible = capabilities.tileMatrixSets.filter {
                            it.identifier in selectedLayer.tileMatrixSetLinks
                        }.ifEmpty { capabilities.tileMatrixSets }
                        compatible.singleOrNull()
                    }
                }
                if (layer != null) selectStyle(layer, config.style)
                selectedLayer = layer
                selectedMatrixSet = matrixSet
                val zoom = matrixSet?.let { clampZoom(mutableState.value.zoom, it) } ?: mutableState.value.zoom
                mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    capabilities = capabilities,
                    selectedLayer = layer?.identifier,
                    selectedTileMatrixSet = matrixSet?.identifier,
                    zoom = zoom,
                    error = null,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: WmtsMapException) {
                mutableState.value = mutableState.value.copy(isLoading = false, error = exception.error)
            } catch (exception: Exception) {
                mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    error = WmtsMapError.Network("Unable to initialize the WMTS map."),
                )
            }
        }
    }

    override fun moveTo(coordinate: GeoCoordinate, zoom: Double?) {
        require(coordinate.latitude in -90.0..90.0)
        require(coordinate.longitude in -180.0..180.0)
        val current = mutableState.value
        mutableState.value = current.copy(
            position = coordinate,
            zoom = zoom?.let { selectedMatrixSet?.let { matrixSet -> clampZoom(it, matrixSet) } ?: it }
                ?: current.zoom,
            error = null,
        )
    }

    override fun zoomIn() {
        zoomBy(1.0)
    }

    override fun zoomOut() {
        zoomBy(-1.0)
    }

    override fun getCurrentPosition(): GeoCoordinate = mutableState.value.position

    override fun getCurrentZoom(): Double = mutableState.value.zoom

    override fun selectLayer(identifier: String) {
        val capabilities = mutableState.value.capabilities ?: return
        val layer = capabilities.layers.firstOrNull { it.identifier == identifier }
        if (layer == null) {
            setError(WmtsMapError.InvalidCapabilities("WMTS layer '$identifier' was not found."))
            return
        }
        try {
            val matrixSet = config.tileMatrixSet?.let {
                WmtsTileAddress.selectMatrixSet(capabilities, layer, it)
            } ?: run {
                val compatible = capabilities.tileMatrixSets.filter {
                    it.identifier in layer.tileMatrixSetLinks
                }.ifEmpty { capabilities.tileMatrixSets }
                compatible.singleOrNull()
            }
            selectStyle(layer, null)
            selectedLayer = layer
            selectedMatrixSet = matrixSet
            mutableState.value = mutableState.value.copy(
                selectedLayer = layer.identifier,
                selectedTileMatrixSet = matrixSet?.identifier,
                zoom = matrixSet?.let { clampZoom(mutableState.value.zoom, it) } ?: mutableState.value.zoom,
                tiles = emptyList(),
                error = null,
            )
        } catch (exception: WmtsMapException) {
            setError(exception.error)
        }
    }

    override fun selectTileMatrixSet(identifier: String) {
        val capabilities = mutableState.value.capabilities ?: return
        val layer = selectedLayer ?: return
        try {
            val matrixSet = WmtsTileAddress.selectMatrixSet(capabilities, layer, identifier)
            selectedMatrixSet = matrixSet
            mutableState.value = mutableState.value.copy(
                selectedTileMatrixSet = matrixSet.identifier,
                zoom = clampZoom(mutableState.value.zoom, matrixSet),
                tiles = emptyList(),
                error = null,
            )
        } catch (exception: WmtsMapException) {
            setError(exception.error)
        }
    }

    fun panBy(deltaX: Float, deltaY: Float) {
        val viewport = createViewport(1f, 1f) ?: return
        val projected = viewport.center.copy(
            easting = viewport.center.easting - deltaX * viewport.resolution,
            northing = viewport.center.northing + deltaY * viewport.resolution,
        )
        moveTo(WmtsProjection.inverse(projected, viewport.matrixSet.supportedCrs))
    }

    fun zoomBy(scale: Float) {
        if (!scale.isFinite() || scale <= 0f) return
        zoomBy(ln(scale.toDouble()) / LN_2)
    }

    fun requestViewport(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        tileJob?.cancel()
        tileJob = scope.launch {
            try {
                val viewport = createViewport(width, height) ?: return@launch
                loadTiles(viewport)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: WmtsMapException) {
                setError(exception.error)
            } catch (exception: Exception) {
                setError(WmtsMapError.Network("Unable to calculate WMTS tiles for this map viewport."))
            }
        }
    }

    override fun close() {
        tileJob?.cancel()
        repository.close()
        scope.cancel()
    }

    private fun zoomBy(delta: Double) {
        val matrixSet = selectedMatrixSet ?: return
        val current = mutableState.value
        val nextZoom = clampZoom(current.zoom + delta, matrixSet)
        if (nextZoom != current.zoom) mutableState.value = current.copy(zoom = nextZoom)
    }

    private suspend fun loadTiles(viewport: MapViewport) {
        val layer = selectedLayer ?: return
        val format = selectFormat(layer)
        val matrix = WmtsTileCalculator.matrixForZoom(viewport.matrixSet, mutableState.value.zoom)
        val left = viewport.center.easting - viewport.width * viewport.resolution / 2.0
        val top = viewport.center.northing + viewport.height * viewport.resolution / 2.0
        val firstTile = WmtsTileCalculator.tileAt(
            com.example.wmtsmap.model.ProjectedCoordinate(left, top),
            matrix,
            viewport.matrixSet.supportedCrs,
        )
        val lastTile = WmtsTileCalculator.tileAt(
            com.example.wmtsmap.model.ProjectedCoordinate(
                viewport.center.easting + viewport.width * viewport.resolution / 2.0,
                viewport.center.northing - viewport.height * viewport.resolution / 2.0,
            ),
            matrix,
            viewport.matrixSet.supportedCrs,
        )
        val minColumn = minOf(firstTile.column, lastTile.column).coerceAtLeast(0)
        val maxColumn = maxOf(firstTile.column, lastTile.column).coerceAtMost(matrix.matrixWidth - 1)
        val minRow = minOf(firstTile.row, lastTile.row).coerceAtLeast(0)
        val maxRow = maxOf(firstTile.row, lastTile.row).coerceAtMost(matrix.matrixHeight - 1)
        val columnCount = maxColumn - minColumn + 1
        val rowCount = maxRow - minRow + 1
        if (columnCount <= 0L || rowCount <= 0L) {
            mutableState.value = mutableState.value.copy(tiles = emptyList(), isLoading = false)
            return
        }
        if (columnCount > MAX_TILES_PER_VIEWPORT / rowCount) {
            setError(WmtsMapError.InvalidCapabilities("The visible viewport requires too many WMTS tiles."))
            return
        }

        mutableState.value = mutableState.value.copy(isLoading = true, error = null)
        val style = selectStyle(layer, config.style).identifier
        val result = supervisorScope {
            (minRow..maxRow).flatMap { row ->
                (minColumn..maxColumn).map { column ->
                    async {
                        val coordinate = WmtsTileCoordinate(column, row)
                        val url = WmtsTileAddress.resolve(
                            capabilitiesUrl = config.capabilitiesUrl,
                            capabilities = mutableState.value.capabilities ?: return@async null,
                            layer = layer,
                            matrixSet = viewport.matrixSet,
                            matrixIdentifier = matrix.identifier,
                            tile = coordinate,
                            style = style,
                            format = format,
                        )
                        try {
                            val bytes = repository.loadTile(url)
                            RenderedWmtsTile(
                                bounds = WmtsTileCalculator.bounds(coordinate, matrix, viewport.matrixSet.supportedCrs),
                                image = decodeWmtsTile(bytes),
                            )
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (exception: WmtsMapException) {
                            setError(exception.error)
                            null
                        } catch (exception: Exception) {
                            setError(WmtsMapError.Network("A WMTS raster tile could not be decoded."))
                            null
                        }
                    }
                }
            }.awaitAll().filterNotNull()
        }
        mutableState.value = mutableState.value.copy(tiles = result, isLoading = false)
    }

    private fun createViewport(width: Float, height: Float): MapViewport? {
        val matrixSet = selectedMatrixSet ?: return null
        val center = WmtsProjection.forward(mutableState.value.position, matrixSet.supportedCrs)
        val resolution = WmtsTileCalculator.resolutionAtZoom(matrixSet, mutableState.value.zoom, matrixSet.supportedCrs)
        return MapViewport(width, height, center, resolution, matrixSet)
    }

    private fun clampZoom(zoom: Double, matrixSet: WmtsTileMatrixSet): Double {
        val lastZoom = matrixSet.tileMatrices.lastIndex.toDouble()
        val minZoom = config.minZoom?.coerceIn(0.0, lastZoom) ?: 0.0
        val maxZoom = config.maxZoom?.coerceIn(minZoom, lastZoom) ?: lastZoom
        return zoom.coerceIn(minZoom, maxZoom)
    }

    private fun selectStyle(layer: WmtsLayer, requested: String?): WmtsStyle {
        val selected = requested?.let { style -> layer.styles.firstOrNull { it.identifier == style } }
        if (selected != null) return selected
        if (requested != null) {
            throw WmtsMapException(
                WmtsMapError.InvalidCapabilities("Style '$requested' is not available for layer '${layer.identifier}'."),
            )
        }
        return layer.styles.firstOrNull { it.isDefault }
            ?: layer.styles.singleOrNull()
            ?: layer.styles.firstOrNull()
            ?: throw WmtsMapException(
                WmtsMapError.InvalidCapabilities("Select one of the ${layer.styles.size} styles for layer '${layer.identifier}'."),
            )
    }

    private fun selectFormat(layer: WmtsLayer): String =
        layer.formats.firstOrNull { it.equals("image/png", ignoreCase = true) }
            ?: layer.formats.firstOrNull()
            ?: throw WmtsMapException(
                WmtsMapError.InvalidCapabilities("Layer '${layer.identifier}' has no supported image format."),
            )

    private fun setError(error: WmtsMapError) {
        mutableState.value = mutableState.value.copy(error = error, isLoading = false)
    }

    private companion object {
        const val MAX_TILES_PER_VIEWPORT = 256L
        val LN_2 = ln(2.0)
    }
}

internal expect fun decodeWmtsTile(bytes: ByteArray): ImageBitmap
