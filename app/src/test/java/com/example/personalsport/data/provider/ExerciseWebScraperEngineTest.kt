package com.example.personalsport.data.provider

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ExerciseWebScraperEngineTest {

    @Before
    fun setUp() {
        ExerciseJsonDatasetParser.clearCache()
    }

    @Test
    fun testIsScrapedImage_identifiesScrapedUrlsCorrectly() {
        val scrapedUrl = "file:///data/user/0/com.example.personalsport/files/scraped_exercises/exercise_a1b2c3d4.png"
        assertTrue("Should return true for scraped exercises local file URL", ExerciseWebScraperEngine.isScrapedImage(scrapedUrl))

        val datasetUrl = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/videos/0001-barbell_bench_press.gif"
        assertFalse("Should return false for standard GitHub raw dataset GIF URL", ExerciseWebScraperEngine.isScrapedImage(datasetUrl))

        val nanoBananaUrl = "file:///android_asset/nano_banana/nano_banana_chest.png"
        assertFalse("Should return false for Nano Banana asset URL", ExerciseWebScraperEngine.isScrapedImage(nanoBananaUrl))

        assertFalse("Should return false for null URL", ExerciseWebScraperEngine.isScrapedImage(null))
        assertFalse("Should return false for empty URL", ExerciseWebScraperEngine.isScrapedImage(""))
    }

    @Test
    fun testGetHash_producesConsistentDeterministicMd5() {
        val hash1 = ExerciseWebScraperEngine.getHash("Trote Progresivo de Activación", "Cardio")
        val hash2 = ExerciseWebScraperEngine.getHash("trote progresivo de activacion", "cardio")
        val hash3 = ExerciseWebScraperEngine.getHash("Skips A/B", "Piernas")

        assertNotNull("Hash 1 should not be null", hash1)
        assertTrue("Hash 1 should not be empty", hash1.isNotEmpty())
        assertEquals("Normalized case/accent inputs should produce identical hash", hash1, hash2)
        assertNotEquals("Different exercise queries should produce different hashes", hash1, hash3)
    }

    @Test
    fun testFindAndStyleExerciseMedia_forRequestedQueries() {
        val testQueries = listOf(
            "Trote Progresivo de Activación" to "Cardio",
            "Skips A/B" to "Piernas",
            "Cuestas Potentes" to "Cuádriceps",
            "Drill de Punto Muerto" to "Espalda / Dorsal",
            "Fondos en Banco" to "Tríceps / Pecho",
            "Remada de Tracción" to "Dorsal Ancho",
            "Press Inclinado" to "Pectoral Superior"
        )

        testQueries.forEach { (exerciseName, targetMuscle) ->
            val resultUrl = ExerciseWebScraperEngine.findAndStyleExerciseMedia(
                context = null,
                exerciseName = exerciseName,
                targetMuscle = targetMuscle
            )

            assertNotNull("Scraped media URL for '$exerciseName' should not be null", resultUrl)
            assertTrue("URL for '$exerciseName' should start with file://, found: $resultUrl", resultUrl.startsWith("file://"))
            assertTrue("URL for '$exerciseName' should contain 'realistic_exercise_' or 'scraped_exercises', found: $resultUrl", resultUrl.contains("realistic_exercise_") || resultUrl.contains("scraped_exercises"))
            assertTrue("URL for '$exerciseName' should end with .png, found: $resultUrl", resultUrl.endsWith(".png"))
        }
    }

    @Test
    fun testPoseTypeClassification_matchesBiomechanicalPoses() {
        assertEquals(
            ExerciseWebScraperEngine.ScrapedPoseType.RUNNING_ACTIVATION,
            ExerciseWebScraperEngine.determinePoseType("Trote Progresivo de Activación")
        )
        assertEquals(
            ExerciseWebScraperEngine.ScrapedPoseType.SKIPS_DRILL,
            ExerciseWebScraperEngine.determinePoseType("Skips A/B")
        )
        assertEquals(
            ExerciseWebScraperEngine.ScrapedPoseType.HILL_RUNNING,
            ExerciseWebScraperEngine.determinePoseType("Cuestas Potentes")
        )
        assertEquals(
            ExerciseWebScraperEngine.ScrapedPoseType.SWIM_DEADLOCK_DRILL,
            ExerciseWebScraperEngine.determinePoseType("Drill de Punto Muerto")
        )
        assertEquals(
            ExerciseWebScraperEngine.ScrapedPoseType.BENCH_DIPS,
            ExerciseWebScraperEngine.determinePoseType("Fondos en Banco")
        )
        assertEquals(
            ExerciseWebScraperEngine.ScrapedPoseType.ROW_TRACTION,
            ExerciseWebScraperEngine.determinePoseType("Remada de Tracción")
        )
        assertEquals(
            ExerciseWebScraperEngine.ScrapedPoseType.INCLINE_PRESS,
            ExerciseWebScraperEngine.determinePoseType("Press Inclinado")
        )
    }

    @Test
    fun testOpenRepositoriesQuerying_returnsRepositoryMedia() {
        val inclineMedia = ExerciseWebScraperEngine.queryOpenExerciseRepositories("Press Inclinado", "Pecho")
        assertNotNull("Query for Press Inclinado should return repository media", inclineMedia)
        assertTrue("Repository media should point to raw github dataset", inclineMedia?.contains("hasaneyldrm/exercises-dataset") == true)

        val dipsMedia = ExerciseWebScraperEngine.queryOpenExerciseRepositories("Fondos en Banco", "Tríceps")
        assertNotNull("Query for Fondos en Banco should return repository media", dipsMedia)
        assertTrue("Repository media should point to raw github dataset", dipsMedia?.contains("hasaneyldrm/exercises-dataset") == true)
    }

    @Test
    fun testIntegrationWithDatasetParser_unknownExerciseInvocations() {
        val unknownQuery = "Remada de Tracción Especial"
        assertFalse("Unknown exercise should not be in dataset", ExerciseJsonDatasetParser.isKnownExercise(unknownQuery))

        val item = ExerciseJsonDatasetParser.findExercise(unknownQuery)
        assertNotNull("Dataset parser should return fallback item with realistic_exercise URL", item)
        assertTrue(
            "Fallback item gif_url should be generated by RealisticExerciseIllustrationGenerator or WebScraper, found: ${item?.gif_url}",
            item?.gif_url?.contains("realistic_exercise_") == true || item?.gif_url?.contains("scraped_exercises") == true
        )

        val fullGifUrl = ExerciseJsonDatasetParser.getFullGifUrl(unknownQuery)
        assertTrue(
            "getFullGifUrl for unknown exercise should return realistic_exercise URL, found: $fullGifUrl",
            fullGifUrl.contains("realistic_exercise_") || fullGifUrl.contains("scraped_exercises")
        )

        val providerGifUrl = ExerciseGifDatasetProvider.getGifUrlForExercise(unknownQuery)
        assertTrue(
            "ExerciseGifDatasetProvider should return realistic_exercise URL for unknown exercise, found: $providerGifUrl",
            providerGifUrl.contains("realistic_exercise_") || providerGifUrl.contains("scraped_exercises")
        )
    }
}
