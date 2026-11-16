package com.example.wmtsmap.parser

import com.example.wmtsmap.model.WmtsMapException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WmtsCapabilitiesParserTest {
    @Test
    fun `parses layers styles resources matrix sets and KVP endpoint`() {
        val capabilities = WmtsCapabilitiesParser.parse(CAPABILITIES_XML)

        assertEquals("1.0.0", capabilities.version)
        assertEquals("Example WMTS", capabilities.serviceIdentification?.title)
        assertEquals("Example provider", capabilities.serviceProvider?.name)
        assertEquals(listOf("https://example.test/wmts"), capabilities.getTileEndpoints)
        assertEquals(listOf("base", "labels"), capabilities.layers.map { it.identifier })
        assertEquals(listOf("GoogleMapsCompatible", "Geographic"), capabilities.tileMatrixSets.map { it.identifier })
        assertEquals("default", capabilities.layers.first().styles.single().identifier)
        assertTrue(capabilities.layers.first().styles.single().isDefault)
        assertEquals(
            "https://example.test/{TileMatrix}/{TileRow}/{TileCol}.png",
            capabilities.layers.first().resourceUrls.single().template,
        )
        assertEquals("WorldCRS84Quad", capabilities.tileMatrixSets.last().tileMatrices.last().identifier)
        assertEquals(512, capabilities.tileMatrixSets.first().tileMatrices.first().tileWidth)
    }

    @Test
    fun `accepts optional metadata being absent`() {
        val minimal = CAPABILITIES_XML
            .replace("<ows:ServiceIdentification><ows:Title>Example WMTS</ows:Title></ows:ServiceIdentification>", "")
            .replace("<ows:ServiceProvider><ows:ProviderName>Example provider</ows:ProviderName></ows:ServiceProvider>", "")
            .replace("<ows:Abstract>Base layer</ows:Abstract>", "")

        val capabilities = WmtsCapabilitiesParser.parse(minimal)

        assertEquals(null, capabilities.serviceIdentification)
        assertEquals(null, capabilities.serviceProvider)
        assertEquals(null, capabilities.layers.first().abstract)
        assertFalse(capabilities.layers.first().styles.isEmpty())
    }

    @Test
    fun `rejects invalid XML and unsupported version`() {
        val malformed = assertIs<WmtsMapException>(
            runCatching { WmtsCapabilitiesParser.parse("<Capabilities>") }.exceptionOrNull(),
        )
        assertTrue(malformed.message.orEmpty().contains("valid XML"))

        val unsupported = assertIs<WmtsMapException>(
            runCatching { WmtsCapabilitiesParser.parse(CAPABILITIES_XML.replace("1.0.0", "2.0.0")) }
                .exceptionOrNull(),
        )
        assertTrue(unsupported.message.orEmpty().contains("Unsupported WMTS"))
    }

    private companion object {
        val CAPABILITIES_XML = """
            <Capabilities xmlns="http://www.opengis.net/wmts/1.0"
                xmlns:ows="http://www.opengis.net/ows/1.1"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                version="1.0.0">
              <ows:ServiceIdentification><ows:Title>Example WMTS</ows:Title></ows:ServiceIdentification>
              <ows:ServiceProvider><ows:ProviderName>Example provider</ows:ProviderName></ows:ServiceProvider>
              <ows:OperationsMetadata>
                <ows:Operation name="GetTile">
                  <ows:DCP><ows:HTTP><ows:Get xlink:href="https://example.test/wmts"/></ows:HTTP></ows:DCP>
                </ows:Operation>
              </ows:OperationsMetadata>
              <Contents>
                <Layer>
                  <ows:Title>Base</ows:Title>
                  <ows:Abstract>Base layer</ows:Abstract>
                  <ows:Identifier>base</ows:Identifier>
                  <Style isDefault="true"><ows:Title>Default</ows:Title><ows:Identifier>default</ows:Identifier></Style>
                  <Format>image/png</Format>
                  <TileMatrixSetLink><TileMatrixSet>GoogleMapsCompatible</TileMatrixSet></TileMatrixSetLink>
                  <ResourceURL format="image/png" resourceType="tile"
                    template="https://example.test/{TileMatrix}/{TileRow}/{TileCol}.png"/>
                </Layer>
                <Layer>
                  <ows:Identifier>labels</ows:Identifier>
                  <Format>image/jpeg</Format>
                  <TileMatrixSetLink><TileMatrixSet>Geographic</TileMatrixSet></TileMatrixSetLink>
                </Layer>
                <TileMatrixSet>
                  <ows:Identifier>GoogleMapsCompatible</ows:Identifier>
                  <ows:SupportedCRS>urn:ogc:def:crs:EPSG::3857</ows:SupportedCRS>
                  <TileMatrix>
                    <ows:Identifier>0</ows:Identifier>
                    <ScaleDenominator>559082264.0287178</ScaleDenominator>
                    <TopLeftCorner>-20037508.3427892 20037508.3427892</TopLeftCorner>
                    <TileWidth>512</TileWidth><TileHeight>256</TileHeight>
                    <MatrixWidth>1</MatrixWidth><MatrixHeight>1</MatrixHeight>
                  </TileMatrix>
                </TileMatrixSet>
                <TileMatrixSet>
                  <ows:Identifier>Geographic</ows:Identifier>
                  <ows:SupportedCRS>urn:ogc:def:crs:OGC:1.3:CRS84</ows:SupportedCRS>
                  <TileMatrix>
                    <ows:Identifier>WorldCRS84Quad</ows:Identifier>
                    <ScaleDenominator>279541132.0143589</ScaleDenominator>
                    <TopLeftCorner>90 -180</TopLeftCorner>
                    <TileWidth>256</TileWidth><TileHeight>256</TileHeight>
                    <MatrixWidth>2</MatrixWidth><MatrixHeight>1</MatrixHeight>
                  </TileMatrix>
                </TileMatrixSet>
              </Contents>
            </Capabilities>
        """.trimIndent()
    }
}
