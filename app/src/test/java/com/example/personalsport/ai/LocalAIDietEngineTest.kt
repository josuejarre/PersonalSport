package com.example.personalsport.ai

import com.example.personalsport.diet.ActivityLevel
import com.example.personalsport.diet.BodyProfile
import com.example.personalsport.diet.DietGoal
import com.example.personalsport.diet.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LocalAIDietEngineTest {

    private val sampleProfile = BodyProfile(
        weightKg = 78f,
        heightCm = 178f,
        age = 28,
        gender = Gender.MALE,
        bodyFatPercentage = 16f,
        activityLevel = ActivityLevel.MODERATE,
        workoutActiveCalories = 350
    )

    @Test
    fun test30DayPlanGeneration_returns30DaysWithValidProjections() {
        val plan = LocalAIDietEngine.generate30DayPlan(sampleProfile, DietGoal.BODY_RECOMP)

        assertNotNull(plan)
        assertEquals(30, plan.days.size)

        // Verify day numbers 1 to 30
        for (i in 0 until 30) {
            val day = plan.days[i]
            assertEquals(i + 1, day.dayNumber)
            assertEquals("Día ${i + 1}", day.dateLabel)
            assertTrue(day.calorieTarget > 1200)
            assertTrue(day.proteinTargetGrams > 100)
            assertTrue(day.carbsTargetGrams > 50)
            assertTrue(day.fatTargetGrams > 30)
            assertTrue(day.fiberTargetGrams >= 28)
            assertEquals(5, day.mealSlots.size)
        }

        // Verify 30-day cumulative weight change projection
        assertTrue(plan.projectedEndWeightKg > 0f)
        assertTrue(abs(plan.startingWeightKg - 78f) < 0.1f)
    }

    @Test
    fun testMealSlotAlternativesGeneration_hasMultipleChoicesWithMatchingMacros() {
        val dayPlan = LocalAIDietEngine.generate30DayPlan(sampleProfile, DietGoal.FAT_LOSS).days.first()

        for (slot in dayPlan.mealSlots) {
            // Verify each meal slot has at least 3 distinct recipe choices
            assertTrue("Slot ${slot.slotType.title} should have 3+ alternatives", slot.alternatives.size >= 3)

            val baseAlternative = slot.alternatives.first()
            val targetCalories = baseAlternative.calories
            val targetProtein = baseAlternative.proteinGrams
            val targetCarbs = baseAlternative.carbsGrams
            val targetFat = baseAlternative.fatGrams

            for (alternative in slot.alternatives) {
                // Verify recipe details, ingredients, prep time & satiety
                assertTrue(alternative.name.isNotEmpty())
                assertTrue(alternative.description.isNotEmpty())
                assertTrue(alternative.prepTimeMinutes > 0)
                assertTrue(alternative.ingredients.isNotEmpty())
                assertTrue(alternative.recipeInstructions.isNotEmpty())
                assertTrue(alternative.satietyIndexScore >= 7.5f)

                // Verify matching macro targets across alternative dish choices for IIFYM flexibility
                assertEquals(targetCalories, alternative.calories)
                assertEquals(targetProtein, alternative.proteinGrams)
                assertEquals(targetCarbs, alternative.carbsGrams)
                assertEquals(targetFat, alternative.fatGrams)
            }
        }
    }

    @Test
    fun testMicronutrientAndIIFYMDeficiencyPrevention_includesEssentialNutrients() {
        val plan = LocalAIDietEngine.generate30DayPlan(sampleProfile, DietGoal.MUSCLE_GAIN)

        for (day in plan.days) {
            val micros = day.dailyMicronutrients
            assertTrue("Iron should be sufficient", micros.ironMg >= 15f)
            assertTrue("Calcium should be sufficient", micros.calciumMg >= 800f)
            assertTrue("Vitamin C should be sufficient", micros.vitaminCMg >= 60f)
            assertTrue("Vitamin D should be sufficient", micros.vitaminDIu >= 400f)
            assertTrue("Omega-3 should be sufficient", micros.omega3Grams >= 1.5f)
            assertTrue("Fiber should prevent deficiency", micros.fiberGrams >= 25f)
        }
    }

    @Test
    fun testSupplementTimingSchedule_integratesWithMealSlots() {
        val fatLossSupps = LocalAIDietEngine.getSupplementTimingSchedule(DietGoal.FAT_LOSS)
        val muscleGainSupps = LocalAIDietEngine.getSupplementTimingSchedule(DietGoal.MUSCLE_GAIN)
        val recompSupps = LocalAIDietEngine.getSupplementTimingSchedule(DietGoal.BODY_RECOMP)

        assertTrue(fatLossSupps.isNotEmpty())
        assertTrue(muscleGainSupps.isNotEmpty())
        assertTrue(recompSupps.isNotEmpty())

        // Verify Creatine is scheduled for Desayuno
        val creatineItem = recompSupps.find { it.name.contains("Creatina") }
        assertNotNull(creatineItem)
        assertEquals(MealSlotType.DESAYUNO, creatineItem?.targetMealSlot)

        // Verify Whey Isolate is scheduled for Merienda / Post-workout
        val wheyItem = recompSupps.find { it.name.contains("Whey") }
        assertNotNull(wheyItem)
        assertEquals(MealSlotType.MERIENDA, wheyItem?.targetMealSlot)
    }
}
