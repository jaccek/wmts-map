package com.example.wmtsmap.data

import com.example.wmtsmap.model.WmtsMapConfig
import com.example.wmtsmap.model.WmtsMapException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KtorWmtsRepositoryTest {
    @Test
    fun `downloads and parses capabilities using configured headers`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("example-token", request.headers["X-Test-Header"])
            respond(
                content = ByteReadChannel(CAPABILITIES_XML),
                status = HttpStatusCode.OK,
                headers = io.ktor.http.headersOf(HttpHeaders.ContentType, "application/xml"),
            )
        }
        val repository = KtorWmtsRepository(
            config = WmtsMapConfig(
                capabilitiesUrl = "https://example.test/wmts?SERVICE=WMTS&REQUEST=GetCapabilities",
                requestHeaders = mapOf("X-Test-Header" to "example-token"),
            ),
            engine = engine,
        )

        try {
            assertEquals("layer", repository.loadCapabilities().layers.single().identifier)
        } finally {
            repository.close()
        }
    }

    @Test
    fun `coalesces duplicate tile requests`() = runTest {
        val requestStarted = CompletableDeferred<Unit>()
        val allowResponse = CompletableDeferred<Unit>()
        var requestCount = 0
        val engine = MockEngine {
            requestCount++
            requestStarted.complete(Unit)
            allowResponse.await()
            respond(
                content = ByteReadChannel(byteArrayOf(1, 2, 3)),
                status = HttpStatusCode.OK,
            )
        }
        val repository = KtorWmtsRepository(
            config = WmtsMapConfig("https://example.test/wmts"),
            engine = engine,
        )

        try {
            val first = async { repository.loadTile("https://example.test/tile/0/0/0") }
            requestStarted.await()
            val second = async { repository.loadTile("https://example.test/tile/0/0/0") }
            allowResponse.complete(Unit)

            assertContentEquals(byteArrayOf(1, 2, 3), first.await())
            assertContentEquals(byteArrayOf(1, 2, 3), second.await())
            assertEquals(1, requestCount)
        } finally {
            repository.close()
        }
    }

    @Test
    fun `maps non-success responses to a typed HTTP error`() = runTest {
        val repository = KtorWmtsRepository(
            config = WmtsMapConfig("https://example.test/wmts"),
            engine = MockEngine {
                respond(content = ByteReadChannel(ByteArray(0)), status = HttpStatusCode.ServiceUnavailable)
            },
        )

        try {
            val exception = assertFailsWith<WmtsMapException> {
                repository.loadTile("https://example.test/tile")
            }
            assertEquals(503, (exception.error as com.example.wmtsmap.model.WmtsMapError.HttpStatus).statusCode)
        } finally {
            repository.close()
        }
    }

    private companion object {
        val CAPABILITIES_XML = """
            <Capabilities version="1.0.0">
              <Contents>
                <Layer>
                  <Identifier>layer</Identifier>
                  <Style isDefault="true"><Identifier>default</Identifier></Style>
                  <Format>image/png</Format>
                  <TileMatrixSetLink><TileMatrixSet>grid</TileMatrixSet></TileMatrixSetLink>
                </Layer>
                <TileMatrixSet>
                  <Identifier>grid</Identifier>
                  <SupportedCRS>EPSG:3857</SupportedCRS>
                  <TileMatrix>
                    <Identifier>0</Identifier><ScaleDenominator>1</ScaleDenominator>
                    <TopLeftCorner>0 0</TopLeftCorner>
                    <TileWidth>256</TileWidth><TileHeight>256</TileHeight>
                    <MatrixWidth>1</MatrixWidth><MatrixHeight>1</MatrixHeight>
                  </TileMatrix>
                </TileMatrixSet>
              </Contents>
            </Capabilities>
        """.trimIndent()
    }
}
