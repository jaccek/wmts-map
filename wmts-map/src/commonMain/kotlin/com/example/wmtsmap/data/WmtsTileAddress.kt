package com.example.wmtsmap.data

import com.example.wmtsmap.model.WmtsCapabilities
import com.example.wmtsmap.model.WmtsLayer
import com.example.wmtsmap.model.WmtsMapError
import com.example.wmtsmap.model.WmtsMapException
import com.example.wmtsmap.model.WmtsTileMatrixSet
import com.example.wmtsmap.tile.WmtsTileCoordinate
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom

/** Builds RESTful or KVP GetTile requests from parsed service metadata. */
object WmtsTileAddress {
    /** Resolves a tile URL using a REST template when available and KVP otherwise. */
    fun resolve(
        capabilitiesUrl: String,
        capabilities: WmtsCapabilities,
        layer: WmtsLayer,
        matrixSet: WmtsTileMatrixSet,
        matrixIdentifier: String,
        tile: WmtsTileCoordinate,
        style: String,
        format: String,
    ): String {
        val restTemplate = layer.resourceUrls.firstOrNull {
            it.resourceType.equals("tile", ignoreCase = true) &&
                (it.format == null || it.format == format)
        }?.template

        if (restTemplate != null) {
            val values = mapOf(
                "Layer" to layer.identifier,
                "Style" to style,
                "TileMatrixSet" to matrixSet.identifier,
                "TileMatrix" to matrixIdentifier,
                "TileRow" to tile.row.toString(),
                "TileCol" to tile.column.toString(),
                "Format" to format,
            )
            return PLACEHOLDER.replace(restTemplate) { match ->
                values[match.groupValues[1]] ?: match.value
            }
        }

        val endpoint = capabilities.getTileEndpoints.firstOrNull() ?: capabilitiesUrl
        val urlBuilder = URLBuilder().apply {
            takeFrom(endpoint)
            parameters["SERVICE"] = "WMTS"
            parameters["REQUEST"] = "GetTile"
            parameters["VERSION"] = capabilities.version
            parameters["LAYER"] = layer.identifier
            parameters["STYLE"] = style
            parameters["FORMAT"] = format
            parameters["TILEMATRIXSET"] = matrixSet.identifier
            parameters["TILEMATRIX"] = matrixIdentifier
            parameters["TILEROW"] = tile.row.toString()
            parameters["TILECOL"] = tile.column.toString()
        }
        return urlBuilder.buildString()
    }

    /** Resolves an explicitly selected layer or reports ambiguity. */
    fun selectLayer(capabilities: WmtsCapabilities, identifier: String?): WmtsLayer {
        val selected = identifier?.let { id -> capabilities.layers.firstOrNull { it.identifier == id } }
        if (selected != null) return selected
        if (identifier != null) invalid("WMTS layer '$identifier' was not found.")
        return capabilities.layers.singleOrNull()
            ?: invalid("Select one of the ${capabilities.layers.size} available WMTS layers.")
    }

    /** Resolves an explicitly selected matrix set or reports ambiguity. */
    fun selectMatrixSet(
        capabilities: WmtsCapabilities,
        layer: WmtsLayer,
        identifier: String?,
    ): WmtsTileMatrixSet {
        val compatible = capabilities.tileMatrixSets.filter {
            it.identifier in layer.tileMatrixSetLinks
        }
        val candidates = if (compatible.isEmpty()) capabilities.tileMatrixSets else compatible
        val selected = identifier?.let { id -> candidates.firstOrNull { it.identifier == id } }
        if (selected != null) return selected
        if (identifier != null) invalid("Tile matrix set '$identifier' is not available for layer '${layer.identifier}'.")
        return candidates.singleOrNull()
            ?: invalid("Select one of the ${candidates.size} available tile matrix sets.")
    }

    private fun invalid(message: String): Nothing =
        throw WmtsMapException(WmtsMapError.InvalidCapabilities(message))

    private val PLACEHOLDER = Regex("\\{([^{}]+)\\}")
}
