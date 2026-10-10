package com.example.personalsport.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GpsPoint(
    val lat: Double,
    val lng: Double
)

object GpsRouteUtils {

    /**
     * Parses a JSON string containing array of coordinate objects into a List of GpsPoint.
     * Supports {"lat": double, "lng": double} and {"latitude": double, "longitude": double}.
     */
    fun parseGpsPoints(jsonStr: String?): List<GpsPoint> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        val list = mutableListOf<GpsPoint>()
        val pattern = Regex("""[{ \s]*"(?:lat|latitude)"\s*:\s*(-?\d+(?:\.\d+)?)\s*,\s*"(?:lng|longitude)"\s*:\s*(-?\d+(?:\.\d+)?)\s*[}\s]*""")
        for (match in pattern.findAll(jsonStr)) {
            val (latStr, lngStr) = match.destructured
            val lat = latStr.toDoubleOrNull()
            val lng = lngStr.toDoubleOrNull()
            if (lat != null && lng != null) {
                list.add(GpsPoint(lat, lng))
            }
        }
        return list
    }

    /**
     * Alias method matching GpsRouteUtils.parseGpsRoutePoints
     */
    fun parseGpsRoutePoints(routeGpsPoints: String?): List<GpsPoint> = parseGpsPoints(routeGpsPoints)

    /**
     * Encodes a list of GpsPoints into a JSON array string.
     */
    fun encodeGpsPoints(points: List<GpsPoint>): String {
        return points.joinToString(separator = ",", prefix = "[", postfix = "]") { point ->
            "{\"lat\":${point.lat},\"lng\":${point.lng}}"
        }
    }

    /**
     * Calculates total route distance in kilometers using the Haversine formula.
     */
    fun calculateTotalDistanceKm(points: List<GpsPoint>): Double {
        if (points.size < 2) return 0.0
        var totalMeters = 0.0
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            totalMeters += distanceMeters(p1.lat, p1.lng, p2.lat, p2.lng)
        }
        return totalMeters / 1000.0
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    /**
     * Calculates average pace formatted as min'sec" /km (e.g. 4'45" /km).
     */
    fun calculatePaceMinKm(distanceKm: Double, durationMinutes: Double): String {
        if (distanceKm <= 0.0 || durationMinutes <= 0.0) return "4'30\" /km"
        val paceDecimal = durationMinutes / distanceKm
        val mins = paceDecimal.toInt().coerceIn(1, 30)
        val secs = (((paceDecimal - mins) * 60).toInt()).coerceIn(0, 59)
        return String.format(Locale.US, "%d'%02d\" /km", mins, secs)
    }

    /**
     * Estimates total elevation gain in meters.
     */
    fun calculateElevationGainMeters(points: List<GpsPoint>): Int {
        if (points.size < 2) return 35
        val minLat = points.minOf { it.lat }
        val maxLat = points.maxOf { it.lat }
        val minLng = points.minOf { it.lng }
        val maxLng = points.maxOf { it.lng }
        val latSpan = Math.abs(maxLat - minLat)
        val lngSpan = Math.abs(maxLng - minLng)
        val totalSpan = latSpan + lngSpan
        val estimatedMeters = (totalSpan * 12000).toInt()
        return estimatedMeters.coerceIn(15, 180)
    }

    /**
     * Formats start and finish time range string e.g. "08:30 - 08:55".
     */
    fun formatStartAndFinishTime(startTimestampMs: Long, durationMinutes: Int): String {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startStr = timeFormat.format(Date(startTimestampMs))
        val finishMs = startTimestampMs + (durationMinutes.coerceAtLeast(1) * 60 * 1000L)
        val finishStr = timeFormat.format(Date(finishMs))
        return "$startStr - $finishStr"
    }

    /**
     * Generates sample GPS polyline points for outdoor running.
     */
    fun generateSampleRunRoute(): List<GpsPoint> = listOf(
        GpsPoint(40.4168, -3.7038),
        GpsPoint(40.4175, -3.7020),
        GpsPoint(40.4190, -3.7005),
        GpsPoint(40.4210, -3.6990),
        GpsPoint(40.4225, -3.7010),
        GpsPoint(40.4215, -3.7035),
        GpsPoint(40.4195, -3.7050),
        GpsPoint(40.4178, -3.7045),
        GpsPoint(40.4168, -3.7038)
    )

    /**
     * Generates sample GPS polyline points for open water swimming.
     */
    fun generateSampleOpenWaterSwimRoute(): List<GpsPoint> = listOf(
        GpsPoint(39.4699, -0.3763),
        GpsPoint(39.4710, -0.3740),
        GpsPoint(39.4730, -0.3725),
        GpsPoint(39.4745, -0.3745),
        GpsPoint(39.4735, -0.3770),
        GpsPoint(39.4715, -0.3780),
        GpsPoint(39.4699, -0.3763)
    )

    fun generateSampleRunRouteJson(): String = encodeGpsPoints(generateSampleRunRoute())

    fun generateSampleOpenWaterSwimRouteJson(): String = encodeGpsPoints(generateSampleOpenWaterSwimRoute())
}
