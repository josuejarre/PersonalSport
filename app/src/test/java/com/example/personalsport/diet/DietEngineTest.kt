package com.example.personalsport.diet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DietEngineTest {

    @Test
    fun testBmrCalculationWithKatchMcArdle() {
        // Body weight 80kg, 15% body fat => Lean Mass = 80 * 0.85 = 68 kg
        // Katch-McArdle: 370 + (21.6 * 68) = 370 + 1468.8 = 1838.8 => 1839 kcal
        val profile = BodyProfile(
            weightKg = 80f,
            heightCm = 180f,
            age = 25,
            gender = Gender.MALE,
            bodyFatPercentage = 15f
        )

        val bmr = DietEngine.calculateBmr(profile)
        assertEquals(1839, bmr)
    }

    @Test
    fun testBmrCalculationWithMifflinStJeor() {
        // Body weight 70kg, 170cm height, age 30, Male, bodyFat null
        // Mifflin-St Jeor: 10*70 + 6.25*170 - 5*30 + 5 = 700 + 1062.5 - 150 + 5 = 1617.5 => 1618 kcal
        val profile = BodyProfile(
            weightKg = 70f,
            heightCm = 170f,
            age = 30,
            gender = Gender.MALE,
            bodyFatPercentage = null
        )

        val bmr = DietEngine.calculateBmr(profile)
        assertEquals(1618, bmr)
    }

    @Test
    fun testTdeeCalculationWithWorkoutCalories() {
        // BMR = 1839, ActivityLevel.MODERATE (1.55), workoutActiveCalories = 400
        // Base TDEE = 1839 * 1.55 = 2850.45
        // Total TDEE = 2850.45 + 400 = 3250 kcal
        val profile = BodyProfile(
            weightKg = 80f,
            heightCm = 180f,
            age = 25,
            gender = Gender.MALE,
            bodyFatPercentage = 15f,
            activityLevel = ActivityLevel.MODERATE,
            workoutActiveCalories = 400
        )

        val tdee = DietEngine.calculateTdee(profile)
        assertEquals(3250, tdee)
    }

    @Test
    fun testMacroTargetsFatLoss() {
        val profile = BodyProfile(
            weightKg = 80f,
            heightCm = 180f,
            age = 25,
            gender = Gender.MALE,
            bodyFatPercentage = 15f,
            activityLevel = ActivityLevel.MODERATE,
            workoutActiveCalories = 400
        ) // TDEE = 3250

        val target = DietEngine.calculateTargetMacros(profile, DietGoal.FAT_LOSS)

        // Caloric deficit (20% lower than 3250 => 2600 kcal)
        assertEquals(2600, target.calories)
        // High protein: 80kg * 2.3 = 184g protein
        assertEquals(184, target.proteinGrams)
        // Fat: ~25% of 2600 = 650 kcal => 72g fat
        assertEquals(72, target.fatGrams)
        // Carbs: remainder calories
        assertTrue("Carbs should be positive and balanced", target.carbsGrams > 0)
    }

    @Test
    fun testMacroTargetsMuscleGain() {
        val profile = BodyProfile(
            weightKg = 75f,
            heightCm = 175f,
            age = 24,
            gender = Gender.MALE,
            bodyFatPercentage = 12f,
            activityLevel = ActivityLevel.ACTIVE,
            workoutActiveCalories = 500
        )

        val tdee = DietEngine.calculateTdee(profile)
        val target = DietEngine.calculateTargetMacros(profile, DietGoal.MUSCLE_GAIN)

        // Caloric surplus (15% above TDEE)
        assertTrue("Target calories should be greater than TDEE for muscle gain", target.calories > tdee)
        assertTrue("Protein should be high (>= 2g/kg)", target.proteinGrams >= (profile.weightKg * 2.0f).toInt())
        assertTrue("Carbohydrates should be high for hypertrophy", target.carbsGrams > 200)
    }

    @Test
    fun testMacroTargetsAthleticPerformance() {
        val profile = BodyProfile(
            weightKg = 70f,
            heightCm = 178f,
            age = 22,
            gender = Gender.FEMALE,
            bodyFatPercentage = 18f,
            activityLevel = ActivityLevel.ACTIVE,
            workoutActiveCalories = 600
        )

        val target = DietEngine.calculateTargetMacros(profile, DietGoal.ATHLETIC_PERFORMANCE)

        // Endurance carb loading (~55%+ of total calories)
        assertTrue("Carbs should be very high for athletic performance", target.carbsPercentage >= 50)
        assertTrue("Protein should be sufficient for endurance muscle repair", target.proteinGrams >= 120)
    }

    @Test
    fun testDailyMealPlanGeneration() {
        val profile = BodyProfile(
            weightKg = 78f,
            heightCm = 176f,
            age = 28,
            gender = Gender.MALE,
            bodyFatPercentage = 16f,
            activityLevel = ActivityLevel.MODERATE,
            workoutActiveCalories = 350
        )

        val mealPlan = DietEngine.generateDailyMealPlan(profile, DietGoal.FAT_LOSS)

        assertEquals(DietGoal.FAT_LOSS, mealPlan.goal)
        assertEquals(5, mealPlan.meals.size)

        // Verify each meal type exists
        val mealTypes = mealPlan.meals.map { it.mealType }
        assertTrue(mealTypes.contains(MealType.DESAYUNO))
        assertTrue(mealTypes.contains(MealType.ALMUERZO))
        assertTrue(mealTypes.contains(MealType.COMIDA))
        assertTrue(mealTypes.contains(MealType.MERIENDA))
        assertTrue(mealTypes.contains(MealType.CENA))

        // Check recipes and ingredients
        mealPlan.meals.forEach { meal ->
            assertTrue(meal.name.isNotEmpty())
            assertTrue(meal.calories > 0)
            assertTrue(meal.recipe.instructions.isNotEmpty())
            assertTrue(meal.recipe.baseIngredients.isNotEmpty())
            assertTrue(meal.scaledIngredients.isNotEmpty())
        }
    }

    @Test
    fun testGroceryListGeneration() {
        val profile = BodyProfile()
        val mealPlan = DietEngine.generateDailyMealPlan(profile, DietGoal.ATHLETIC_PERFORMANCE)

        val groceryCategories = DietEngine.generateGroceryList(mealPlan)

        assertNotNull(groceryCategories)
        assertTrue("Grocery list should have categorized items", groceryCategories.isNotEmpty())

        groceryCategories.forEach { category ->
            assertTrue(category.categoryName.isNotEmpty())
            assertTrue(category.items.isNotEmpty())
        }
    }
}
