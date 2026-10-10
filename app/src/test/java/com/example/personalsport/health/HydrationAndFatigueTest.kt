package com.example.personalsport.health

import androidx.compose.ui.graphics.Color
import com.example.personalsport.ui.health.ExerciseIntensity
import com.example.personalsport.ui.health.MuscleGroupStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HydrationAndFatigueTest {

    @Test
    fun testSweatLossCalculation() {
        val durationMins = 60
        val ambientTempC = 28
        val intensity = ExerciseIntensity.ALTA // 1000 ml/h base

        val baseRate = intensity.sweatRateMlPerHour
        val tempFactor = if (ambientTempC > 22) 1.0 + (ambientTempC - 22) * 0.03 else 1.0
        val sweatLossMl = ((baseRate * (durationMins / 60.0)) * tempFactor).toInt()

        assertTrue(sweatLossMl > 1000) // Temperature > 22C increases sweat loss above base 1000ml
    }

    @Test
    fun testMuscleGroupStatusColorsAndMessages() {
        val exhaustedMuscle = MuscleGroupStatus(
            id = "quads",
            name = "Cuádriceps",
            viewArea = "Frontal",
            recoveryPercent = 25,
            hoursRemaining = 36,
            primaryExercisesWorked = "Sentadillas Pesadas",
            stretchRecommendation = "Estiramiento de cuádriceps de pie"
        )

        assertEquals("Alta Fatiga (Descanso)", exhaustedMuscle.statusText)
        assertEquals(Color(0xFFC62828), exhaustedMuscle.statusColor)

        val moderateMuscle = MuscleGroupStatus(
            id = "biceps",
            name = "Bíceps",
            viewArea = "Frontal",
            recoveryPercent = 50,
            hoursRemaining = 12,
            primaryExercisesWorked = "Curl con barra",
            stretchRecommendation = "Extensión de brazo"
        )

        assertEquals("Fatiga Moderada", moderateMuscle.statusText)
        assertEquals(Color(0xFFF57F17), moderateMuscle.statusColor)

        val recoveredMuscle = MuscleGroupStatus(
            id = "abs",
            name = "Abdomen",
            viewArea = "Frontal",
            recoveryPercent = 95,
            hoursRemaining = 0,
            primaryExercisesWorked = "Planchas",
            stretchRecommendation = "Postura de Cobra"
        )

        assertEquals("100% Regenerado (Óptimo)", recoveredMuscle.statusText)
        assertEquals(Color(0xFF2E7D32), recoveredMuscle.statusColor)
    }
}
