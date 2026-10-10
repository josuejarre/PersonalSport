package com.example.personalsport.ui.components

import com.example.personalsport.data.local.entity.WorkoutLog
import com.example.personalsport.data.model.GpsPoint
import com.example.personalsport.data.model.GpsRouteUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteMapPreviewTest {

    @Test
    fun testParseGpsPointsWithValidLatLngFormat() {
        val json = "[{\"lat\":40.4168,\"lng\":-3.7038},{\"lat\":40.4175,\"lng\":-3.7020}]"
        val points = GpsRouteUtils.parseGpsPoints(json)

        assertEquals(2, points.size)
        assertEquals(40.4168, points[0].lat, 0.0001)
        assertEquals(-3.7038, points[0].lng, 0.0001)
        assertEquals(40.4175, points[1].lat, 0.0001)
        assertEquals(-3.7020, points[1].lng, 0.0001)
    }

    @Test
    fun testParseGpsPointsWithLatitudeLongitudeFormat() {
        val json = "[{\"latitude\":39.4699,\"longitude\":-0.3763},{\"latitude\":39.4710,\"longitude\":-0.3740}]"
        val points = GpsRouteUtils.parseGpsPoints(json)

        assertEquals(2, points.size)
        assertEquals(39.4699, points[0].lat, 0.0001)
        assertEquals(-0.3763, points[0].lng, 0.0001)
    }

    @Test
    fun testParseGpsPointsWithNullOrInvalidInputs() {
        assertTrue(GpsRouteUtils.parseGpsPoints(null).isEmpty())
        assertTrue(GpsRouteUtils.parseGpsPoints("").isEmpty())
        assertTrue(GpsRouteUtils.parseGpsPoints("   ").isEmpty())
        assertTrue(GpsRouteUtils.parseGpsPoints("invalid_json").isEmpty())
    }

    @Test
    fun testEncodeAndParseGpsPointsRoundTrip() {
        val originalPoints = listOf(
            GpsPoint(40.4168, -3.7038),
            GpsPoint(40.4175, -3.7020),
            GpsPoint(40.4190, -3.7005)
        )

        val encoded = GpsRouteUtils.encodeGpsPoints(originalPoints)
        assertTrue(encoded.contains("\"lat\":40.4168"))
        assertTrue(encoded.contains("\"lng\":-3.7038"))

        val decoded = GpsRouteUtils.parseGpsPoints(encoded)
        assertEquals(originalPoints.size, decoded.size)
        for (i in originalPoints.indices) {
            assertEquals(originalPoints[i].lat, decoded[i].lat, 0.0001)
            assertEquals(originalPoints[i].lng, decoded[i].lng, 0.0001)
        }
    }

    @Test
    fun testGenerateSampleRunAndSwimRoutes() {
        val runRoute = GpsRouteUtils.generateSampleRunRoute()
        assertTrue("Run route should contain at least 2 points", runRoute.size >= 2)
        val runJson = GpsRouteUtils.generateSampleRunRouteJson()
        assertTrue(runJson.isNotBlank())
        assertEquals(runRoute.size, GpsRouteUtils.parseGpsPoints(runJson).size)

        val swimRoute = GpsRouteUtils.generateSampleOpenWaterSwimRoute()
        assertTrue("Swim route should contain at least 2 points", swimRoute.size >= 2)
        val swimJson = GpsRouteUtils.generateSampleOpenWaterSwimRouteJson()
        assertTrue(swimJson.isNotBlank())
        assertEquals(swimRoute.size, GpsRouteUtils.parseGpsPoints(swimJson).size)
    }

    @Test
    fun testWorkoutLogWithRouteGpsPointsAndOpenWaterType() {
        val runLog = WorkoutLog(
            date = System.currentTimeMillis(),
            workoutName = "Huawei Watch GT - Carrera Corta (5K)",
            durationMinutes = 25,
            routeGpsPoints = GpsRouteUtils.generateSampleRunRouteJson()
        )

        assertNotNull(runLog.routeGpsPoints)
        assertFalse(runLog.routeGpsPoints.isNullOrBlank())

        val swimLog = WorkoutLog(
            date = System.currentTimeMillis(),
            workoutName = "Natación en Aguas Abiertas",
            durationMinutes = 45,
            routeGpsPoints = GpsRouteUtils.generateSampleOpenWaterSwimRouteJson()
        )

        assertNotNull(swimLog.routeGpsPoints)
        assertTrue(swimLog.workoutName.contains("Aguas Abiertas", ignoreCase = true))
    }

    @Test
    fun testGpsRouteBoundsAndSpanCalculations() {
        val points = GpsRouteUtils.generateSampleRunRoute()
        val minLat = points.minOf { it.lat }
        val maxLat = points.maxOf { it.lat }
        val minLng = points.minOf { it.lng }
        val maxLng = points.maxOf { it.lng }

        assertTrue(maxLat > minLat)
        assertTrue(maxLng > minLng)

        val latSpan = maxLat - minLat
        val lngSpan = maxLng - minLng
        assertTrue(latSpan > 0.0)
        assertTrue(lngSpan > 0.0)
    }
}
