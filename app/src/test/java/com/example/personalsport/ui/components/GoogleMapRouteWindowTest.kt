package com.example.personalsport.ui.components

import com.example.personalsport.data.local.entity.WorkoutLog
import com.example.personalsport.data.model.GpsPoint
import com.example.personalsport.data.model.GpsRouteUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleMapRouteWindowTest {

    @Test
    fun testParseGpsRoutePointsAliasMethod() {
        val json = "[{\"lat\":40.4168,\"lng\":-3.7038},{\"lat\":40.4175,\"lng\":-3.7020}]"
        val points = GpsRouteUtils.parseGpsRoutePoints(json)

        assertEquals(2, points.size)
        assertEquals(40.4168, points[0].lat, 0.0001)
        assertEquals(-3.7038, points[0].lng, 0.0001)
    }

    @Test
    fun testCalculateTotalDistanceKmUsingHaversine() {
        val points = listOf(
            GpsPoint(40.4168, -3.7038),
            GpsPoint(40.4210, -3.6990),
            GpsPoint(40.4225, -3.7010)
        )
        val distanceKm = GpsRouteUtils.calculateTotalDistanceKm(points)
        assertTrue("Distance should be greater than 0 km", distanceKm > 0.0)
        assertTrue("Distance should be less than 5 km for close coordinates", distanceKm < 5.0)
    }

    @Test
    fun testCalculatePaceMinKm() {
        val pace = GpsRouteUtils.calculatePaceMinKm(5.0, 25.0) // 25 min for 5 km = 5'00" /km
        assertEquals("5'00\" /km", pace)

        val paceFast = GpsRouteUtils.calculatePaceMinKm(10.0, 40.0) // 40 min for 10 km = 4'00" /km
        assertEquals("4'00\" /km", paceFast)
    }

    @Test
    fun testCalculateElevationGainMeters() {
        val runPoints = GpsRouteUtils.generateSampleRunRoute()
        val elevation = GpsRouteUtils.calculateElevationGainMeters(runPoints)
        assertTrue("Elevation should be between 15 and 180 meters", elevation in 15..180)
    }

    @Test
    fun testFormatStartAndFinishTime() {
        val startMs = 1710000000000L // Specific timestamp
        val duration = 30
        val timeRange = GpsRouteUtils.formatStartAndFinishTime(startMs, duration)
        assertTrue(timeRange.contains("-"))
        val parts = timeRange.split("-")
        assertEquals(2, parts.size)
        assertTrue(parts[0].trim().matches(Regex("""\d{2}:\d{2}""")))
        assertTrue(parts[1].trim().matches(Regex("""\d{2}:\d{2}""")))
    }

    @Test
    fun testStartAndFinishMarkerSelectionFromGpsRoute() {
        val routePoints = GpsRouteUtils.generateSampleRunRoute()
        val startPoint = routePoints.first()
        val finishPoint = routePoints.last()

        assertNotNull(startPoint)
        assertNotNull(finishPoint)
        assertEquals(40.4168, startPoint.lat, 0.0001)
        assertEquals(-3.7038, startPoint.lng, 0.0001)
        assertEquals(40.4168, finishPoint.lat, 0.0001)
        assertEquals(-3.7038, finishPoint.lng, 0.0001)
    }

    @Test
    fun testOpenWaterSwimGpsRoutePoints() {
        val swimPoints = GpsRouteUtils.generateSampleOpenWaterSwimRoute()
        assertTrue("Swim route should have at least 2 points", swimPoints.size >= 2)
        val json = GpsRouteUtils.generateSampleOpenWaterSwimRouteJson()
        val parsed = GpsRouteUtils.parseGpsRoutePoints(json)
        assertEquals(swimPoints.size, parsed.size)
    }

    @Test
    fun testWorkoutLogIntegrationWithRouteGpsData() {
        val log = WorkoutLog(
            id = 101,
            date = System.currentTimeMillis(),
            workoutName = "Carrera de Atletismo 10K",
            durationMinutes = 45,
            routeGpsPoints = GpsRouteUtils.generateSampleRunRouteJson()
        )

        assertNotNull(log.routeGpsPoints)
        val points = GpsRouteUtils.parseGpsRoutePoints(log.routeGpsPoints)
        assertTrue(points.isNotEmpty())
        val dist = GpsRouteUtils.calculateTotalDistanceKm(points)
        assertTrue(dist >= 0.0)
    }
}
