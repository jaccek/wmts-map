package com.example.wmtsmap.model

/** Immutable options for loading and displaying a WMTS layer. */
data class WmtsMapConfig(
    val capabilitiesUrl: String,
    val layer: String? = null,
    val style: String? = null,
    val tileMatrixSet: String? = null,
    val initialPosition: GeoCoordinate = GeoCoordinate(latitude = 0.0, longitude = 0.0),
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
        require(initialPosition.latitude in -90.0..90.0)
        require(initialPosition.longitude in -180.0..180.0)
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
