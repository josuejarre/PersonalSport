package com.example.personalsport.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAISleepAnalyzerTest {

    @Test
    fun testExcelenteSleepQualityAndRecommendations() {
        val input = SleepAnalysisInput(
            hoursSlept = 8.2f,
            deepSleepPercentage = 22.5f,
            remSleepPercentage = 21.0f,
            restingHeartRateBpm = 52,
            dinnerTimingHoursBeforeBed = 3.5f,
            hasLateCaffeine = false,
            hasMagnesiumZincIntake = true,
            hydrationLiters = 2.8f,
            workoutIntensity = "MEDIA",
            athleteAge = 25,
            unloggedBackgroundSteps = 6500
        )

        val result = LocalAISleepAnalyzer.analyzeSleep(input)

        assertNotNull(result)
        assertEquals(SleepQualityBadge.EXCELENTE, result.qualityBadge)
        assertTrue(result.sleepScore >= 85)
        assertTrue(result.recommendations.isNotEmpty())
        assertTrue(result.dietSupplementFeedback.contains("Magnesio + Zinc"))
    }

    @Test
    fun testNecesitaRecuperacionSleepQuality() {
        val input = SleepAnalysisInput(
            hoursSlept = 4.5f,
            deepSleepPercentage = 11.0f,
            remSleepPercentage = 12.0f,
            restingHeartRateBpm = 78,
            dinnerTimingHoursBeforeBed = 1.0f,
            hasLateCaffeine = true,
            hasMagnesiumZincIntake = false,
            hydrationLiters = 1.2f,
            workoutIntensity = "ALTA",
            athleteAge = 35,
            unloggedBackgroundSteps = 12500
        )

        val result = LocalAISleepAnalyzer.analyzeSleep(input)

        assertNotNull(result)
        assertEquals(SleepQualityBadge.NECESITA_RECUPERACION, result.qualityBadge)
        assertTrue(result.sleepScore < 50)
        assertTrue(result.recommendations.any { it.contains("cafeína", ignoreCase = true) })
        assertTrue(result.recommendations.any { it.contains("cenar", ignoreCase = true) })
        assertTrue(result.recommendations.any { it.contains("7.5h", ignoreCase = true) })
    }

    @Test
    fun testNeatAndUnloggedStepsImpact() {
        val input = SleepAnalysisInput(
            hoursSlept = 6.0f,
            deepSleepPercentage = 16.0f,
            remSleepPercentage = 18.0f,
            restingHeartRateBpm = 62,
            unloggedBackgroundSteps = 11000,
            unloggedActiveMovements = "Paseos urbanos y baile"
        )

        val result = LocalAISleepAnalyzer.analyzeSleep(input)

        assertTrue(result.neatImpactSummary.contains("11000 pasos libres"))
        assertTrue(result.neatImpactSummary.contains("Paseos urbanos"))
        assertTrue(result.recommendations.any { it.contains("11000 pasos", ignoreCase = true) })
    }

    @Test
    fun testAIRecoveryCountdownCalculation_HighLoad() {
        val now = System.currentTimeMillis()
        val input = RecoveryInput(
            lastWorkoutTimestampMillis = now - (18 * 3600 * 1000L), // 18 hours ago
            workoutIntensity = "ALTA",
            workoutDurationMinutes = 75,
            unloggedBackgroundSteps = 11000,
            unloggedNeatMinutes = 50,
            sleepScore = 72,
            restingHeartRateBpm = 58,
            athleteAge = 32,
            currentTimeMillis = now
        )

        val countdown = AIRecoveryCountdown.calculateRecovery(input)

        assertNotNull(countdown)
        assertTrue(countdown.totalRecoveryRequiredHours >= 36)
        assertEquals(false, countdown.isFullyRecovered)
        assertTrue(countdown.formattedRemainingTime.startsWith("Restan: "))
        assertTrue(countdown.neatAnalysisSummary.contains("11000 pasos libres"))
        assertTrue(countdown.recommendedTimeWindow.isNotEmpty())
        assertTrue(countdown.recommendedNextSessionIntensity.isNotEmpty())
    }

    @Test
    fun testAIRecoveryCountdownCalculation_FullyRecovered() {
        val now = System.currentTimeMillis()
        val input = RecoveryInput(
            lastWorkoutTimestampMillis = now - (50 * 3600 * 1000L), // 50 hours ago
            workoutIntensity = "SUAVE",
            workoutDurationMinutes = 30,
            unloggedBackgroundSteps = 3000,
            sleepScore = 90,
            restingHeartRateBpm = 50,
            athleteAge = 24,
            currentTimeMillis = now
        )

        val countdown = AIRecoveryCountdown.calculateRecovery(input)

        assertNotNull(countdown)
        assertEquals(true, countdown.isFullyRecovered)
        assertEquals("Restan: 00h 00m", countdown.formattedRemainingTime)
        assertEquals(100, countdown.muscleRecoveryPercentage)
        assertTrue(countdown.recommendationBadgeText.contains("100% Recuperado"))
    }
}
