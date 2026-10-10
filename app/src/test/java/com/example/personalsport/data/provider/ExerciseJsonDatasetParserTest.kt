package com.example.personalsport.data.provider

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ExerciseJsonDatasetParserTest {

    private val baseUrl = ExerciseJsonDatasetParser.BASE_GITHUB_RAW_URL

    @Before
    fun setUp() {
        ExerciseJsonDatasetParser.clearCache()
    }

    @Test
    fun testExactGifUrlForBenchPress() {
        val enUrl = ExerciseJsonDatasetParser.getFullGifUrl("Barbell Bench Press")
        val esUrl = ExerciseJsonDatasetParser.getFullGifUrl("Press de Banca")

        val expected = "${baseUrl}videos/0001-barbell_bench_press.gif"
        assertEquals(expected, enUrl)
        assertEquals(expected, esUrl)
    }

    @Test
    fun testExactGifUrlForInclineBenchPress() {
        val enUrl = ExerciseJsonDatasetParser.getFullGifUrl("Incline Barbell Bench Press")
        val esUrl = ExerciseJsonDatasetParser.getFullGifUrl("Press Inclinado con Barra")

        val expected = "${baseUrl}videos/0002-incline_barbell_press.gif"
        assertEquals(expected, enUrl)
        assertEquals(expected, esUrl)
    }

    @Test
    fun testExactGifUrlForSquat() {
        val enUrl = ExerciseJsonDatasetParser.getFullGifUrl("Barbell Squat")
        val esUrl = ExerciseJsonDatasetParser.getFullGifUrl("Sentadilla")

        val expected = "${baseUrl}videos/0040-barbell_squat.gif"
        assertEquals(expected, enUrl)
        assertEquals(expected, esUrl)
    }

    @Test
    fun testExactGifUrlForPullUp() {
        val enUrl = ExerciseJsonDatasetParser.getFullGifUrl("Pull-up")
        val esUrl = ExerciseJsonDatasetParser.getFullGifUrl("Dominadas")

        val expected = "${baseUrl}videos/0024-pull_up.gif"
        assertEquals(expected, enUrl)
        assertEquals(expected, esUrl)
    }

    @Test
    fun testExactGifUrlForSitUp() {
        val enUrl34 = ExerciseJsonDatasetParser.getFullGifUrl("3/4 Sit-up")
        val enUrl = ExerciseJsonDatasetParser.getFullGifUrl("Sit Up")
        val esUrl = ExerciseJsonDatasetParser.getFullGifUrl("Abdominales Crunch")

        val expected = "${baseUrl}videos/0001-2gPfomN.gif"
        assertEquals(expected, enUrl34)
        assertEquals(expected, enUrl)
        assertEquals(expected, esUrl)
    }

    @Test
    fun testExactGifUrlForLatPulldown() {
        val enUrl = ExerciseJsonDatasetParser.getFullGifUrl("Lat Pulldown")
        val esUrl = ExerciseJsonDatasetParser.getFullGifUrl("Jalón al Pecho")

        val expected = "${baseUrl}videos/0020-lat_pulldown.gif"
        assertEquals(expected, enUrl)
        assertEquals(expected, esUrl)
    }

    @Test
    fun testExactGifUrlForDips() {
        val enUrl = ExerciseJsonDatasetParser.getFullGifUrl("Dips")
        val esUrl = ExerciseJsonDatasetParser.getFullGifUrl("Fondos en Paralelas")

        val expected = "${baseUrl}videos/0075-triceps_dips.gif"
        assertEquals(expected, enUrl)
        assertEquals(expected, esUrl)
    }

    @Test
    fun testExactGifUrlForShoulderPress() {
        val enUrl = ExerciseJsonDatasetParser.getFullGifUrl("Overhead Press")
        val esUrl = ExerciseJsonDatasetParser.getFullGifUrl("Press Militar")

        val expected = "${baseUrl}videos/0060-overhead_press.gif"
        assertEquals(expected, enUrl)
        assertEquals(expected, esUrl)
    }

    @Test
    fun testInstructionStepsTranslatedToSpanish() {
        val sitUpSteps = ExerciseJsonDatasetParser.getInstructionStepsEs("3/4 Sit-up")
        assertFalse("Sit-up steps in Spanish should not be empty", sitUpSteps.isEmpty())
        assertTrue(
            "Sit-up step should contain Spanish text",
            sitUpSteps.any { it.contains("espalda") || it.contains("rodillas") || it.contains("abdominales") || it.contains("Recuéstate") }
        )

        val benchSteps = ExerciseJsonDatasetParser.getInstructionStepsEs("Barbell Bench Press")
        assertFalse("Bench Press steps in Spanish should not be empty", benchSteps.isEmpty())
        assertTrue(
            "Bench Press step should contain Spanish text",
            benchSteps.any { it.contains("banco") || it.contains("barra") || it.contains("pecho") || it.contains("Recuéstate") }
        )
    }

    @Test
    fun testParseJsonDirectly() {
        val json = """
            [
              {
                "id": "0001",
                "name": "3/4 Sit-up",
                "gif_url": "videos/0001-2gPfomN.gif",
                "instruction_steps": ["Paso 1", "Paso 2"]
              }
            ]
        """.trimIndent()

        val parsed = ExerciseJsonDatasetParser.parseJson(json)
        assertEquals(1, parsed.size)
        assertEquals("0001", parsed[0].id)
        assertEquals("3/4 Sit-up", parsed[0].name)
        assertEquals("videos/0001-2gPfomN.gif", parsed[0].gif_url)
        assertEquals(2, parsed[0].instruction_steps.size)
    }

    @Test
    fun testLegExtensionMapping_quadricepsNotSitUp() {
        val testQueries = listOf(
            "Extensión de Cuádriceps en Máquina",
            "Extensión de Cuádriceps",
            "Cuádriceps en Máquina",
            "Extensión de Cuadriceps"
        )

        testQueries.forEach { query ->
            val exercise = ExerciseJsonDatasetParser.findExercise(query)
            assertNotNull("Exercise for '$query' should not be null", exercise)
            assertEquals("Leg Extension", exercise?.name)
            assertEquals("Quads", exercise?.target)
            assertNotEquals("3/4 Sit-up", exercise?.name)
            assertTrue("GIF URL should contain leg extension", exercise?.gif_url?.contains("leg_extension") == true)
        }
    }

    @Test
    fun testExhaustiveBiomechanicalMapping_allSpanishTitles() {
        val mappings = listOf(
            "Extensión de Cuádriceps" to "Leg Extension",
            "Cuádriceps en Máquina" to "Leg Extension",
            "Prensa de Piernas" to "Leg Press",
            "Sentadillas" to "Barbell Squat",
            "Sentadilla con Barra" to "Barbell Squat",
            "Press de Banca Horizontal" to "Barbell Bench Press",
            "Press de Banca Inclinado" to "Incline Barbell Bench Press",
            "Dominadas" to "Pull-up",
            "Jalón al Pecho" to "Lat Pulldown",
            "Remo con Barra" to "Barbell Row",
            "Press Militar" to "Overhead Press",
            "Curl de Bíceps" to "Dumbbell Curl",
            "Extensiones de Tríceps" to "Cable Triceps Pushdown",
            "Fondos en Paralelas" to "Dips",
            "Carrera" to "Running",
            "Sprint" to "Interval Sprints",
            "Series x 400m" to "Interval Sprints",
            "Fartlek" to "Fartlek Run",
            "Natación" to "Freestyle Swimming",
            "Crol" to "Freestyle Swimming",
            "Abdominales" to "3/4 Sit-up",
            "Crunch" to "3/4 Sit-up"
        )

        mappings.forEach { (spanishTitle, expectedEnglishName) ->
            val item = ExerciseJsonDatasetParser.findExercise(spanishTitle)
            assertNotNull("Item for '$spanishTitle' should not be null", item)
            assertEquals("Spanish title '$spanishTitle' should map to '$expectedEnglishName'", expectedEnglishName, item?.name)

            if (expectedEnglishName != "3/4 Sit-up") {
                assertNotEquals("'$spanishTitle' MUST NOT map to 3/4 Sit-up!", "3/4 Sit-up", item?.name)
            }
        }
    }

    @Test
    fun testCategoryBasedFallback_neverDefaultToSitUp() {
        // Pierna / Leg -> Fallback to Leg Extension or Squat
        val legFallback = ExerciseJsonDatasetParser.findExercise("Rutina Desconocida de Pierna", categoryHint = "Pierna")
        assertNotNull(legFallback)
        assertTrue(
            "Leg fallback should be Leg Extension or Squat, found: ${legFallback?.name}",
            legFallback?.name == "Leg Extension" || legFallback?.name == "Barbell Squat"
        )
        assertNotEquals("Leg fallback must NOT be 3/4 Sit-up", "3/4 Sit-up", legFallback?.name)

        // Pecho / Chest -> Fallback to Bench Press
        val chestFallback = ExerciseJsonDatasetParser.findExercise("Ejercicio Desconocido Pecho", categoryHint = "Pecho")
        assertNotNull(chestFallback)
        assertEquals("Barbell Bench Press", chestFallback?.name)

        // Espalda / Back -> Fallback to Lat Pulldown or Pull-up
        val backFallback = ExerciseJsonDatasetParser.findExercise("Movimiento de Espalda Misterioso", categoryHint = "Espalda")
        assertNotNull(backFallback)
        assertTrue(
            "Back fallback should be Lat Pulldown or Pull-up, found: ${backFallback?.name}",
            backFallback?.name == "Lat Pulldown" || backFallback?.name == "Pull-up"
        )

        // Hombro / Shoulder -> Fallback to Overhead Press
        val shoulderFallback = ExerciseJsonDatasetParser.findExercise("Trabajo de Hombro Raro", categoryHint = "Hombro")
        assertNotNull(shoulderFallback)
        assertEquals("Overhead Press", shoulderFallback?.name)

        // Brazos / Arms -> Fallback to Dumbbell Curl or Triceps Pushdown
        val armsFallback = ExerciseJsonDatasetParser.findExercise("Fuerza de Brazo Inexistente", categoryHint = "Brazos")
        assertNotNull(armsFallback)
        assertTrue(
            "Arms fallback should be Dumbbell Curl or Triceps Pushdown, found: ${armsFallback?.name}",
            armsFallback?.name == "Dumbbell Curl" || armsFallback?.name == "Cable Triceps Pushdown"
        )

        // Cardio / Carrera / Natación -> Fallback to Running or Swim
        val cardioFallback = ExerciseJsonDatasetParser.findExercise("Cardio Extremo", categoryHint = "Cardio")
        assertNotNull(cardioFallback)
        assertEquals("Running", cardioFallback?.name)

        val swimFallback = ExerciseJsonDatasetParser.findExercise("Agua Profunda", categoryHint = "Natación")
        assertNotNull(swimFallback)
        assertEquals("Freestyle Swimming", swimFallback?.name)

        // Abdominales / Core -> Fallback to 3/4 Sit-up
        val absFallback = ExerciseJsonDatasetParser.findExercise("Core Raro", categoryHint = "Abdominales")
        assertNotNull(absFallback)
        assertEquals("3/4 Sit-up", absFallback?.name)

        // Completely unknown exercise name without any context
        val totalUnknown = ExerciseJsonDatasetParser.findExercise("XYZ_COMPLETELY_UNKNOWN_EXERCISE_999")
        assertNotNull(totalUnknown)
        assertNotEquals("Completely unknown exercise MUST NEVER default to 3/4 Sit-up!", "3/4 Sit-up", totalUnknown?.name)
        assertTrue(
            "Completely unknown exercise should default to Squat or Bench Press, found: ${totalUnknown?.name}",
            totalUnknown?.name == "Barbell Squat" || totalUnknown?.name == "Barbell Bench Press"
        )
    }
}
