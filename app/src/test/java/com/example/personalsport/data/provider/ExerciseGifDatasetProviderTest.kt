package com.example.personalsport.data.provider

import org.junit.Assert.*
import org.junit.Test

class ExerciseGifDatasetProviderTest {

    @Test
    fun testDatasetSize_atLeast100Exercises() {
        val exercises = ExerciseGifDatasetProvider.getAllExercises()
        assertTrue("Dataset should contain at least 100 exercises, found: ${exercises.size}", exercises.size >= 100)
    }

    @Test
    fun testResolveGifUrl_relativeAndFullPaths() {
        val relative = "videos/0001-2gPfomN.gif"
        val resolvedRelative = ExerciseGifDatasetProvider.resolveGifUrl(relative)
        assertEquals(
            "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/videos/0001-2gPfomN.gif",
            resolvedRelative
        )

        val fullUrl = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/videos/0001-2gPfomN.gif"
        val resolvedFull = ExerciseGifDatasetProvider.resolveGifUrl(fullUrl)
        assertEquals(fullUrl, resolvedFull)

        val fallbackNull = ExerciseGifDatasetProvider.resolveGifUrl(null)
        assertEquals(ExerciseGifDatasetProvider.FALLBACK_GIF_URL, fallbackNull)

        val fallbackBlank = ExerciseGifDatasetProvider.resolveGifUrl("")
        assertEquals(ExerciseGifDatasetProvider.FALLBACK_GIF_URL, fallbackBlank)
    }

    @Test
    fun testOfflineFallbackUrl() {
        val fallback = ExerciseGifDatasetProvider.getOfflineFallbackGifUrl()
        assertEquals("file:///android_asset/fallback_exercise.gif", fallback)
    }

    @Test
    fun testBilingualLookup_spanishAndEnglishExerciseNames() {
        // Test key exercise mappings requested: running, interval series, sprints, squats, bench press, lat pulldowns, dips, pull-ups, push-ups, deadlifts, curls
        val requiredExercises = listOf(
            "Press de Banca" to "Barbell Bench Press",
            "Jalón al Pecho" to "Lat Pulldown",
            "Sentadilla" to "Barbell Squat",
            "Dominadas" to "Pull-Up",
            "Prensa de Piernas" to "Leg Press",
            "Press Militar" to "Overhead Press",
            "Fondos en Paralelas" to "Triceps Dips",
            "Peso Muerto" to "Deadlift",
            "Curl de Bíceps" to "Dumbbell Curl",
            "Extensiones de Tríceps" to "Cable Triceps Pushdown",
            "Flexiones de Pecho" to "Push-Up",
            "Trote y Carrera de Resistencia" to "Running",
            "Sprints e Intervalos de Alta Velocidad" to "Interval Sprints"
        )

        requiredExercises.forEach { (esName, enName) ->
            val esUrl = ExerciseGifDatasetProvider.getGifUrlForExercise(esName)
            val enUrl = ExerciseGifDatasetProvider.getGifUrlForExercise(enName)

            assertNotNull("GIF URL for Spanish name '$esName' should not be null", esUrl)
            assertNotNull("GIF URL for English name '$enName' should not be null", enUrl)
            assertEquals(
                "Spanish name '$esName' and English name '$enName' should resolve to the same GIF URL",
                esUrl,
                enUrl
            )
            assertTrue(
                "GIF URL '$esUrl' for '$esName' should start with base raw GitHub dataset URL",
                esUrl.startsWith(ExerciseGifDatasetProvider.BASE_RAW_URL)
            )
        }
    }

    @Test
    fun testExactGifMappingsForCoreExercises() {
        val baseUrl = ExerciseGifDatasetProvider.BASE_RAW_URL

        // 1. Bench Press / Press de Banca Horizontal
        val benchPressUrlEs = ExerciseGifDatasetProvider.getGifUrlForExercise("Press de Banca Horizontal")
        val benchPressUrlEn = ExerciseGifDatasetProvider.getGifUrlForExercise("Barbell Bench Press")
        assertEquals("${baseUrl}videos/0001-barbell_bench_press.gif", benchPressUrlEs)
        assertEquals("${baseUrl}videos/0001-barbell_bench_press.gif", benchPressUrlEn)
        assertFalse("Bench press should NOT resolve to sit-up 0001-2gPfomN.gif", benchPressUrlEs.contains("0001-2gPfomN.gif"))

        // 2. Abdominales Crunch / Sit Up
        val crunchUrlEs = ExerciseGifDatasetProvider.getGifUrlForExercise("Abdominales Crunch")
        val crunchUrlEn = ExerciseGifDatasetProvider.getGifUrlForExercise("Sit Up")
        assertEquals("${baseUrl}videos/0001-2gPfomN.gif", crunchUrlEs)
        assertEquals("${baseUrl}videos/0001-2gPfomN.gif", crunchUrlEn)

        // 3. Sentadilla / Barbell Squat
        val squatUrlEs = ExerciseGifDatasetProvider.getGifUrlForExercise("Sentadilla")
        val squatUrlEn = ExerciseGifDatasetProvider.getGifUrlForExercise("Barbell Squat")
        assertEquals("${baseUrl}videos/0040-barbell_squat.gif", squatUrlEs)
        assertEquals("${baseUrl}videos/0040-barbell_squat.gif", squatUrlEn)

        // 4. Dominadas / Pull-Up
        val pullUpUrlEs = ExerciseGifDatasetProvider.getGifUrlForExercise("Dominadas")
        val pullUpUrlEn = ExerciseGifDatasetProvider.getGifUrlForExercise("Pull Up")
        assertEquals("${baseUrl}videos/0024-pull_up.gif", pullUpUrlEs)
        assertEquals("${baseUrl}videos/0024-pull_up.gif", pullUpUrlEn)

        // 5. Jalón al Pecho / Lat Pulldown
        val latPulldownUrlEs = ExerciseGifDatasetProvider.getGifUrlForExercise("Jalón al Pecho")
        val latPulldownUrlEn = ExerciseGifDatasetProvider.getGifUrlForExercise("Lat Pulldown")
        assertEquals("${baseUrl}videos/0020-lat_pulldown.gif", latPulldownUrlEs)
        assertEquals("${baseUrl}videos/0020-lat_pulldown.gif", latPulldownUrlEn)

        // 6. Press Militar / Overhead Press / Shoulder Press
        val shoulderPressUrlEs = ExerciseGifDatasetProvider.getGifUrlForExercise("Press Militar")
        val shoulderPressUrlEn = ExerciseGifDatasetProvider.getGifUrlForExercise("Shoulder Press")
        assertEquals("${baseUrl}videos/0060-overhead_press.gif", shoulderPressUrlEs)
        assertEquals("${baseUrl}videos/0060-overhead_press.gif", shoulderPressUrlEn)

        // 7. Prensa de Piernas / Leg Press
        val legPressUrlEs = ExerciseGifDatasetProvider.getGifUrlForExercise("Prensa de Piernas")
        val legPressUrlEn = ExerciseGifDatasetProvider.getGifUrlForExercise("Leg Press")
        assertEquals("${baseUrl}videos/0042-leg_press.gif", legPressUrlEs)
        assertEquals("${baseUrl}videos/0042-leg_press.gif", legPressUrlEn)

        // 8. Fondos en Paralelas / Dips
        val dipsUrlEs = ExerciseGifDatasetProvider.getGifUrlForExercise("Fondos en Paralelas")
        val dipsUrlEn = ExerciseGifDatasetProvider.getGifUrlForExercise("Dips")
        assertEquals("${baseUrl}videos/0075-triceps_dips.gif", dipsUrlEs)
        assertEquals("${baseUrl}videos/0075-triceps_dips.gif", dipsUrlEn)

        // 9. Curl de Bíceps / Dumbbell Curl
        val bicepCurlUrlEs = ExerciseGifDatasetProvider.getGifUrlForExercise("Curl de Bíceps")
        val bicepCurlUrlEn = ExerciseGifDatasetProvider.getGifUrlForExercise("Dumbbell Curl")
        assertEquals("${baseUrl}videos/0070-dumbbell_curl.gif", bicepCurlUrlEs)
        assertEquals("${baseUrl}videos/0070-dumbbell_curl.gif", bicepCurlUrlEn)

        // 10. Running / Sprint / 10 Series x 400m
        val sprintUrl = ExerciseGifDatasetProvider.getGifUrlForExercise("10 Series x 400m")
        val runningUrl = ExerciseGifDatasetProvider.getGifUrlForExercise("Carrera")
        assertEquals("${baseUrl}videos/0111-interval_sprints.gif", sprintUrl)
        assertEquals("${baseUrl}videos/0110-running_endurance.gif", runningUrl)

        // 11. Natación / Swim
        val swimUrlEs = ExerciseGifDatasetProvider.getGifUrlForExercise("Natación")
        val swimUrlEn = ExerciseGifDatasetProvider.getGifUrlForExercise("Swim")
        assertEquals("${baseUrl}videos/0120-freestyle_crol.gif", swimUrlEs)
        assertEquals("${baseUrl}videos/0120-freestyle_crol.gif", swimUrlEn)
    }

    @Test
    fun testGetExerciseDetails_returnsCorrectDetailsWithoutCrossMatching() {
        val benchDetails = ExerciseGifDatasetProvider.getExerciseDetails("Press de Banca Horizontal")
        assertNotNull(benchDetails)
        assertEquals("barbell_bench_press", benchDetails?.id)

        val jalonDetails = ExerciseGifDatasetProvider.getExerciseDetails("Jalón al Pecho")
        assertNotNull(jalonDetails)
        assertEquals("lat_pulldown", jalonDetails?.id)

        val prensaDetails = ExerciseGifDatasetProvider.getExerciseDetails("Prensa de Piernas")
        assertNotNull(prensaDetails)
        assertEquals("leg_press", prensaDetails?.id)

        val crunchDetails = ExerciseGifDatasetProvider.getExerciseDetails("Abdominales Crunch")
        assertNotNull(crunchDetails)
        assertEquals("abdominal_crunch", crunchDetails?.id)
    }

    @Test
    fun testFindExercise_returnsMatchingEntry() {
        val entryEs = ExerciseGifDatasetProvider.findExercise("Press de Banca")
        assertNotNull(entryEs)
        assertEquals("barbell_bench_press", entryEs?.id)
        assertEquals("Pecho", entryEs?.targetMuscleEs)

        val entryEn = ExerciseGifDatasetProvider.findExercise("Barbell Bench Press")
        assertNotNull(entryEn)
        assertEquals("barbell_bench_press", entryEn?.id)

        val entryFlexiones = ExerciseGifDatasetProvider.findExercise("Flexiones de Pecho")
        assertNotNull(entryFlexiones)
        assertEquals("push_up", entryFlexiones?.id)
    }

    @Test
    fun testGetExercisesForTargetMuscle() {
        val chestExercises = ExerciseGifDatasetProvider.getExercisesForTargetMuscle("Pecho")
        assertFalse("Chest exercises should not be empty", chestExercises.isEmpty())

        val backExercises = ExerciseGifDatasetProvider.getExercisesForTargetMuscle("Espalda")
        assertFalse("Back exercises should not be empty", backExercises.isEmpty())

        val legExercises = ExerciseGifDatasetProvider.getExercisesForTargetMuscle("Piernas")
        assertFalse("Leg exercises should not be empty", legExercises.isEmpty())
    }

    @Test
    fun testGetExercisesByCategory() {
        val gymExercises = ExerciseGifDatasetProvider.getExercisesByCategory("Gym")
        assertFalse("Gym exercises should not be empty", gymExercises.isEmpty())

        val calisthenicsExercises = ExerciseGifDatasetProvider.getExercisesByCategory("Calisthenics")
        assertFalse("Calisthenics exercises should not be empty", calisthenicsExercises.isEmpty())

        val runningExercises = ExerciseGifDatasetProvider.getExercisesByCategory("Running")
        assertFalse("Running exercises should not be empty", runningExercises.isEmpty())

        val swimmingExercises = ExerciseGifDatasetProvider.getExercisesByCategory("Swimming")
        assertFalse("Swimming exercises should not be empty", swimmingExercises.isEmpty())
    }

    @Test
    fun testSearchExercises() {
        val resultsFlexiones = ExerciseGifDatasetProvider.searchExercises("flexion")
        assertFalse("Search for 'flexion' should return results", resultsFlexiones.isEmpty())

        val resultsCrol = ExerciseGifDatasetProvider.searchExercises("crol")
        assertFalse("Search for 'crol' should return results", resultsCrol.isEmpty())

        val resultsSprint = ExerciseGifDatasetProvider.searchExercises("sprint")
        assertFalse("Search for 'sprint' should return results", resultsSprint.isEmpty())
    }

    @Test
    fun testDirectGifUrlAndMediaImageUrlFormat() {
        val allExercises = ExerciseGifDatasetProvider.getAllExercises()
        allExercises.forEach { exercise ->
            assertTrue(
                "GIF URL '${exercise.gifUrl}' for ${exercise.id} should start with GitHub raw repository path",
                exercise.gifUrl.startsWith(ExerciseGifDatasetProvider.BASE_RAW_URL)
            )
            assertTrue(
                "GIF URL '${exercise.gifUrl}' for ${exercise.id} should end with .gif",
                exercise.gifUrl.endsWith(".gif")
            )
            assertTrue(
                "Media Image URL '${exercise.mediaImageUrl}' for ${exercise.id} should start with GitHub raw repository path",
                exercise.mediaImageUrl.startsWith(ExerciseGifDatasetProvider.BASE_RAW_URL)
            )
        }
    }
}
