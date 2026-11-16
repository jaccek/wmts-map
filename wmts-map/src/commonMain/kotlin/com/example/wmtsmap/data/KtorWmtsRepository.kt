package com.example.wmtsmap.data

import com.example.wmtsmap.model.WmtsCapabilities
import com.example.wmtsmap.model.WmtsMapConfig
import com.example.wmtsmap.model.WmtsMapError
import com.example.wmtsmap.model.WmtsMapException
import com.example.wmtsmap.parser.WmtsCapabilitiesParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/** Ktor-backed WMTS capabilities and tile repository with bounded downloads and caching. */
class KtorWmtsRepository(
    private val config: WmtsMapConfig,
    engine: HttpClientEngine? = null,
    private val cache: WmtsTileCache = InMemoryWmtsTileCache(config.memoryCacheEntries),
) : AutoCloseable {
    private val client = if (engine == null) {
        HttpClient {
            configureClient()
        }
    } else {
        HttpClient(engine) {
            configureClient()
        }
    }
    private val ownerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tileLimit = Semaphore(config.maxConcurrentTileRequests)
    private val inFlightMutex = Mutex()
    private val inFlight = mutableMapOf<String, kotlinx.coroutines.Deferred<ByteArray>>()

    /** Downloads and parses the configured GetCapabilities document. */
    suspend fun loadCapabilities(): WmtsCapabilities {
        try {
            val response = client.get(config.capabilitiesUrl) {
                headers {
                    config.requestHeaders.forEach { (name, value) -> append(name, value) }
                }
            }
            if (!response.status.isSuccess()) {
                throw WmtsMapException(WmtsMapError.HttpStatus(response.status.value))
            }
            return WmtsCapabilitiesParser.parse(response.bodyAsText())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: WmtsMapException) {
            throw exception
        } catch (exception: HttpRequestTimeoutException) {
            throw WmtsMapException(WmtsMapError.Network("The WMTS request timed out."), exception)
        } catch (exception: Exception) {
            val message = if (exception.hasTlsCertificateFailure()) {
                "The WMTS server's TLS certificate could not be verified."
            } else {
                "Unable to load WMTS capabilities."
            }
            throw WmtsMapException(WmtsMapError.Network(message), exception)
        }
    }

    /** Returns cached tile bytes or downloads the tile, coalescing concurrent requests by URL. */
    suspend fun loadTile(url: String): ByteArray {
        cache.get(url)?.let { return it }

        val deferred = inFlightMutex.withLock {
            cache.get(url)?.let { return it }
            inFlight[url] ?: ownerScope.async {
                tileLimit.withPermit {
                    downloadTile(url)
                }
            }.also { request ->
                inFlight[url] = request
                request.invokeOnCompletion {
                    ownerScope.launch {
                        inFlightMutex.withLock {
                            if (inFlight[url] === request) inFlight.remove(url)
                        }
                    }
                }
            }
        }

        return deferred.await()
    }

    /** Clears all cached tiles. */
    suspend fun invalidateCache() {
        val pendingRequests = inFlightMutex.withLock {
            inFlight.values.toList().also { inFlight.clear() }
        }
        pendingRequests.forEach { it.cancel() }
        cache.invalidate()
    }

    override fun close() {
        ownerScope.cancel()
        client.close()
    }

    private suspend fun downloadTile(url: String): ByteArray {
        try {
            val response = client.get(url) {
                headers {
                    config.requestHeaders.forEach { (name, value) -> append(name, value) }
                }
            }
            if (!response.status.isSuccess()) {
                throw WmtsMapException(WmtsMapError.HttpStatus(response.status.value))
            }
            return response.bodyAsBytes().also { cache.put(url, it) }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: WmtsMapException) {
            throw exception
        } catch (exception: HttpRequestTimeoutException) {
            throw WmtsMapException(WmtsMapError.Network("The WMTS tile request timed out."), exception)
        } catch (exception: Exception) {
            val message = if (exception.hasTlsCertificateFailure()) {
                "The WMTS server's TLS certificate could not be verified."
            } else {
                "Unable to load a WMTS tile."
            }
            throw WmtsMapException(WmtsMapError.Network(message), exception)
        }
    }

    private fun Throwable.hasTlsCertificateFailure(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            val typeName = current::class.simpleName.orEmpty()
            val message = current.message.orEmpty()
            if (
                typeName.contains("SSLHandshakeException") ||
                typeName.contains("CertificateException") ||
                typeName.contains("CertPath") ||
                message.contains("PKIX path building failed", ignoreCase = true) ||
                message.contains("trust anchor for certification path", ignoreCase = true) ||
                message.contains("unable to find valid certification path", ignoreCase = true)
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun io.ktor.client.HttpClientConfig<*>.configureClient() {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = config.connectTimeoutMillis
            requestTimeoutMillis = config.requestTimeoutMillis
            socketTimeoutMillis = config.socketTimeoutMillis
        }
    }
}
