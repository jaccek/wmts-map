package com.example.wmtsmap.tile

import com.example.wmtsmap.model.ProjectedCoordinate
import com.example.wmtsmap.model.GeoCoordinate
import com.example.wmtsmap.model.WmtsTileMatrix
import com.example.wmtsmap.model.WmtsTileMatrixSet
import com.example.wmtsmap.projection.WmtsProjection
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

/** A column and row within a WMTS tile matrix. */
data class WmtsTileCoordinate(val column: Long, val row: Long)

/** The projected bounds of a tile. */
data class WmtsTileBounds(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double,
)

/** Metadata-driven tile grid calculations for a WMTS matrix set. */
object WmtsTileCalculator {
    private const val OGC_PIXEL_SIZE_METERS = 0.00028

    /** Returns the WGS 84 center of the first matrix's advertised coverage extent. */
    fun coverageCenter(matrixSet: WmtsTileMatrixSet): GeoCoordinate {
        val matrix = matrixSet.tileMatrices.firstOrNull()
            ?: throw IllegalArgumentException("The tile matrix set must contain a tile matrix.")
        val crs = matrixSet.supportedCrs
        val resolution = resolution(matrix, crs)
        val topLeft = WmtsProjection.normalizeTopLeftCorner(matrix.topLeftCorner, crs)
        val center = ProjectedCoordinate(
            easting = topLeft.easting + matrix.matrixWidth.toDouble() * matrix.tileWidth * resolution / 2.0,
            northing = topLeft.northing - matrix.matrixHeight.toDouble() * matrix.tileHeight * resolution / 2.0,
        )
        return WmtsProjection.inverse(center, crs)
    }

    /** Returns the projected units per pixel for [matrix]. */
    fun resolution(matrix: WmtsTileMatrix, crs: String): Double =
        matrix.scaleDenominator * OGC_PIXEL_SIZE_METERS / WmtsProjection.metersPerUnit(crs)

    /** Interpolates map resolution between the actual WMTS scale denominators at a fractional zoom. */
    fun resolutionAtZoom(matrixSet: WmtsTileMatrixSet, zoom: Double, crs: String): Double {
        require(matrixSet.tileMatrices.isNotEmpty())
        require(zoom.isFinite())
        val ordered = matrixSet.tileMatrices.sortedByDescending { it.scaleDenominator }
        val clampedZoom = zoom.coerceIn(0.0, ordered.lastIndex.toDouble())
        val lowerIndex = clampedZoom.toInt()
        val upperIndex = (lowerIndex + 1).coerceAtMost(ordered.lastIndex)
        val fraction = clampedZoom - lowerIndex
        val lowerResolution = resolution(ordered[lowerIndex], crs)
        val upperResolution = resolution(ordered[upperIndex], crs)
        return lowerResolution * (upperResolution / lowerResolution).pow(fraction)
    }

    /** Chooses the nearest matrix resolution for a continuous zoom level. */
    fun matrixForZoom(matrixSet: WmtsTileMatrixSet, zoom: Double): WmtsTileMatrix {
        require(matrixSet.tileMatrices.isNotEmpty())
        require(zoom.isFinite())
        val ordered = matrixSet.tileMatrices.sortedByDescending { it.scaleDenominator }
        val index = zoom.roundToInt().coerceIn(0, ordered.lastIndex)
        return ordered[index]
    }

    /** Converts a projected position into a matrix column and row. */
    fun tileAt(
        coordinate: ProjectedCoordinate,
        matrix: WmtsTileMatrix,
        crs: String,
    ): WmtsTileCoordinate {
        val resolution = resolution(matrix, crs)
        val topLeft = WmtsProjection.normalizeTopLeftCorner(matrix.topLeftCorner, crs)
        return WmtsTileCoordinate(
            column = floor((coordinate.easting - topLeft.easting) / (resolution * matrix.tileWidth)).toLong(),
            row = floor((topLeft.northing - coordinate.northing) / (resolution * matrix.tileHeight)).toLong(),
        )
    }

    /** Returns projected bounds for [tile], respecting tile size and matrix origin. */
    fun bounds(
        tile: WmtsTileCoordinate,
        matrix: WmtsTileMatrix,
        crs: String,
    ): WmtsTileBounds {
        val resolution = resolution(matrix, crs)
        val topLeft = WmtsProjection.normalizeTopLeftCorner(matrix.topLeftCorner, crs)
        val left = topLeft.easting + tile.column * matrix.tileWidth * resolution
        val top = topLeft.northing - tile.row * matrix.tileHeight * resolution
        return WmtsTileBounds(
            left = left,
            top = top,
            right = left + matrix.tileWidth * resolution,
            bottom = top - matrix.tileHeight * resolution,
        )
    }
}
