package com.example.wmtsmap.parser

import com.example.wmtsmap.model.ProjectedCoordinate
import com.example.wmtsmap.model.WmtsCapabilities
import com.example.wmtsmap.model.WmtsLayer
import com.example.wmtsmap.model.WmtsMapError
import com.example.wmtsmap.model.WmtsMapException
import com.example.wmtsmap.model.WmtsResourceUrl
import com.example.wmtsmap.model.WmtsServiceIdentification
import com.example.wmtsmap.model.WmtsServiceProvider
import com.example.wmtsmap.model.WmtsStyle
import com.example.wmtsmap.model.WmtsTileMatrix
import com.example.wmtsmap.model.WmtsTileMatrixSet
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.xmlStreaming

/** Parses OGC WMTS 1.0.0 capabilities into platform-independent models. */
object WmtsCapabilitiesParser {
    private const val SUPPORTED_VERSION = "1.0.0"

    /** Parses a capabilities document or throws [WmtsMapException] for invalid input. */
    fun parse(xml: String): WmtsCapabilities {
        val root = try {
            parseXmlTree(xml)
        } catch (exception: Exception) {
            if (exception is WmtsMapException) throw exception
            throw WmtsMapException(
                WmtsMapError.InvalidCapabilities("The WMTS capabilities document is not valid XML."),
                exception,
            )
        }

        if (root.name != "Capabilities") {
            invalid("The document root must be Capabilities.")
        }

        val version = root.attributes["version"]
        if (version != SUPPORTED_VERSION) {
            invalid("Unsupported WMTS capabilities version: ${version ?: "missing"}.")
        }

        val contents = root.child("Contents")
            ?: invalid("The capabilities document is missing Contents.")
        val layers = contents.children("Layer").map(::parseLayer)
        val matrixSets = contents.children("TileMatrixSet").map(::parseMatrixSet)

        if (layers.isEmpty()) invalid("The capabilities document contains no layers.")
        if (matrixSets.isEmpty()) invalid("The capabilities document contains no tile matrix sets.")

        return WmtsCapabilities(
            version = version,
            serviceIdentification = root.child("ServiceIdentification")?.let {
                WmtsServiceIdentification(
                    title = it.childText("Title"),
                    abstract = it.childText("Abstract"),
                    serviceType = it.childText("ServiceType"),
                    serviceTypeVersion = it.childText("ServiceTypeVersion"),
                )
            },
            serviceProvider = root.child("ServiceProvider")?.let {
                WmtsServiceProvider(
                    name = it.childText("ProviderName"),
                    site = it.child("ProviderSite")?.attributes?.get("href"),
                )
            },
            layers = layers,
            tileMatrixSets = matrixSets,
            getTileEndpoints = extractGetTileEndpoints(root),
        )
    }

    private fun parseLayer(element: XmlElement): WmtsLayer {
        val identifier = element.childText("Identifier")
            ?: invalid("A WMTS layer is missing its identifier.")
        val styles = element.children("Style").map { style ->
            WmtsStyle(
                identifier = style.childText("Identifier")
                    ?: invalid("A WMTS style in layer '$identifier' is missing its identifier."),
                title = style.childText("Title"),
                isDefault = style.attributes["isDefault"].equals("true", ignoreCase = true),
            )
        }

        return WmtsLayer(
            identifier = identifier,
            title = element.childText("Title"),
            abstract = element.childText("Abstract"),
            styles = styles,
            formats = element.children("Format").mapNotNull { it.textValue() },
            tileMatrixSetLinks = element.children("TileMatrixSetLink")
                .mapNotNull { it.childText("TileMatrixSet") },
            resourceUrls = element.children("ResourceURL").mapNotNull { resource ->
                val template = resource.attributes["template"] ?: return@mapNotNull null
                WmtsResourceUrl(
                    template = template,
                    resourceType = resource.attributes["resourceType"].orEmpty(),
                    format = resource.attributes["format"],
                )
            },
        )
    }

    private fun parseMatrixSet(element: XmlElement): WmtsTileMatrixSet {
        val identifier = element.childText("Identifier")
            ?: invalid("A tile matrix set is missing its identifier.")
        val crs = element.childText("SupportedCRS")
            ?: invalid("Tile matrix set '$identifier' is missing SupportedCRS.")

        val matrices = element.children("TileMatrix").map { matrix ->
            val matrixIdentifier = matrix.childText("Identifier")
                ?: invalid("A tile matrix in '$identifier' is missing its identifier.")
            val topLeftValues = matrix.childText("TopLeftCorner")
                ?.trim()
                ?.split(WHITESPACE)
                ?.filter(String::isNotEmpty)
                ?.mapNotNull(String::toDoubleOrNull)
                ?: invalid("Tile matrix '$matrixIdentifier' has an invalid TopLeftCorner.")
            if (topLeftValues.size != 2 || topLeftValues.any { !it.isFinite() }) {
                invalid("Tile matrix '$matrixIdentifier' must have two TopLeftCorner coordinates.")
            }

            WmtsTileMatrix(
                identifier = matrixIdentifier,
                scaleDenominator = matrix.requiredDouble("ScaleDenominator"),
                topLeftCorner = ProjectedCoordinate(topLeftValues[0], topLeftValues[1]),
                tileWidth = matrix.requiredInt("TileWidth"),
                tileHeight = matrix.requiredInt("TileHeight"),
                matrixWidth = matrix.requiredLong("MatrixWidth"),
                matrixHeight = matrix.requiredLong("MatrixHeight"),
            )
        }

        if (matrices.isEmpty()) invalid("Tile matrix set '$identifier' contains no tile matrices.")

        return WmtsTileMatrixSet(identifier, crs, matrices)
    }

    private fun extractGetTileEndpoints(root: XmlElement): List<String> =
        root.descendants("Operation")
            .filter { it.attributes["name"] == "GetTile" }
            .flatMap { it.descendants("Get") }
            .mapNotNull { it.attributes["href"] }
            .distinct()

    private fun parseXmlTree(xml: String): XmlElement {
        if (xml.isBlank()) invalid("The capabilities document is empty.")
        val reader = xmlStreaming.newGenericReader(xml, expandEntities = false)
        val stack = mutableListOf<XmlElement>()
        var root: XmlElement? = null

        try {
            while (reader.hasNext()) {
                when (reader.next()) {
                    EventType.START_ELEMENT -> {
                        val attributes = buildMap {
                            for (index in 0 until reader.attributeCount) {
                                put(reader.getAttributeLocalName(index), reader.getAttributeValue(index))
                            }
                        }
                        val element = XmlElement(reader.localName, attributes)
                        val parent = stack.lastOrNull()
                        if (parent == null) {
                            if (root != null) invalid("The XML document has multiple root elements.")
                            root = element
                        } else {
                            parent.children.add(element)
                        }
                        stack.add(element)
                    }

                    EventType.TEXT,
                    EventType.CDSECT,
                    EventType.ENTITY_REF,
                    -> stack.lastOrNull()?.text?.append(reader.text)

                    EventType.DOCDECL -> invalid("Document type declarations are not supported.")
                    EventType.END_ELEMENT -> if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
                    else -> Unit
                }
            }
        } catch (exception: WmtsMapException) {
            throw exception
        } catch (exception: XmlException) {
            throw exception
        } finally {
            reader.close()
        }

        return root ?: invalid("The XML document has no root element.")
    }

    private fun XmlElement.requiredDouble(name: String): Double =
        childText(name)?.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
            ?: invalid("Tile matrix '$name' is missing or has an invalid value.")

    private fun XmlElement.requiredInt(name: String): Int =
        childText(name)?.toIntOrNull()?.takeIf { it > 0 }
            ?: invalid("Tile matrix '$name' is missing or has an invalid value.")

    private fun XmlElement.requiredLong(name: String): Long =
        childText(name)?.toLongOrNull()?.takeIf { it > 0L }
            ?: invalid("Tile matrix '$name' is missing or has an invalid value.")

    private fun XmlElement.childText(name: String): String? =
        child(name)?.textValue()

    private fun XmlElement.child(name: String): XmlElement? =
        children.firstOrNull { it.name == name }

    private fun XmlElement.children(name: String): List<XmlElement> =
        children.filter { it.name == name }

    private fun XmlElement.descendants(name: String): List<XmlElement> =
        children.flatMap { child ->
            (if (child.name == name) listOf(child) else emptyList()) + child.descendants(name)
        }

    private fun XmlElement.textValue(): String? =
        text.toString().trim().takeIf(String::isNotEmpty)

    private fun invalid(message: String): Nothing =
        throw WmtsMapException(WmtsMapError.InvalidCapabilities(message))

    private class XmlElement(
        val name: String,
        val attributes: Map<String, String>,
    ) {
        val children: MutableList<XmlElement> = mutableListOf()
        val text = StringBuilder()
    }

    private val WHITESPACE = Regex("\\s+")
}
