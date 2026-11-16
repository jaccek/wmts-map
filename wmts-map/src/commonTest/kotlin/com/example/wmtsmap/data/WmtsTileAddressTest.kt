package com.example.wmtsmap.data

import com.example.wmtsmap.model.ProjectedCoordinate
import com.example.wmtsmap.model.WmtsCapabilities
import com.example.wmtsmap.model.WmtsLayer
import com.example.wmtsmap.model.WmtsMapException
import com.example.wmtsmap.model.WmtsResourceUrl
import com.example.wmtsmap.model.WmtsStyle
import com.example.wmtsmap.model.WmtsTileMatrixSet
import com.example.wmtsmap.tile.WmtsTileCoordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WmtsTileAddressTest {
    @Test
    fun `resolves REST placeholders`() {
        val url = WmtsTileAddress.resolve(
            capabilitiesUrl = "https://example.test?REQUEST=GetCapabilities",
            capabilities = capabilities,
            layer = layer.copy(
                resourceUrls = listOf(
                    WmtsResourceUrl(
                        template = "https://example.test/{Layer}/{Style}/{TileMatrixSet}/{TileMatrix}/{TileRow}/{TileCol}.png",
                        resourceType = "tile",
                        format = "image/png",
                    ),
                ),
            ),
            matrixSet = matrixSet,
            matrixIdentifier = "L0",
            tile = WmtsTileCoordinate(3, 5),
            style = "default",
            format = "image/png",
        )

        assertEquals("https://example.test/base/default/world/L0/5/3.png", url)
    }

    @Test
    fun `prefers a REST tile template on the capabilities host`() {
        val url = WmtsTileAddress.resolve(
            capabilitiesUrl = "https://maps.wien.gv.at/basemap/1.0.0/WMTSCapabilities.xml",
            capabilities = capabilities,
            layer = layer.copy(
                resourceUrls = listOf(
                    WmtsResourceUrl(
                        template = "https://maps1.wien.gv.at/basemap/{Layer}/{Style}/{TileMatrixSet}/{TileMatrix}/{TileRow}/{TileCol}.png",
                        resourceType = "tile",
                        format = "image/png",
                    ),
                    WmtsResourceUrl(
                        template = "https://maps.wien.gv.at/basemap/{Layer}/{Style}/{TileMatrixSet}/{TileMatrix}/{TileRow}/{TileCol}.png",
                        resourceType = "tile",
                        format = "image/png",
                    ),
                ),
            ),
            matrixSet = matrixSet,
            matrixIdentifier = "5",
            tile = WmtsTileCoordinate(17, 11),
            style = "default",
            format = "image/png",
        )

        assertEquals(
            "https://maps.wien.gv.at/basemap/base/default/world/5/11/17.png",
            url,
        )
    }

    @Test
    fun `resolves KVP parameters and refuses ambiguous selection`() {
        val url = WmtsTileAddress.resolve(
            capabilitiesUrl = "https://example.test/wmts",
            capabilities = capabilities,
            layer = layer.copy(resourceUrls = emptyList()),
            matrixSet = matrixSet,
            matrixIdentifier = "L0",
            tile = WmtsTileCoordinate(3, 5),
            style = "default",
            format = "image/png",
        )

        assertEquals("https://example.test/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=base&STYLE=default&FORMAT=image%2Fpng&TILEMATRIXSET=world&TILEMATRIX=L0&TILEROW=5&TILECOL=3", url)
        assertFailsWith<WmtsMapException> {
            WmtsTileAddress.selectLayer(capabilities.copy(layers = listOf(layer, layer.copy(identifier = "second"))), null)
        }
    }

    private companion object {
        val capabilities = WmtsCapabilities(
            version = "1.0.0",
            serviceIdentification = null,
            serviceProvider = null,
            layers = emptyList(),
            tileMatrixSets = emptyList(),
            getTileEndpoints = listOf("https://example.test/wmts"),
        )
        val layer = WmtsLayer(
            identifier = "base",
            title = null,
            abstract = null,
            styles = listOf(WmtsStyle("default", null, true)),
            formats = listOf("image/png"),
            tileMatrixSetLinks = listOf("world"),
            resourceUrls = emptyList(),
        )
        val matrixSet = WmtsTileMatrixSet(
            identifier = "world",
            supportedCrs = "EPSG:3857",
            tileMatrices = emptyList(),
        )
    }
}
