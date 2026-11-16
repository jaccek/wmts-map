# WMTS Map

`wmts-map` is a Compose Multiplatform library and `example` is a small app that
demonstrates loading and interacting with a WMTS raster layer.

## Modules and architecture

- `:wmts-map` contains public Compose/controller APIs and platform-independent
  WMTS models, parsing, URL resolution, projection math, tile calculations,
  networking, and caching.
- `:example` uses the library as a normal Gradle project dependency.
- Capabilities parsing and geographic/tile calculations are independent of
  rendering. Ktor is the replaceable networking boundary; `WmtsTileCache` is
  the replaceable byte-cache boundary.
- A controller owns its coroutine scope and HTTP client. Removing the map from
  composition cancels its work and closes the client.

## Supported platforms and functionality

The configured library and example targets are Android and iOS. Android uses
Ktor OkHttp and Android bitmap decoding; iOS uses Ktor Darwin and Skia image
decoding. Desktop/JVM is not configured.

Implemented WMTS functionality:

- WMTS 1.0.0 capabilities parsing, including layers, titles, abstracts, styles,
  formats, linked tile matrix sets, matrices, service/provider metadata,
  RESTful resource templates, and KVP GetTile endpoints.
- RESTful and KVP tile URL generation.
- Raster tile fetching with configurable headers/timeouts, bounded concurrent
  requests, same-URL request coalescing, and a bounded in-memory LRU cache.
- Pan, pinch-to-zoom, configurable initial WGS 84 center, matrix/layer selection,
  and drawing based on each matrix's origin, scale, dimensions, and tile size.
- EPSG:4326, OGC:CRS84, EPSG:3857, and EPSG:2180 transformations.

Only the named CRSs are supported. EPSG:2180 uses the GRS80 ellipsoid and the
standard Transverse Mercator parameters for Poland CS92. The WGS 84 / ETRS89
datum difference is treated as negligible for this map use. WMTS scale
denominators are converted to units per pixel using the OGC 0.28 mm pixel size;
angular CRSs use the standard equatorial metres-per-degree conversion.
EPSG:4326 `TopLeftCorner` values are normalized from latitude/longitude axis
order before tile math; OGC:CRS84 uses longitude/latitude order.

Tile columns and rows are calculated from the selected matrix's advertised
top-left corner, scale denominator, tile width/height, and matrix dimensions.
Zoom is an index over matrices ordered by decreasing scale denominator;
fractional zoom interpolates between the actual adjacent resolutions rather
than assuming matrices double in size.

## Installation

For local development, add the module dependency:

```kotlin
commonMain.dependencies {
    implementation(project(":wmts-map"))
}
```

The project uses the centralized version catalog at
[`gradle/libs.versions.toml`](./gradle/libs.versions.toml).

## Basic usage

```kotlin
WmtsMap(
    config = WmtsMapConfig(
        capabilitiesUrl = "https://example.com/wmts?SERVICE=WMTS&REQUEST=GetCapabilities",
        layer = "my-layer",
        tileMatrixSet = "my-matrix-set",
        initialPosition = GeoCoordinate(latitude = 51.1, longitude = 17.0),
        initialZoom = 5.0,
    ),
    modifier = Modifier.fillMaxSize(),
    onMapReady = { controller -> /* retain for controls */ },
    onMapError = { error -> /* show error.message */ },
)
```

If a service has multiple layers or matrix sets, configure them explicitly or
select them after reading the capabilities from the controller state. A single
available layer/matrix set is selected automatically. A layer's default style
is used when available.

`WmtsMapConfig` also accepts request headers, connect/request/socket timeout
values, a maximum concurrent tile-request count, initial/minimum/maximum zoom,
and in-memory cache capacity.

## Controller and observable state

```kotlin
val controller = /* supplied by onMapReady */
controller.zoomIn()
controller.zoomOut()
controller.moveTo(GeoCoordinate(latitude = 52.0, longitude = 21.0), zoom = 7.0)
controller.selectLayer("another-layer")
controller.selectTileMatrixSet("another-matrix-set")
controller.reload()

val currentPosition = controller.getCurrentPosition()
val currentZoom = controller.getCurrentZoom()
```

`controller.state` is a `StateFlow<WmtsMapState>` containing loading status,
capabilities, selected layer/matrix set, WGS 84 position, zoom, rendered tiles,
and a typed error. It can be collected in Compose with `collectAsState()`.

## Networking, caching, and errors

Ktor handles HTTP GET requests. Non-2xx responses become `HttpStatus` errors;
timeouts and transport failures become `Network` errors; malformed or
unsupported capabilities become `InvalidCapabilities` errors. Coroutine
cancellation is propagated.

The map uses a bounded in-memory LRU cache by default. `KtorWmtsRepository`
accepts a custom `WmtsTileCache` implementation and exposes cache invalidation;
no disk/persistent cache implementation is included. Ktor MockEngine and local
capabilities fixtures are used for tests; unit tests do not depend on a live
WMTS service.

## Example application

Run `./gradlew :example:assembleDebug` to build the Android example. Its initial
URL is a Polish Geoportal orthophoto WMTS capabilities endpoint; network/service
availability is not assumed. Enter another capabilities URL and tap **Load**
to try another service. The UI offers layer and compatible matrix-set cycling,
map zoom controls, WGS 84 centering, coordinates, zoom, formats, and CRS
metadata.

The iOS framework build command is
`./gradlew :example:linkDebugFrameworkIosSimulatorArm64`.

## Tests and checks

Run `./gradlew :wmts-map:iosSimulatorArm64Test` for the common parser,
projection, tile-math, URL-resolution, cache, and mocked-network tests on the
iOS simulator target. `./gradlew :wmts-map:allTests :example:allTests` is also
defined, but the current Compose Preview/AGP configuration fails while
configuring a release preview task because release unit tests are disabled.

No formatting or static-analysis tool is configured.

## Current limitations

- No Desktop/JVM target or mouse-wheel gesture support is configured.
- No persistent tile cache or HTTP cache-header policy is implemented.
- `TileMatrixSetLimits`, non-raster formats, WMTS versions other than 1.0.0,
  and projections other than those listed above are not supported.
- EPSG:2180 uses a standard series implementation of Transverse Mercator;
  high-accuracy geodetic datum grid shifts are not applied.
- KVP operation constraints and authentication beyond caller-supplied headers
  are not interpreted.
