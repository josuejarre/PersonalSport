package com.example.personalsport.ai

import com.example.personalsport.data.local.entity.BodyMetric
import com.example.personalsport.data.local.entity.Equipment
import com.example.personalsport.data.local.entity.WorkoutLog
import com.example.personalsport.ui.planner.SportType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAIFitnessEngineTest {

    @Test
    fun testGenerateAthletics400mIntervalsRoutine() {
        val equipment = listOf(Equipment(name = "Spikes Pista Atletismo", description = "Zapatillas de clavos"))
        val history = listOf(
            WorkoutLog(date = 1000L, workoutName = "Running 5k", durationMinutes = 25),
            WorkoutLog(date = 2000L, workoutName = "Series 400m", durationMinutes = 30)
        )

        val routine = LocalAIFitnessEngine.generateWorkoutRoutine(
            sportType = SportType.ATHLETICS_SERIES,
            athleticsSeriesType = AthleticsSeriesType.INTERVALS_400M,
            targetDurationMinutes = 45,
            intensity = "High / Intense",
            equipmentList = equipment,
            workoutHistory = history
        )

        assertEquals(SportType.ATHLETICS_SERIES, routine.sportType)
        assertTrue(routine.title.contains("400m"))
        assertTrue(routine.mainSet.isNotEmpty())
        val mainStep = routine.mainSet.first()
        assertTrue(mainStep.title.contains("400m"))
        assertEquals("Spikes Pista Atletismo", mainStep.equipmentUsed)
    }

    @Test
    fun testGenerateAthletics1000mRepetitionsRoutine() {
        val routine = LocalAIFitnessEngine.generateWorkoutRoutine(
            sportType = SportType.ATHLETICS_SERIES,
            athleticsSeriesType = AthleticsSeriesType.REPETITIONS_1000M,
            targetDurationMinutes = 50,
            intensity = "Moderate",
            equipmentList = emptyList(),
            workoutHistory = emptyList()
        )

        assertEquals(SportType.ATHLETICS_SERIES, routine.sportType)
        assertTrue(routine.title.contains("1000m"))
        assertTrue(routine.mainSet.any { it.title.contains("1000m") })
    }

    @Test
    fun testGenerateAthleticsPyramidSeriesRoutine() {
        val routine = LocalAIFitnessEngine.generateWorkoutRoutine(
            sportType = SportType.ATHLETICS_SERIES,
            athleticsSeriesType = AthleticsSeriesType.PYRAMID_SERIES,
            targetDurationMinutes = 40,
            intensity = "Moderate",
            equipmentList = emptyList(),
            workoutHistory = emptyList()
        )

        assertEquals(SportType.ATHLETICS_SERIES, routine.sportType)
        assertTrue(routine.title.contains("Piramidales"))
        assertTrue(routine.mainSet.any { it.title.contains("Pirámide") })
    }

    @Test
    fun testGenerateAthleticsFartlekRoutine() {
        val routine = LocalAIFitnessEngine.generateWorkoutRoutine(
            sportType = SportType.ATHLETICS_SERIES,
            athleticsSeriesType = AthleticsSeriesType.FARTLEK,
            targetDurationMinutes = 45,
            intensity = "High / Intense",
            equipmentList = emptyList(),
            workoutHistory = emptyList()
        )

        assertEquals(SportType.ATHLETICS_SERIES, routine.sportType)
        assertTrue(routine.title.contains("Fartlek"))
        assertTrue(routine.mainSet.any { it.title.contains("Fartlek") })
    }

    @Test
    fun testGenerateAthleticsSprintSeriesRoutine() {
        val routine = LocalAIFitnessEngine.generateWorkoutRoutine(
            sportType = SportType.ATHLETICS_SERIES,
            athleticsSeriesType = AthleticsSeriesType.SPRINT_SERIES,
            targetDurationMinutes = 35,
            intensity = "High / Intense",
            equipmentList = emptyList(),
            workoutHistory = emptyList()
        )

        assertEquals(SportType.ATHLETICS_SERIES, routine.sportType)
        assertTrue(routine.title.contains("Velocidad Pura"))
        assertTrue(routine.mainSet.any { it.title.contains("100m Sprints") })
    }

    @Test
    fun testGenerateAthleticsRecoveryPacesRoutine() {
        val routine = LocalAIFitnessEngine.generateWorkoutRoutine(
            sportType = SportType.ATHLETICS_SERIES,
            athleticsSeriesType = AthleticsSeriesType.RECOVERY_PACES,
            targetDurationMinutes = 45,
            intensity = "Easy / Recovery",
            equipmentList = emptyList(),
            workoutHistory = emptyList()
        )

        assertEquals(SportType.ATHLETICS_SERIES, routine.sportType)
        assertTrue(routine.title.contains("Recuperación"))
        assertTrue(routine.mainSet.any { it.title.contains("Zona 1-2") })
    }

    @Test
    fun testGenerateStrengthRoutineAdaptsToEquipment() {
        val equipment = listOf(
            Equipment(name = "Mancuernas 15kg", description = "Dumbbells"),
            Equipment(name = "Cinta Treadmill Pro", description = "Treadmill")
        )

        val routine = LocalAIFitnessEngine.generateWorkoutRoutine(
            sportType = SportType.STRENGTH,
            targetDurationMinutes = 45,
            intensity = "Moderate",
            equipmentList = equipment,
            workoutHistory = emptyList()
        )

        assertEquals(SportType.STRENGTH, routine.sportType)
        assertTrue(routine.mainSet.any { it.equipmentUsed?.contains("Mancuernas") == true })
        assertTrue(routine.mainSet.any { it.equipmentUsed?.contains("Treadmill") == true })
    }

    @Test
    fun testAnalyzeBodyMetrics_BodyFatReduction() {
        val now = System.currentTimeMillis()
        val metrics = listOf(
            BodyMetric(id = 1, date = now - 14 * 86400000L, weightKg = 75.0f, bodyFatPercentage = 20.0f),
            BodyMetric(id = 2, date = now, weightKg = 74.2f, bodyFatPercentage = 18.8f) // Reduced fat by 1.2%
        )

        val insight = LocalAIFitnessEngine.analyzeBodyMetrics(metrics)

        assertNotNull(insight)
        assertEquals(InsightTrendType.FAT_LOSS, insight.trendType)
        assertTrue(insight.summary.contains("Grasa reducida 1.2%"))
        assertTrue(insight.summary.contains("hipertrofia"))
        assertNotNull(insight.fatChangeDeltaPct)
        assertEquals(-1.2f, insight.fatChangeDeltaPct!!, 0.05f)
    }

    @Test
    fun testAnalyzeBodyMetrics_MuscleGain() {
        val now = System.currentTimeMillis()
        val metrics = listOf(
            BodyMetric(id = 1, date = now - 30 * 86400000L, weightKg = 70.0f, bodyFatPercentage = 15.0f), // Lean mass = 59.5 kg
            BodyMetric(id = 2, date = now, weightKg = 71.5f, bodyFatPercentage = 14.5f) // Lean mass = 61.13 kg (+1.63 kg)
        )

        val insight = LocalAIFitnessEngine.analyzeBodyMetrics(metrics)

        assertNotNull(insight)
        assertEquals(InsightTrendType.MUSCLE_GAIN, insight.trendType)
        assertTrue(insight.summary.contains("Ganancia muscular"))
        assertNotNull(insight.estimatedLeanMassChangeKg)
        assertTrue(insight.estimatedLeanMassChangeKg!! > 0.5f)
    }

    @Test
    fun testAnalyzeBodyMetrics_EmptyAndSingle() {
        val emptyInsight = LocalAIFitnessEngine.analyzeBodyMetrics(emptyList())
        assertEquals(InsightTrendType.MAINTENANCE, emptyInsight.trendType)
        assertTrue(emptyInsight.summary.contains("Sin métricas"))

        val singleInsight = LocalAIFitnessEngine.analyzeBodyMetrics(listOf(BodyMetric(id = 1, date = 1000L, weightKg = 80.0f, bodyFatPercentage = 22.0f)))
        assertEquals(InsightTrendType.MAINTENANCE, singleInsight.trendType)
        assertTrue(singleInsight.summary.contains("80.0 kg"))
    }
}
