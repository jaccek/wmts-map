package com.example.wmtsmap.tile

import com.example.wmtsmap.model.ProjectedCoordinate
import com.example.wmtsmap.model.WmtsTileMatrix
import com.example.wmtsmap.model.WmtsTileMatrixSet
import com.example.wmtsmap.projection.WmtsProjection
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

class WmtsTileCalculatorTest {
    @Test
    fun `derives center from the first matrix extent`() {
        val matrixSet = WmtsTileMatrixSet(
            identifier = "custom",
            supportedCrs = "EPSG:3857",
            tileMatrices = listOf(
                WmtsTileMatrix(
                    identifier = "first",
                    scaleDenominator = 10_000.0,
                    topLeftCorner = ProjectedCoordinate(-100.0, 200.0),
                    tileWidth = 10,
                    tileHeight = 20,
                    matrixWidth = 2,
                    matrixHeight = 4,
                ),
                WmtsTileMatrix(
                    identifier = "later",
                    scaleDenominator = 1_000.0,
                    topLeftCorner = ProjectedCoordinate(0.0, 0.0),
                    tileWidth = 256,
                    tileHeight = 256,
                    matrixWidth = 1,
                    matrixHeight = 1,
                ),
            ),
        )

        val projectedCenter = WmtsProjection.forward(
            WmtsTileCalculator.coverageCenter(matrixSet),
            matrixSet.supportedCrs,
        )

        assertEquals(-72.0, projectedCenter.easting, 1e-8)
        assertEquals(88.0, projectedCenter.northing, 1e-8)
    }

    @Test
    fun `calculates tile index and bounds using actual dimensions and nonzero origin`() {
        val matrix = WmtsTileMatrix(
            identifier = "custom",
            scaleDenominator = 10_000.0,
            topLeftCorner = ProjectedCoordinate(1_000.0, 2_000.0),
            tileWidth = 512,
            tileHeight = 128,
            matrixWidth = 4,
            matrixHeight = 3,
        )
        val resolution = WmtsTileCalculator.resolution(matrix, "EPSG:3857")
        val coordinate = ProjectedCoordinate(
            1_000.0 + 512 * resolution * 1.4,
            2_000.0 - 128 * resolution * 2.2,
        )

        assertEquals(WmtsTileCoordinate(1, 2), WmtsTileCalculator.tileAt(coordinate, matrix, "EPSG:3857"))
        val expectedBounds = WmtsTileBounds(
            left = 1_000.0 + 512 * resolution,
            top = 2_000.0 - 2 * 128 * resolution,
            right = 1_000.0 + 2 * 512 * resolution,
            bottom = 2_000.0 - 3 * 128 * resolution,
        )
        val actualBounds = WmtsTileCalculator.bounds(WmtsTileCoordinate(1, 2), matrix, "EPSG:3857")
        assertEquals(expectedBounds.left, actualBounds.left, 1e-9)
        assertEquals(expectedBounds.top, actualBounds.top, 1e-9)
        assertEquals(expectedBounds.right, actualBounds.right, 1e-9)
        assertEquals(expectedBounds.bottom, actualBounds.bottom, 1e-9)
    }

    @Test
    fun `selects a matrix by zoom and normalizes EPSG 4326 top left axis order`() {
        val matrixSet = WmtsTileMatrixSet(
            identifier = "geographic",
            supportedCrs = "EPSG:4326",
            tileMatrices = listOf(
                WmtsTileMatrix("coarse", 279_541_132.0143589, ProjectedCoordinate(90.0, -180.0), 256, 256, 2, 1),
                WmtsTileMatrix("fine", 139_770_566.00717944, ProjectedCoordinate(90.0, -180.0), 512, 128, 8, 2),
            ),
        )

        assertEquals("fine", WmtsTileCalculator.matrixForZoom(matrixSet, 10.0).identifier)
        assertEquals(
            WmtsTileCoordinate(0, 0),
            WmtsTileCalculator.tileAt(ProjectedCoordinate(-179.0, 89.0), matrixSet.tileMatrices.first(), "EPSG:4326"),
        )
    }

    @Test
    fun `interpolates zoom using advertised matrix resolutions`() {
        val matrixSet = WmtsTileMatrixSet(
            identifier = "irregular",
            supportedCrs = "EPSG:3857",
            tileMatrices = listOf(
                WmtsTileMatrix("coarse", 1_000.0, ProjectedCoordinate(0.0, 0.0), 256, 256, 1, 1),
                WmtsTileMatrix("fine", 100.0, ProjectedCoordinate(0.0, 0.0), 256, 256, 1, 1),
            ),
        )

        assertEquals(
            0.00028 * sqrt(100_000.0),
            WmtsTileCalculator.resolutionAtZoom(matrixSet, 0.5, "EPSG:3857"),
            absoluteTolerance = 1e-12,
        )
    }
}
