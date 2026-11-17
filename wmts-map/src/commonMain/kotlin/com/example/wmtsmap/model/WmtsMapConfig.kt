package com.example.wmtsmap.model

/**
 * Immutable options for loading and displaying a WMTS layer.
 *
 * When layer, tile matrix set, or initial position is omitted, the map derives each default
 * from the first compatible capabilities entry.
 */
data class WmtsMapConfig(
    val capabilitiesUrl: String,
    val layer: String? = null,
    val style: String? = null,
    val tileMatrixSet: String? = null,
    val initialPosition: GeoCoordinate? = null,
    val initialZoom: Double = 0.0,
    val minZoom: Double? = null,
    val maxZoom: Double? = null,
    val requestHeaders: Map<String, String> = emptyMap(),
    val connectTimeoutMillis: Long = 10_000,
    val requestTimeoutMillis: Long = 30_000,
    val socketTimeoutMillis: Long = 30_000,
    val maxConcurrentTileRequests: Int = 6,
    val memoryCacheEntries: Int = 256,
) {
    init {
        require(capabilitiesUrl.isNotBlank())
        initialPosition?.let {
            require(it.latitude in -90.0..90.0)
            require(it.longitude in -180.0..180.0)
        }
        require(initialZoom.isFinite())
        require(connectTimeoutMillis > 0L)
        require(requestTimeoutMillis > 0L)
        require(socketTimeoutMillis > 0L)
        require(maxConcurrentTileRequests > 0)
        require(memoryCacheEntries > 0)
        require(minZoom == null || minZoom.isFinite())
        require(maxZoom == null || maxZoom.isFinite())
        require(minZoom == null || maxZoom == null || minZoom <= maxZoom)
    }
}
