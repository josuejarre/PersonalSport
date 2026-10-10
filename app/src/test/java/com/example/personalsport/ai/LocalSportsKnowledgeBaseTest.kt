package com.example.personalsport.ai

import com.example.personalsport.ui.equipment.BrandCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSportsKnowledgeBaseTest {

    @Test
    fun testRunningEngine_vdotPacesAndFormulaCalculations() {
        val vdot45Paces = LocalSportsKnowledgeBase.calculateVdotPaces(45f)

        assertNotNull(vdot45Paces)
        assertEquals(45f, vdot45Paces.vdotScore, 0.01f)
        assertTrue("Easy pace should contain /km", vdot45Paces.easyPaceMinKm.contains("/km"))
        assertTrue("Threshold pace should contain /km", vdot45Paces.thresholdPaceMinKm.contains("/km"))
        assertTrue("Interval pace should contain /km", vdot45Paces.intervalPaceMinKm.contains("/km"))
        assertTrue("Repetition pace should contain /km", vdot45Paces.repetitionPaceMinKm.contains("/km"))
        assertTrue("Advice should mention Jack Daniels", vdot45Paces.seriesAdviceEs.contains("Jack Daniels"))

        val estimatedVdot = LocalSportsKnowledgeBase.estimateVdotFrom5kTime(1200) // 20 minutes
        assertEquals(50f, estimatedVdot, 0.1f)
    }

    @Test
    fun testSwimmingEngine_cssPaceAndSwolfCalculations() {
        // 400m in 6:00 (360s), 200m in 2:40 (160s)
        val cssResult = LocalSportsKnowledgeBase.calculateCssPace(360, 160)

        assertNotNull(cssResult)
        assertTrue("CSS pace should be formatted as /100m", cssResult.cssFormatted.contains("/100m"))
        assertTrue("Endurance pace should be formatted as /100m", cssResult.endurancePaceFormatted.contains("/100m"))
        assertTrue("Sprint pace should be formatted as /100m", cssResult.sprintPaceFormatted.contains("/100m"))
        assertTrue("Should include recommended drills", cssResult.recommendedDrillsEs.isNotEmpty())

        val swolf = LocalSportsKnowledgeBase.calculateSwolfScore(18, 22) // 18 strokes + 22 seconds = 40
        assertEquals(40, swolf.swolfScore)
        assertTrue("Rating should be in Spanish", swolf.ratingEs.contains("Buena") || swolf.ratingEs.contains("Eficacia"))
    }

    @Test
    fun testStrengthEngine_covers11BrandsAndBarbellCompounds() {
        val catalog = LocalSportsKnowledgeBase.getStrengthExercisesCatalog()

        assertNotNull(catalog)
        assertTrue("Should contain multiple strength concepts", catalog.size >= 5)

        // Verify compound exercises
        val benchPress = catalog.find { it.nameEs.contains("Press de Banca") }
        assertNotNull(benchPress)
        assertEquals(MuscleGroupEs.PECHO, benchPress?.primaryMuscle)

        val squat = catalog.find { it.nameEs.contains("Sentadilla") }
        assertNotNull(squat)
        assertEquals(MuscleGroupEs.CUADRICEPS, squat?.primaryMuscle)

        // Verify brand compatibility covers supported brands
        val supportedBrands = BrandCatalog.supportedBrands
        assertEquals(11, supportedBrands.size)

        val allCompatibilities = catalog.flatMap { it.brandCompatibilities }.toSet()
        assertTrue("Should include Technogym", allCompatibilities.contains("Technogym"))
        assertTrue("Should include Life Fitness", allCompatibilities.contains("Life Fitness"))
        assertTrue("Should include Matrix", allCompatibilities.contains("Matrix"))
    }

    @Test
    fun testCalisthenicsEngine_progressionsFromBeginnerToAdvanced() {
        val progressions = LocalSportsKnowledgeBase.getCalisthenicsProgressions()

        assertNotNull(progressions)
        assertTrue("Should contain multiple progressions", progressions.size >= 3)

        val pullUps = progressions.find { it.exerciseNameEs.contains("Dominadas") }
        assertNotNull(pullUps)
        assertTrue("Should have progression steps", pullUps!!.progressionStepsEs.size >= 4)
        assertTrue("Should mention weighted option in Spanish", pullUps.weightedOptionEs.contains("lastre"))
    }

    @Test
    fun testNutritionEngine_highProteinRecipesAndSatietyIndex() {
        val recipes = LocalSportsKnowledgeBase.getCookbookRecipesCatalog()

        assertNotNull(recipes)
        assertTrue("Should contain recipes across categories", recipes.size >= 4)

        for (recipe in recipes) {
            assertTrue("Title should be in Spanish", recipe.titleEs.isNotBlank())
            assertTrue("Category should be in Spanish", recipe.categoryEs.isNotBlank())
            assertTrue("Calories should be > 0", recipe.calories > 0)
            assertTrue("Protein should be high", recipe.proteinGrams >= 20)
            assertTrue("Satiety index should be high", recipe.satietyIndexScore >= 8.0f)
            assertTrue("Ingredients should be non-empty", recipe.ingredientsEs.isNotEmpty())
            assertTrue("Instructions should be non-empty", recipe.instructionsEs.isNotEmpty())
        }
    }

    @Test
    fun testEnrichedBooks_HollywoodStars_MacroFriendly_Vegan_HighProtein_SwimSmooth_SportsNutrition() {
        // Hollywood Stars
        val hollywood = LocalSportsKnowledgeBase.getHollywoodStarsTrainings()
        assertTrue("Hollywood training catalog should not be empty", hollywood.isNotEmpty())
        assertTrue("Hollywood title should mention V-Taper", hollywood.first().titleEs.contains("V-Taper") || hollywood.first().titleEs.contains("Hollywood"))

        // Macro Friendly Cookbook
        val macroRecipes = LocalSportsKnowledgeBase.getMacroFriendlyRecipesCatalog()
        assertTrue("Macro friendly recipes should not be empty", macroRecipes.isNotEmpty())
        assertTrue("Should contain volumetric recipe", macroRecipes.any { it.categoryEs.contains("Macro Friendly") })

        // Vegan Cookbook for Athletes
        val veganRecipes = LocalSportsKnowledgeBase.getVeganAthleteRecipesCatalog()
        assertTrue("Vegan athlete recipes should not be empty", veganRecipes.isNotEmpty())
        assertTrue("Should contain plant-based protein sources", veganRecipes.any { it.titleEs.contains("Tempeh") || it.titleEs.contains("Lentejas") })

        // Swim Smooth Archetypes
        val archetypes = LocalSportsKnowledgeBase.getSwimSmoothArchetypesAndDrills()
        assertTrue("Swim Smooth archetypes should contain Overglider", archetypes.any { it.archetypeEs.contains("Overglider") })

        // Essentials of Sports Nutrition
        val essentials = LocalSportsKnowledgeBase.getSportsNutritionEssentials()
        assertNotNull(essentials)
        assertTrue("BMR formula should mention Katch-McArdle", essentials.bmrFormulaEs.contains("Katch-McArdle"))
    }

    @Test
    fun testBiologicalPromptContextGeneration() {
        val metrics = BiologicalMetrics(
            age = 30,
            gender = "Masculino",
            weightKg = 75.0f,
            bodyFatPercentage = 15.0f,
            visceralFat = 5.0f,
            boneMassKg = 3.3f,
            muscleMassKg = 36.0f
        )

        val biologicalPrompt = LocalSportsKnowledgeBase.buildBiologicalPromptContext(
            age = metrics.age,
            gender = metrics.gender,
            weightKg = metrics.weightKg,
            bodyFatPercentage = metrics.bodyFatPercentage,
            visceralFat = metrics.visceralFat,
            boneMassKg = metrics.boneMassKg,
            muscleMassKg = metrics.muscleMassKg
        )

        assertNotNull(biologicalPrompt)
        assertTrue(biologicalPrompt.contains("PARÁMETROS BIOLÓGICOS DEL ATLETA"))
        assertTrue(biologicalPrompt.contains("30 años"))
        assertTrue(biologicalPrompt.contains("75.0 kg"))
        assertTrue(biologicalPrompt.contains("15.0 %"))
        assertTrue(biologicalPrompt.contains("Nivel 5.0") || biologicalPrompt.contains("5.0"))
        assertTrue(biologicalPrompt.contains("36.0 kg"))

        val ragWithMetrics = LocalSportsKnowledgeBase.getKnowledgeContext("fuerza", metrics)
        assertTrue(ragWithMetrics.contains("PARÁMETROS BIOLÓGICOS DEL ATLETA"))
        assertTrue(ragWithMetrics.contains("FUERZA Y MAQUINARIA"))
    }

    @Test
    fun testSpanishGuaranteeAndRAGContextRetrieval() {
        val directive = LocalSportsKnowledgeBase.SPANISH_SYSTEM_DIRECTIVE
        assertTrue(directive.contains("EXCLUSIVAMENTE EN ESPAÑOL"))

        val runningContext = LocalSportsKnowledgeBase.getKnowledgeContext("carrera")
        assertTrue(runningContext.contains("CARRERA Y ATLETISMO"))
        assertTrue(runningContext.contains("DANIELS"))

        val swimmingContext = LocalSportsKnowledgeBase.getKnowledgeContext("swim")
        assertTrue(swimmingContext.contains("NATACIÓN"))
        assertTrue(swimmingContext.contains("SWIM SMOOTH"))

        val strengthContext = LocalSportsKnowledgeBase.getKnowledgeContext("fuerza")
        assertTrue(strengthContext.contains("FUERZA Y MAQUINARIA"))
        assertTrue(strengthContext.contains("STARTING STRENGTH"))

        val calisthenicsContext = LocalSportsKnowledgeBase.getKnowledgeContext("calistenia")
        assertTrue(calisthenicsContext.contains("CALISTENIA"))
        assertTrue(calisthenicsContext.contains("OVERCOMING GRAVITY"))

        val nutritionContext = LocalSportsKnowledgeBase.getKnowledgeContext("dieta")
        assertTrue(nutritionContext.contains("NUTRICIÓN DEPORTIVA"))
        assertTrue(nutritionContext.contains("HIGH PROTEIN"))
    }
}
