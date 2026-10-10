package com.example.personalsport.ui.planner

import com.example.personalsport.ai.AIRoutineWizardState
import com.example.personalsport.ai.CardioDiscipline
import com.example.personalsport.ai.LocalAIRoutineGenerator
import com.example.personalsport.ai.RoutineModality
import com.example.personalsport.ai.SwimDistanceSeriesCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyWorkoutDashboardTest {

    @Test
    fun weeklyDashboard_providesAll7DaysWithSpecificFocusTitles() = runTest {
        val wizardState = AIRoutineWizardState(
            swimSeriesCategory = SwimDistanceSeriesCategory.COMBINATION_SERIES
        )
        val plan = LocalAIRoutineGenerator.generate3MonthPlan(wizardState)
        val week1 = plan.months.first().weeks.first()

        val weekdays = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        assertEquals(7, week1.days.size)

        weekdays.forEach { dayName ->
            val dayPlan = week1.days.find { it.dayName == dayName }
            assertNotNull("Day $dayName should exist in weekly plan", dayPlan)
            assertTrue("Day $dayName should have non-empty blocks or exercises", dayPlan!!.blocks.isNotEmpty())
        }
    }

    @Test
    fun weeklyDashboard_calculatesDayProgressAndCompletionCorrectly() = runTest {
        val wizardState = AIRoutineWizardState(
            swimSeriesCategory = SwimDistanceSeriesCategory.COMBINATION_SERIES
        )
        val plan = LocalAIRoutineGenerator.generate3MonthPlan(wizardState)
        val tuesday = plan.months.first().weeks.first().days.find { it.dayName == "Martes" }!!

        val exercises = tuesday.blocks.flatMap { it.exercises }
        assertTrue(exercises.isNotEmpty())

        val firstExerciseId = exercises.first().id
        val completedSet = setOf(firstExerciseId)

        val completedCount = exercises.count { completedSet.contains(it.id) }
        assertEquals(1, completedCount)

        val progressPercentage = (completedCount * 100) / exercises.size
        assertTrue(progressPercentage > 0)
    }

    @Test
    fun weeklyDashboard_detectsDisciplineBadgesCorrectly() = runTest {
        val wizardState = AIRoutineWizardState(
            modality = RoutineModality.COMBINADO,
            selectedDisciplines = setOf(CardioDiscipline.MUSCULACION, CardioDiscipline.NATACION, CardioDiscipline.CARRERA)
        )
        val plan = LocalAIRoutineGenerator.generate3MonthPlan(wizardState)
        val week1Days = plan.months.first().weeks.first().days

        // Track running day (Lunes)
        val monday = week1Days.find { it.dayName == "Lunes" }!!
        val mondayBadges = getDisciplineBadgesForDay(monday)
        assertTrue("Monday should contain Pista de Atletismo badge", mondayBadges.any { it.label == "Pista de Atletismo" })
        assertTrue("Monday should contain Gimnasio / Musculación badge", mondayBadges.any { it.label == "Gimnasio / Musculación" })
        assertTrue("Monday should NOT contain Piscina badge", mondayBadges.none { it.label == "Piscina" })

        // Swimming day (Martes)
        val tuesday = week1Days.find { it.dayName == "Martes" }!!
        val tuesdayBadges = getDisciplineBadgesForDay(tuesday)
        assertTrue("Tuesday should contain Piscina badge", tuesdayBadges.any { it.label == "Piscina" })
        assertTrue("Tuesday should contain Gimnasio / Musculación badge", tuesdayBadges.any { it.label == "Gimnasio / Musculación" })
        assertTrue("Tuesday should NOT contain Pista de Atletismo badge", tuesdayBadges.none { it.label == "Pista de Atletismo" })

        // Rest day (Domingo)
        val sunday = week1Days.find { it.dayName == "Domingo" }!!
        val sundayBadges = getDisciplineBadgesForDay(sunday)
        assertEquals(1, sundayBadges.size)
        assertEquals("Descanso Activo / Recuperación", sundayBadges.first().label)
    }

    @Test
    fun weeklyDashboard_displaysStrengthOnlyBadgesOnNonTrackNonPoolDays() = runTest {
        val wizardState = AIRoutineWizardState(
            modality = RoutineModality.MUSCULACION,
            selectedDisciplines = setOf(CardioDiscipline.MUSCULACION)
        )
        val plan = LocalAIRoutineGenerator.generate3MonthPlan(wizardState)
        val monday = plan.months.first().weeks.first().days.find { it.dayName == "Lunes" }!!

        val mondayBadges = getDisciplineBadgesForDay(monday)
        assertEquals(1, mondayBadges.size)
        assertEquals("Gimnasio / Musculación", mondayBadges.first().label)
        assertTrue("No Pista badge on gym-only day", mondayBadges.none { it.label == "Pista de Atletismo" })
        assertTrue("No Piscina badge on gym-only day", mondayBadges.none { it.label == "Piscina" })
    }
}
