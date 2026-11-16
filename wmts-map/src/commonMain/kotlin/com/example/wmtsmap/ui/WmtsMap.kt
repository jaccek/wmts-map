package com.example.wmtsmap.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.example.wmtsmap.controller.WmtsMapController
import com.example.wmtsmap.controller.WmtsMapControllerImpl
import com.example.wmtsmap.controller.WmtsMapState
import com.example.wmtsmap.model.WmtsMapConfig
import com.example.wmtsmap.model.WmtsMapError
import com.example.wmtsmap.projection.WmtsProjection
import com.example.wmtsmap.tile.WmtsTileCalculator
import kotlin.math.roundToInt

/**
 * Displays raster tiles from a WMTS service.
 *
 * A layer or tile matrix set must be specified in [config] when the service offers multiple
 * choices. Observe [WmtsMapController.state] to render service metadata and selection controls.
 */
@Composable
fun WmtsMap(
    config: WmtsMapConfig,
    modifier: Modifier = Modifier,
    onMapReady: (WmtsMapController) -> Unit = {},
    onMapError: (WmtsMapError) -> Unit = {},
) {
    val controller = remember(config) { WmtsMapControllerImpl(config) }
    val state by controller.state.collectAsState()
    val backgroundColor = MaterialTheme.colorScheme.surface

    DisposableEffect(controller) {
        onDispose(controller::close)
    }

    LaunchedEffect(controller) {
        controller.initialize()
    }

    LaunchedEffect(state.capabilities) {
        if (state.capabilities != null) onMapReady(controller)
    }

    LaunchedEffect(state.error) {
        state.error?.let(onMapError)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(backgroundColor) },
    ) {
        val density = LocalDensity.current
        val widthPixels = with(density) { maxWidth.toPx() }
        val heightPixels = with(density) { maxHeight.toPx() }

        LaunchedEffect(
            controller,
            widthPixels,
            heightPixels,
            state.position,
            state.zoom,
            state.selectedLayer,
            state.selectedTileMatrixSet,
        ) {
            controller.requestViewport(widthPixels, heightPixels)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Interactive WMTS map" }
                .pointerInput(controller) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        controller.panBy(pan.x, pan.y)
                        controller.zoomBy(zoomChange)
                    }
                },
        ) {
            drawTiles(state)
        }

        MapStatus(state, Modifier.align(Alignment.TopCenter).padding(12.dp))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTiles(state: WmtsMapState) {
    val crs = state.capabilities?.tileMatrixSets
        ?.firstOrNull { it.identifier == state.selectedTileMatrixSet }
        ?.supportedCrs
        ?: return
    val matrixSet = state.capabilities.tileMatrixSets.first { it.identifier == state.selectedTileMatrixSet }
    val resolution = WmtsTileCalculator.resolutionAtZoom(matrixSet, state.zoom, crs)
    val center = WmtsProjection.forward(state.position, crs)

    state.tiles.forEach { tile ->
        val left = (tile.bounds.left - center.easting) / resolution + size.width / 2.0
        val top = (center.northing - tile.bounds.top) / resolution + size.height / 2.0
        val right = (tile.bounds.right - center.easting) / resolution + size.width / 2.0
        val bottom = (center.northing - tile.bounds.bottom) / resolution + size.height / 2.0
        drawImage(
            image = tile.image,
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize((right - left).roundToInt(), (bottom - top).roundToInt()),
        )
    }
}

@Composable
private fun MapStatus(state: WmtsMapState, modifier: Modifier = Modifier) {
    Box(modifier) {
        when {
            state.error != null -> Text(
                text = state.error.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )

            state.isLoading && state.tiles.isEmpty() -> CircularProgressIndicator()
        }
    }
}
