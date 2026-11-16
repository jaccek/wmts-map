package com.example.wmtsmap.projection

import com.example.wmtsmap.model.GeoCoordinate
import com.example.wmtsmap.model.ProjectedCoordinate
import com.example.wmtsmap.model.WmtsMapError
import com.example.wmtsmap.model.WmtsMapException
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** Converts WGS 84 positions to and from supported WMTS coordinate reference systems. */
object WmtsProjection {
    private const val WEB_MERCATOR_RADIUS = 6_378_137.0
    private const val GRS80_SEMI_MAJOR_AXIS = 6_378_137.0
    private const val GRS80_INVERSE_FLATTENING = 298.257222101
    private const val POLAND_CENTRAL_MERIDIAN_DEGREES = 19.0
    private const val POLAND_SCALE_FACTOR = 0.9993
    private const val POLAND_FALSE_EASTING = 500_000.0
    private const val POLAND_FALSE_NORTHING = -5_300_000.0
    private const val EPSG4326_METERS_PER_DEGREE = 111_319.49079327358
    private const val MAX_MERCATOR_LATITUDE = 85.0511287798066

    /** Projects a WGS 84 coordinate into the east/north axes of [crs]. */
    fun forward(coordinate: GeoCoordinate, crs: String): ProjectedCoordinate =
        when (normalizeCrs(crs)) {
            "EPSG:4326", "CRS:84" -> ProjectedCoordinate(coordinate.longitude, coordinate.latitude)
            "EPSG:3857" -> forwardWebMercator(coordinate)
            "EPSG:2180" -> forwardPolandCs92(coordinate)
            else -> throw WmtsMapException(WmtsMapError.UnsupportedCrs(crs))
        }

    /** Converts an east/north coordinate in [crs] to WGS 84 longitude/latitude. */
    fun inverse(coordinate: ProjectedCoordinate, crs: String): GeoCoordinate =
        when (normalizeCrs(crs)) {
            "EPSG:4326", "CRS:84" -> GeoCoordinate(coordinate.northing, coordinate.easting)
            "EPSG:3857" -> inverseWebMercator(coordinate)
            "EPSG:2180" -> inversePolandCs92(coordinate)
            else -> throw WmtsMapException(WmtsMapError.UnsupportedCrs(crs))
        }

    /** Converts a WMTS top-left corner to conventional east/north axis order. */
    fun normalizeTopLeftCorner(coordinate: ProjectedCoordinate, crs: String): ProjectedCoordinate =
        if (normalizeCrs(crs) == "EPSG:4326") {
            ProjectedCoordinate(coordinate.northing, coordinate.easting)
        } else {
            coordinate
        }

    /** Returns the number of metres represented by one unit on the CRS's horizontal axis. */
    fun metersPerUnit(crs: String): Double =
        when (normalizeCrs(crs)) {
            "EPSG:4326", "CRS:84" -> EPSG4326_METERS_PER_DEGREE
            "EPSG:3857", "EPSG:2180" -> 1.0
            else -> throw WmtsMapException(WmtsMapError.UnsupportedCrs(crs))
        }

    /** Normalizes common EPSG URN, URL, and short-name forms. */
    fun normalizeCrs(crs: String): String {
        val code = crs.trim().substringAfterLast('/').substringAfterLast(':')
        return when (code) {
            "4326" -> "EPSG:4326"
            "3857" -> "EPSG:3857"
            "2180" -> "EPSG:2180"
            "CRS84" -> "CRS:84"
            else -> crs.trim().uppercase()
        }
    }

    private fun forwardWebMercator(coordinate: GeoCoordinate): ProjectedCoordinate {
        require(coordinate.latitude in -90.0..90.0 && coordinate.longitude in -180.0..180.0)
        val latitude = coordinate.latitude.coerceIn(-MAX_MERCATOR_LATITUDE, MAX_MERCATOR_LATITUDE)
        val latitudeRadians = radians(latitude)
        return ProjectedCoordinate(
            easting = WEB_MERCATOR_RADIUS * radians(coordinate.longitude),
            northing = WEB_MERCATOR_RADIUS * lnTanQuarterPlus(latitudeRadians),
        )
    }

    private fun inverseWebMercator(coordinate: ProjectedCoordinate): GeoCoordinate =
        GeoCoordinate(
            latitude = degrees(2.0 * kotlin.math.atan(kotlin.math.exp(coordinate.northing / WEB_MERCATOR_RADIUS)) - PI / 2.0),
            longitude = degrees(coordinate.easting / WEB_MERCATOR_RADIUS),
        )

    private fun forwardPolandCs92(coordinate: GeoCoordinate): ProjectedCoordinate {
        require(coordinate.latitude in -90.0..90.0 && coordinate.longitude in -180.0..180.0)
        val latitude = radians(coordinate.latitude)
        val longitude = radians(coordinate.longitude)
        val centralMeridian = radians(POLAND_CENTRAL_MERIDIAN_DEGREES)
        val eccentricitySquared = grs80EccentricitySquared()
        val secondEccentricitySquared = eccentricitySquared / (1.0 - eccentricitySquared)
        val sinLatitude = sin(latitude)
        val cosLatitude = cos(latitude)
        val tangent = tan(latitude)
        val n = GRS80_SEMI_MAJOR_AXIS / sqrt(1.0 - eccentricitySquared * sinLatitude.pow(2))
        val t = tangent.pow(2)
        val c = secondEccentricitySquared * cosLatitude.pow(2)
        val a = cosLatitude * (longitude - centralMeridian)
        val meridianArc = meridianArc(latitude, eccentricitySquared)

        return ProjectedCoordinate(
            easting = POLAND_FALSE_EASTING + POLAND_SCALE_FACTOR * n * (
                a + (1.0 - t + c) * a.pow(3) / 6.0 +
                    (5.0 - 18.0 * t + t.pow(2) + 72.0 * c - 58.0 * secondEccentricitySquared) * a.pow(5) / 120.0
                ),
            northing = POLAND_FALSE_NORTHING + POLAND_SCALE_FACTOR * (
                meridianArc + n * tangent * (
                    a.pow(2) / 2.0 +
                        (5.0 - t + 9.0 * c + 4.0 * c.pow(2)) * a.pow(4) / 24.0 +
                        (61.0 - 58.0 * t + t.pow(2) + 600.0 * c - 330.0 * secondEccentricitySquared) * a.pow(6) / 720.0
                    )
                ),
        )
    }

    private fun inversePolandCs92(coordinate: ProjectedCoordinate): GeoCoordinate {
        val eccentricitySquared = grs80EccentricitySquared()
        val secondEccentricitySquared = eccentricitySquared / (1.0 - eccentricitySquared)
        val easting = coordinate.easting - POLAND_FALSE_EASTING
        val meridianArc = (coordinate.northing - POLAND_FALSE_NORTHING) / POLAND_SCALE_FACTOR
        val mu = meridianArc / (
            GRS80_SEMI_MAJOR_AXIS *
                (1.0 - eccentricitySquared / 4.0 - 3.0 * eccentricitySquared.pow(2) / 64.0 -
                    5.0 * eccentricitySquared.pow(3) / 256.0)
            )
        val e1 = (1.0 - sqrt(1.0 - eccentricitySquared)) / (1.0 + sqrt(1.0 - eccentricitySquared))
        val phi1 = mu +
            (3.0 * e1 / 2.0 - 27.0 * e1.pow(3) / 32.0) * sin(2.0 * mu) +
            (21.0 * e1.pow(2) / 16.0 - 55.0 * e1.pow(4) / 32.0) * sin(4.0 * mu) +
            151.0 * e1.pow(3) / 96.0 * sin(6.0 * mu) +
            1097.0 * e1.pow(4) / 512.0 * sin(8.0 * mu)
        val sinPhi1 = sin(phi1)
        val cosPhi1 = cos(phi1)
        val tangent = tan(phi1)
        val n1 = GRS80_SEMI_MAJOR_AXIS / sqrt(1.0 - eccentricitySquared * sinPhi1.pow(2))
        val r1 = GRS80_SEMI_MAJOR_AXIS * (1.0 - eccentricitySquared) /
            (1.0 - eccentricitySquared * sinPhi1.pow(2)).pow(1.5)
        val t1 = tangent.pow(2)
        val c1 = secondEccentricitySquared * cosPhi1.pow(2)
        val d = easting / (n1 * POLAND_SCALE_FACTOR)
        val latitude = phi1 - n1 * tangent / r1 * (
            d.pow(2) / 2.0 -
                (5.0 + 3.0 * t1 + 10.0 * c1 - 4.0 * c1.pow(2) - 9.0 * secondEccentricitySquared) * d.pow(4) / 24.0 +
                (61.0 + 90.0 * t1 + 298.0 * c1 + 45.0 * t1.pow(2) -
                    252.0 * secondEccentricitySquared - 3.0 * c1.pow(2)) * d.pow(6) / 720.0
            )
        val longitude = radians(POLAND_CENTRAL_MERIDIAN_DEGREES) + (
            d - (1.0 + 2.0 * t1 + c1) * d.pow(3) / 6.0 +
                (5.0 - 2.0 * c1 + 28.0 * t1 - 3.0 * c1.pow(2) +
                    8.0 * secondEccentricitySquared + 24.0 * t1.pow(2)) * d.pow(5) / 120.0
            ) / cosPhi1

        return GeoCoordinate(degrees(latitude), degrees(longitude))
    }

    private fun meridianArc(latitude: Double, eccentricitySquared: Double): Double {
        val e4 = eccentricitySquared.pow(2)
        val e6 = eccentricitySquared.pow(3)
        return GRS80_SEMI_MAJOR_AXIS * (
            (1.0 - eccentricitySquared / 4.0 - 3.0 * e4 / 64.0 - 5.0 * e6 / 256.0) * latitude -
                (3.0 * eccentricitySquared / 8.0 + 3.0 * e4 / 32.0 + 45.0 * e6 / 1024.0) * sin(2.0 * latitude) +
                (15.0 * e4 / 256.0 + 45.0 * e6 / 1024.0) * sin(4.0 * latitude) -
                35.0 * e6 / 3072.0 * sin(6.0 * latitude)
            )
    }

    private fun grs80EccentricitySquared(): Double {
        val flattening = 1.0 / GRS80_INVERSE_FLATTENING
        return flattening * (2.0 - flattening)
    }

    private fun lnTanQuarterPlus(latitude: Double): Double =
        kotlin.math.ln(tan(PI / 4.0 + latitude / 2.0))

    private fun radians(degrees: Double): Double = degrees * PI / 180.0

    private fun degrees(radians: Double): Double = radians * 180.0 / PI
}
