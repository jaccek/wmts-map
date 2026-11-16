package com.example.wmtsmap.controller

import androidx.compose.ui.graphics.ImageBitmap
import com.example.wmtsmap.model.GeoCoordinate
import com.example.wmtsmap.model.ProjectedCoordinate
import com.example.wmtsmap.model.WmtsCapabilities
import com.example.wmtsmap.model.WmtsMapError
import com.example.wmtsmap.model.WmtsTileMatrixSet
import com.example.wmtsmap.tile.WmtsTileBounds
import kotlinx.coroutines.flow.StateFlow

/** Observable data rendered by [WmtsMap]. */
data class WmtsMapState(
    val isLoading: Boolean,
    val capabilities: WmtsCapabilities?,
    val selectedLayer: String?,
    val selectedTileMatrixSet: String?,
    val position: GeoCoordinate,
    val zoom: Double,
    val tiles: List<RenderedWmtsTile>,
    val error: WmtsMapError?,
)

/** A decoded tile and the projected bounds used to place it on the map. */
data class RenderedWmtsTile(
    val bounds: WmtsTileBounds,
    val image: ImageBitmap,
)

/** Controls navigation and selection for a [WmtsMap] instance. */
interface WmtsMapController {
    val state: StateFlow<WmtsMapState>

    fun moveTo(coordinate: GeoCoordinate, zoom: Double? = null)
    fun zoomIn()
    fun zoomOut()
    fun getCurrentPosition(): GeoCoordinate
    fun getCurrentZoom(): Double
    fun selectLayer(identifier: String)
    fun selectTileMatrixSet(identifier: String)
    fun reload()
}

internal data class MapViewport(
    val width: Float,
    val height: Float,
    val center: ProjectedCoordinate,
    val resolution: Double,
    val matrixSet: WmtsTileMatrixSet,
)
