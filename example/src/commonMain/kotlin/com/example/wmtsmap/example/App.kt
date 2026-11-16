package com.example.wmtsmap.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wmtsmap.controller.WmtsMapController
import com.example.wmtsmap.model.GeoCoordinate
import com.example.wmtsmap.model.WmtsLayer
import com.example.wmtsmap.model.WmtsMapConfig
import com.example.wmtsmap.model.WmtsTileMatrixSet
import com.example.wmtsmap.ui.WmtsMap

private const val SAMPLE_WMTS_CAPABILITIES =
//    "https://maps.wien.gv.at/basemap/1.0.0/WMTSCapabilities.xml"
//    "https://integracja.gugik.gov.pl/cgi-bin/KrajowaIntegracjaEwidencjiGruntow?Service=WMTS&Request=GetCapabilities"
//    "https://mapy.geoportal.gov.pl/wss/service/WMTS/guest/wmts/BDOT10k?Service=WMTS&Request=GetCapabilities"
//    "https://mapy.geoportal.gov.pl/wss/service/WMTS/guest/wmts/BDOT10k-BDOO?Service=WMTS&Request=GetCapabilities"
    "https://mapy.geoportal.gov.pl/wss/service/PZGIK/ORTO/WMTS/StandardResolution?Service=WMTS&Request=GetCapabilities"

private const val SAMPLE_LAYER = "ORTOFOTOMAPA"//"geolandbasemap"
private const val SAMPLE_MATRIX_SET = "EPSG:2180"//"google3857"
private val SAMPLE_POSITION = GeoCoordinate(latitude = 48.2082, longitude = 16.3738)

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            WmtsMapExample()
        }
    }
}

@Composable
private fun WmtsMapExample() {
    var serviceUrl by remember { mutableStateOf(SAMPLE_WMTS_CAPABILITIES) }
    var activeUrl by remember { mutableStateOf(SAMPLE_WMTS_CAPABILITIES) }
    var mapController by remember { mutableStateOf<WmtsMapController?>(null) }
    var latitude by remember { mutableStateOf(SAMPLE_POSITION.latitude.toString()) }
    var longitude by remember { mutableStateOf(SAMPLE_POSITION.longitude.toString()) }
    var position by remember { mutableStateOf(SAMPLE_POSITION) }
    var zoom by remember { mutableStateOf(5.0) }
    var layerIdentifier by remember { mutableStateOf<String?>(null) }
    var matrixSetIdentifier by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val mapState = mapController?.state?.collectAsState()
    val capabilities = mapState?.value?.capabilities
    val layers = capabilities?.layers.orEmpty()
    val selectedLayer = layers.firstOrNull { it.identifier == layerIdentifier }
    val matrixSets = capabilities?.tileMatrixSets.orEmpty()
    val linkedMatrixSets = matrixSets.filter { it.identifier in selectedLayer?.tileMatrixSetLinks.orEmpty() }
    val availableMatrixSets = if (selectedLayer != null && linkedMatrixSets.isNotEmpty()) {
        linkedMatrixSets
    } else {
        matrixSets
    }

    LaunchedEffect(mapController) {
        mapController?.state?.collect { state ->
            position = state.position
            zoom = state.zoom
            layerIdentifier = state.selectedLayer ?: layerIdentifier
            matrixSetIdentifier = state.selectedTileMatrixSet ?: matrixSetIdentifier
            errorMessage = state.error?.message
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("WMTS Map example", style = MaterialTheme.typography.titleLarge)
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = serviceUrl,
                onValueChange = { serviceUrl = it },
                modifier = Modifier.weight(1f),
                label = { Text("Capabilities URL") },
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    activeUrl = serviceUrl
                    mapController = null
                    layerIdentifier = null
                    matrixSetIdentifier = null
                    errorMessage = null
                },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Load")
            }
        }

        WmtsMap(
            config = WmtsMapConfig(
                capabilitiesUrl = activeUrl,
                layer = SAMPLE_LAYER.takeIf { activeUrl == SAMPLE_WMTS_CAPABILITIES },
                tileMatrixSet = SAMPLE_MATRIX_SET.takeIf { activeUrl == SAMPLE_WMTS_CAPABILITIES },
                initialPosition = position,
                initialZoom = zoom,
            ),
            modifier = Modifier.fillMaxWidth().height(340.dp),
            onMapReady = { mapController = it },
            onMapError = { errorMessage = it.message },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { mapController?.zoomOut() }) { Text("Zoom −") }
            OutlinedButton(onClick = { mapController?.zoomIn() }) { Text("Zoom +") }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    val next = layers.nextLayerIdentifier(layerIdentifier)
                    if (next != null) {
                        layerIdentifier = next
                        mapController?.selectLayer(next)
                        matrixSetIdentifier = null
                    }
                },
                enabled = layers.size > 1,
                modifier = Modifier.weight(1f),
            ) {
                Text("Layer: ${layerIdentifier ?: "select"}")
            }
            OutlinedButton(
                onClick = {
                    val next = availableMatrixSets.nextMatrixSetIdentifier(matrixSetIdentifier)
                    if (next != null) {
                        matrixSetIdentifier = next
                        mapController?.selectTileMatrixSet(next)
                    }
                },
                enabled = availableMatrixSets.size > 1,
                modifier = Modifier.weight(1f),
            ) {
                Text("Matrix set: ${matrixSetIdentifier ?: "select"}")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = latitude,
                onValueChange = { latitude = it },
                modifier = Modifier.weight(1f),
                label = { Text("Latitude") },
                singleLine = true,
            )
            OutlinedTextField(
                value = longitude,
                onValueChange = { longitude = it },
                modifier = Modifier.weight(1f),
                label = { Text("Longitude") },
                singleLine = true,
            )
            Button(
                onClick = {
                    val selectedLatitude = latitude.toDoubleOrNull()
                    val selectedLongitude = longitude.toDoubleOrNull()
                    if (selectedLatitude != null && selectedLongitude != null &&
                        selectedLatitude in -90.0..90.0 && selectedLongitude in -180.0..180.0
                    ) {
                        mapController?.moveTo(GeoCoordinate(selectedLatitude, selectedLongitude))
                        errorMessage = null
                    } else {
                        errorMessage = "Enter a valid WGS 84 latitude and longitude."
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Center")
            }
        }

        Text(
            "WGS84 ${position.latitude}, ${position.longitude}  |  Zoom $zoom",
            style = MaterialTheme.typography.bodyMedium,
        )

        capabilities?.let {
            Text(
                "Layers: ${layers.joinToString { layer -> layer.title ?: layer.identifier }}\n" +
                    "Tile matrix sets: ${availableMatrixSets.joinToString { "${it.identifier} (${it.supportedCrs})" }}\n" +
                    "Selected formats: ${selectedLayer?.formats?.joinToString().orEmpty()}",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

private fun List<WmtsLayer>.nextLayerIdentifier(current: String?): String? =
    when {
        isEmpty() -> null
        else -> get((indexOfFirst { it.identifier == current } + 1).mod(size)).identifier
    }

private fun List<WmtsTileMatrixSet>.nextMatrixSetIdentifier(current: String?): String? =
    when {
        isEmpty() -> null
        else -> get((indexOfFirst { it.identifier == current } + 1).mod(size)).identifier
    }
