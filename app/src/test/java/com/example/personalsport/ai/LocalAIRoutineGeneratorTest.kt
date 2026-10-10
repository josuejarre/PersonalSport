package com.example.personalsport.ai

import com.example.personalsport.diet.DietGoal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAIRoutineGeneratorTest {

    @Test
    fun generate3MonthPlan_generatesTwelveWeeksWithThreeMonths() = runTest {
        val config = AIRoutineWizardState(
            modality = RoutineModality.COMBINADO,
            selectedDisciplines = setOf(CardioDiscipline.MUSCULACION, CardioDiscipline.NATACION, CardioDiscipline.CARRERA),
            runningSubType = RunningSubType.ATLETISMO,
            athleticsSeriesCategory = AthleticsSeriesCategory.SHORT_SERIES,
            swimSeriesCategory = SwimDistanceSeriesCategory.SHORT_SERIES,
            includeCalisthenics = true,
            goalType = DietGoal.FAT_LOSS
        )

        val plan = LocalAIRoutineGenerator.generate3MonthPlan(config)

        assertNotNull(plan)
        assertEquals(3, plan.months.size)

        val allWeeks = plan.months.flatMap { it.weeks }
        assertEquals(12, allWeeks.size)

        allWeeks.forEach { week ->
            assertEquals(7, week.days.size) // Lunes to Domingo
        }

        // Verify Month titles
        assertTrue(plan.months[0].title.contains("Mes 1"))
        assertTrue(plan.months[1].title.contains("Mes 2"))
        assertTrue(plan.months[2].title.contains("Mes 3"))
    }

    @Test
    fun generate3MonthPlan_includesCalisthenicsAndMultiSportBlocks() = runTest {
        val config = AIRoutineWizardState(
            modality = RoutineModality.COMBINADO,
            selectedDisciplines = setOf(CardioDiscipline.MUSCULACION, CardioDiscipline.NATACION, CardioDiscipline.CARRERA),
            includeCalisthenics = true,
            selectedCalisthenicsExercises = setOf("Dominadas", "Flexiones", "Fondos en banco")
        )

        val plan = LocalAIRoutineGenerator.generate3MonthPlan(config)
        val week1Days = plan.months.first().weeks.first().days

        val monday = week1Days.find { it.dayName == "Lunes" }
        assertNotNull(monday)

        val calisthenicsBlock = monday!!.blocks.find { it.category.contains("Calistenia", ignoreCase = true) }
        assertNotNull(calisthenicsBlock)
        assertTrue(calisthenicsBlock!!.exercises.any { it.title.contains("Dominadas", ignoreCase = true) })

        val tuesday = week1Days.find { it.dayName == "Martes" }
        assertNotNull(tuesday)
        val swimBlock = tuesday!!.blocks.find { it.category.contains("Natación", ignoreCase = true) }
        assertNotNull(swimBlock)
    }

    @Test
    fun generate3MonthPlan_validatesCaloricEffectiveness() = runTest {
        val config = AIRoutineWizardState(
            modality = RoutineModality.COMBINADO,
            goalType = DietGoal.FAT_LOSS
        )

        val plan = LocalAIRoutineGenerator.generate3MonthPlan(config)

        assertTrue(plan.caloricValidation.isValidated)
        assertTrue(plan.caloricValidation.dailyExerciseCaloriesAvg > 0)
        assertTrue(plan.caloricValidation.statusTitle.isNotBlank())
        assertTrue(plan.caloricValidation.statusMessage.contains("déficit", ignoreCase = true))
    }

    @Test
    fun generate3MonthPlan_generatesSwimmingCombinationSeries_whenSelected() = runTest {
        val config = AIRoutineWizardState(
            modality = RoutineModality.COMBINADO,
            selectedDisciplines = setOf(CardioDiscipline.NATACION),
            swimSeriesCategory = SwimDistanceSeriesCategory.COMBINATION_SERIES
        )

        val plan = LocalAIRoutineGenerator.generate3MonthPlan(config)
        val tuesday = plan.months.first().weeks.first().days.find { it.dayName == "Martes" }

        assertNotNull(tuesday)
        val swimBlock = tuesday!!.blocks.find { it.category.contains("Natación", ignoreCase = true) }
        assertNotNull(swimBlock)

        val titles = swimBlock!!.exercises.map { it.title }

        // Must contain mixed short sprint sets, tempo medium sets, and long endurance sets
        assertTrue("Should contain short sprint sets", titles.any { it.contains("Cortas", ignoreCase = true) || it.contains("Sprint", ignoreCase = true) })
        assertTrue("Should contain medium tempo sets", titles.any { it.contains("Medias", ignoreCase = true) || it.contains("Tempo", ignoreCase = true) })
        assertTrue("Should contain long endurance sets", titles.any { it.contains("Larga", ignoreCase = true) || it.contains("Resistencia", ignoreCase = true) })
    }

    @Test
    fun generate3MonthPlan_rotatesStrengthMuscleSplitsAcrossDays() = runTest {
        val config = AIRoutineWizardState(
            modality = RoutineModality.MUSCULACION
        )

        val plan = LocalAIRoutineGenerator.generate3MonthPlan(config)
        val week1Days = plan.months.first().weeks.first().days

        // Día A: Lunes (Pecho, Hombro Frontal & Tríceps)
        val monday = week1Days.find { it.dayName == "Lunes" }!!
        assertEquals("Pecho, Hombro Frontal & Tríceps", monday.muscleFocus)
        val mondayExercises = monday.blocks.flatMap { it.exercises }.map { it.title }
        assertTrue(mondayExercises.any { it.contains("Press de Banca", ignoreCase = true) })
        assertTrue(mondayExercises.any { it.contains("Press Militar", ignoreCase = true) })
        assertTrue(mondayExercises.any { it.contains("Fondos", ignoreCase = true) })
        assertTrue(mondayExercises.any { it.contains("Extensiones de Tríceps", ignoreCase = true) })

        // Día B: Martes (Espalda, Hombro Posterior & Bíceps)
        val tuesday = week1Days.find { it.dayName == "Martes" }!!
        assertEquals("Espalda, Hombro Posterior & Bíceps", tuesday.muscleFocus)
        val tuesdayExercises = tuesday.blocks.flatMap { it.exercises }.map { it.title }
        assertTrue(tuesdayExercises.any { it.contains("Dominadas", ignoreCase = true) })
        assertTrue(tuesdayExercises.any { it.contains("Remo", ignoreCase = true) })
        assertTrue(tuesdayExercises.any { it.contains("Jalón al Pecho", ignoreCase = true) })
        assertTrue(tuesdayExercises.any { it.contains("Curl de Bíceps", ignoreCase = true) })

        // Día C: Miércoles (Pierna & Glúteos)
        val wednesday = week1Days.find { it.dayName == "Miércoles" }!!
        assertEquals("Pierna & Glúteos", wednesday.muscleFocus)
        val wednesdayExercises = wednesday.blocks.flatMap { it.exercises }.map { it.title }
        assertTrue(wednesdayExercises.any { it.contains("Prensa de Piernas", ignoreCase = true) })
        assertTrue(wednesdayExercises.any { it.contains("Sentadillas", ignoreCase = true) })
        assertTrue(wednesdayExercises.any { it.contains("Extensión de Cuádriceps", ignoreCase = true) })
        assertTrue(wednesdayExercises.any { it.contains("Cuestas", ignoreCase = true) })

        // Día D: Jueves (Core, Estabilidad & Calistenia)
        val thursday = week1Days.find { it.dayName == "Jueves" }!!
        assertEquals("Core, Estabilidad & Calistenia", thursday.muscleFocus)
        val thursdayExercises = thursday.blocks.flatMap { it.exercises }.map { it.title }
        assertTrue(thursdayExercises.any { it.contains("Dominadas Libres", ignoreCase = true) })
        assertTrue(thursdayExercises.any { it.contains("Abdominales", ignoreCase = true) })
        assertTrue(thursdayExercises.any { it.contains("Fondos", ignoreCase = true) })
        assertTrue(thursdayExercises.any { it.contains("Planchas", ignoreCase = true) })
    }
}
