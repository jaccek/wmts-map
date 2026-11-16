package com.example.wmtsmap.model

/** A geographic position in WGS 84 longitude/latitude coordinates. */
data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double,
)

/** A position in the east/north axes of a projected coordinate reference system. */
data class ProjectedCoordinate(
    val easting: Double,
    val northing: Double,
)

/** Parsed service and layer metadata from a WMTS 1.0.0 capabilities document. */
data class WmtsCapabilities(
    val version: String,
    val serviceIdentification: WmtsServiceIdentification?,
    val serviceProvider: WmtsServiceProvider?,
    val layers: List<WmtsLayer>,
    val tileMatrixSets: List<WmtsTileMatrixSet>,
    val getTileEndpoints: List<String>,
)

/** Human-readable service information. */
data class WmtsServiceIdentification(
    val title: String?,
    val abstract: String?,
    val serviceType: String?,
    val serviceTypeVersion: String?,
)

/** Optional provider contact information. */
data class WmtsServiceProvider(
    val name: String?,
    val site: String?,
)

/** A named WMTS layer and its supported styles, formats, and matrix sets. */
data class WmtsLayer(
    val identifier: String,
    val title: String?,
    val abstract: String?,
    val styles: List<WmtsStyle>,
    val formats: List<String>,
    val tileMatrixSetLinks: List<String>,
    val resourceUrls: List<WmtsResourceUrl>,
)

/** A WMTS style available for a layer. */
data class WmtsStyle(
    val identifier: String,
    val title: String?,
    val isDefault: Boolean,
)

/** A RESTful WMTS resource URL template. */
data class WmtsResourceUrl(
    val template: String,
    val resourceType: String,
    val format: String?,
)

/** A supported tile matrix set and its matrix definitions. */
data class WmtsTileMatrixSet(
    val identifier: String,
    val supportedCrs: String,
    val tileMatrices: List<WmtsTileMatrix>,
)

/** A tile matrix definition using the WMTS top-left corner and matrix dimensions. */
data class WmtsTileMatrix(
    val identifier: String,
    val scaleDenominator: Double,
    val topLeftCorner: ProjectedCoordinate,
    val tileWidth: Int,
    val tileHeight: Int,
    val matrixWidth: Long,
    val matrixHeight: Long,
)

/** Failures surfaced by capabilities parsing or map data loading. */
sealed interface WmtsMapError {
    val message: String

    data class InvalidCapabilities(override val message: String) : WmtsMapError
    data class UnsupportedCrs(val crs: String) : WmtsMapError {
        override val message: String = "Unsupported coordinate reference system: $crs"
    }

    data class Network(override val message: String) : WmtsMapError
    data class HttpStatus(val statusCode: Int) : WmtsMapError {
        override val message: String = "WMTS request failed with HTTP status $statusCode"
    }
}

/** An exception carrying a stable, user-displayable WMTS error. */
class WmtsMapException(
    val error: WmtsMapError,
    cause: Throwable? = null,
) : Exception(error.message, cause)
