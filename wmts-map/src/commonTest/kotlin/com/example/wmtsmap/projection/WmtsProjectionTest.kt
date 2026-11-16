package com.example.wmtsmap.projection

import com.example.wmtsmap.model.GeoCoordinate
import com.example.wmtsmap.model.ProjectedCoordinate
import com.example.wmtsmap.model.WmtsMapException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WmtsProjectionTest {
    @Test
    fun `round trips WGS84 and Web Mercator`() {
        val position = GeoCoordinate(latitude = 51.1, longitude = 17.0)

        val projected = WmtsProjection.forward(position, "urn:ogc:def:crs:EPSG::3857")
        val restored = WmtsProjection.inverse(projected, "EPSG:3857")

        assertEquals(position.latitude, restored.latitude, 1e-8)
        assertEquals(position.longitude, restored.longitude, 1e-8)
    }

    @Test
    fun `round trips WGS84 and Poland CS92`() {
        val position = GeoCoordinate(latitude = 51.1, longitude = 17.0)

        val projected = WmtsProjection.forward(position, "EPSG:2180")
        val restored = WmtsProjection.inverse(projected, "EPSG:2180")

        assertEquals(position.latitude, restored.latitude, 1e-6)
        assertEquals(position.longitude, restored.longitude, 1e-6)
    }

    @Test
    fun `uses Poland CS92 central meridian false easting and false northing`() {
        val projected = WmtsProjection.forward(GeoCoordinate(0.0, 19.0), "EPSG:2180")

        assertEquals(500_000.0, projected.easting, 1e-6)
        assertEquals(-5_300_000.0, projected.northing, 1e-6)
    }

    @Test
    fun `handles EPSG 4326 axis order and rejects unknown CRS`() {
        assertEquals(
            ProjectedCoordinate(easting = 17.0, northing = 51.1),
            WmtsProjection.forward(GeoCoordinate(51.1, 17.0), "EPSG:4326"),
        )
        assertEquals(
            ProjectedCoordinate(easting = -180.0, northing = 90.0),
            WmtsProjection.normalizeTopLeftCorner(ProjectedCoordinate(90.0, -180.0), "EPSG:4326"),
        )
        assertFailsWith<WmtsMapException> {
            WmtsProjection.forward(GeoCoordinate(0.0, 0.0), "EPSG:9999")
        }
    }
}
