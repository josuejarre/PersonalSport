package com.example.personalsport.data.provider

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RealisticExerciseIllustrationGeneratorTest {

    @Before
    fun setUp() {
        ExerciseJsonDatasetParser.clearCache()
    }

    @Test
    fun testGenerateRealisticIllustration_returnsValidFileUrl() {
        val exerciseName = "Dominadas Libres Estrictas"
        val targetMuscle = "Dorsal Ancho"

        val imageUrl = RealisticExerciseIllustrationGenerator.generateRealisticIllustration(null, exerciseName, targetMuscle)

        assertNotNull("Generated illustration URL should not be null", imageUrl)
        assertTrue("Generated illustration URL should start with file://, found: $imageUrl", imageUrl.startsWith("file://"))
        assertTrue("Generated illustration URL should contain realistic_exercise_, found: $imageUrl", imageUrl.contains("realistic_exercise_"))
        assertTrue("Generated illustration URL should end with .png, found: $imageUrl", imageUrl.endsWith(".png"))
    }

    @Test
    fun testGenerateRealisticIllustration_forAllPostures() {
        val exercisesAndMuscles = listOf(
            "Dominadas en Barra" to "Espalda",
            "Salto de Cuerda Comba" to "Pantorrillas",
            "Sentadilla Profunda" to "Cuádriceps",
            "Press de Banca Plano" to "Pecho",
            "Abdominales Crunch Core" to "Abdominales",
            "Carrera Sprint 100m" to "Piernas",
            "Natación Estilo Crol" to "Dorsal",
            "Peso Muerto Rumano" to "Femoral",
            "Press Militar Hombros" to "Hombros",
            "Curl de Bíceps" to "Brazos"
        )

        for ((exercise, muscle) in exercisesAndMuscles) {
            val url = RealisticExerciseIllustrationGenerator.generateRealisticIllustration(null, exercise, muscle)
            assertNotNull("Generated URL for $exercise should not be null", url)
            assertTrue("Generated URL for $exercise should start with file://", url.startsWith("file://"))
            assertTrue("Generated URL for $exercise should contain cache prefix", url.contains("realistic_exercise_"))
        }
    }

    @Test
    fun testIsRealisticIllustration() {
        val realisticUrl = "file:///data/user/0/com.example.personalsport/cache/realistic_exercise_a1b2c3d4.png"
        val datasetUrl = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/videos/0001-barbell_bench_press.gif"

        assertTrue("Should return true for realistic exercise image URL", RealisticExerciseIllustrationGenerator.isRealisticIllustration(realisticUrl))
        assertFalse("Should return false for standard dataset GIF URL", RealisticExerciseIllustrationGenerator.isRealisticIllustration(datasetUrl))
        assertFalse("Should return false for null URL", RealisticExerciseIllustrationGenerator.isRealisticIllustration(null))
        assertFalse("Should return false for empty URL", RealisticExerciseIllustrationGenerator.isRealisticIllustration(""))
    }

    @Test
    fun testConstantsAndColors() {
        assertEquals("🏋️ Ilustración Biomecánica Realista 3D", RealisticExerciseIllustrationGenerator.BADGE_TEXT)
        assertEquals(0xFFFF3D00.toInt(), RealisticExerciseIllustrationGenerator.COLOR_PRIMARY_HIGHLIGHT_RED)
        assertEquals(0xFFFF6500.toInt(), RealisticExerciseIllustrationGenerator.COLOR_PRIMARY_HIGHLIGHT_ORANGE)
        assertEquals(0xFFFFFFFF.toInt(), RealisticExerciseIllustrationGenerator.COLOR_BACKGROUND_WHITE)
    }

    @Test
    fun testDeterminePose() {
        assertEquals(RealisticExerciseIllustrationGenerator.PoseType.PULL_UP, RealisticExerciseIllustrationGenerator.determinePose("Dominadas en Barra", "Dorsal"))
        assertEquals(RealisticExerciseIllustrationGenerator.PoseType.PULL_UP, RealisticExerciseIllustrationGenerator.determinePose("Jalón al Pecho", "Espalda"))
        assertEquals(RealisticExerciseIllustrationGenerator.PoseType.SQUAT, RealisticExerciseIllustrationGenerator.determinePose("Sentadilla Profunda con Barra", "Cuádriceps"))
        assertEquals(RealisticExerciseIllustrationGenerator.PoseType.SQUAT, RealisticExerciseIllustrationGenerator.determinePose("Prensa de Piernas 45°", "Piernas"))
        assertEquals(RealisticExerciseIllustrationGenerator.PoseType.BENCH_PRESS, RealisticExerciseIllustrationGenerator.determinePose("Press de Banca Horizontal", "Pecho"))
        assertEquals(RealisticExerciseIllustrationGenerator.PoseType.BENCH_PRESS, RealisticExerciseIllustrationGenerator.determinePose("Flexiones de Pecho", "Pectoral"))
        assertEquals(RealisticExerciseIllustrationGenerator.PoseType.JUMP_ROPE, RealisticExerciseIllustrationGenerator.determinePose("Saltos con Comba", "Cardio"))
        assertEquals(RealisticExerciseIllustrationGenerator.PoseType.JUMP_ROPE, RealisticExerciseIllustrationGenerator.determinePose("Jump Rope Double Unders", "Pantorrillas"))
        assertEquals(RealisticExerciseIllustrationGenerator.PoseType.RUNNING, RealisticExerciseIllustrationGenerator.determinePose("Carrera Sprint 400m", "Piernas"))
        assertEquals(RealisticExerciseIllustrationGenerator.PoseType.RUNNING, RealisticExerciseIllustrationGenerator.determinePose("Trote Ligero", "Cardio"))
        assertEquals(RealisticExerciseIllustrationGenerator.PoseType.SWIMMING, RealisticExerciseIllustrationGenerator.determinePose("Natación Estilo Crol", "Dorsal"))
        assertEquals(RealisticExerciseIllustrationGenerator.PoseType.DEADLIFT, RealisticExerciseIllustrationGenerator.determinePose("Peso Muerto Rumano", "Femoral"))
        assertEquals(RealisticExerciseIllustrationGenerator.PoseType.ABS_CRUNCH, RealisticExerciseIllustrationGenerator.determinePose("Abdominales Crunch", "Core"))
    }

    @Test
    fun testDetermineMuscleGroup() {
        assertEquals(RealisticExerciseIllustrationGenerator.MuscleGroup.CHEST, RealisticExerciseIllustrationGenerator.determineMuscleGroup("Press Inclinado", "Pecho"))
        assertEquals(RealisticExerciseIllustrationGenerator.MuscleGroup.BACK, RealisticExerciseIllustrationGenerator.determineMuscleGroup("Remo con Barra", "Espalda"))
        assertEquals(RealisticExerciseIllustrationGenerator.MuscleGroup.LEGS, RealisticExerciseIllustrationGenerator.determineMuscleGroup("Extensión de Cuádriceps", "Piernas"))
        assertEquals(RealisticExerciseIllustrationGenerator.MuscleGroup.CALVES, RealisticExerciseIllustrationGenerator.determineMuscleGroup("Elevación de Talones", "Pantorrillas"))
        assertEquals(RealisticExerciseIllustrationGenerator.MuscleGroup.SHOULDERS, RealisticExerciseIllustrationGenerator.determineMuscleGroup("Press Militar", "Hombros"))
        assertEquals(RealisticExerciseIllustrationGenerator.MuscleGroup.ARMS, RealisticExerciseIllustrationGenerator.determineMuscleGroup("Curl de Bíceps", "Brazos"))
        assertEquals(RealisticExerciseIllustrationGenerator.MuscleGroup.ABS, RealisticExerciseIllustrationGenerator.determineMuscleGroup("Abdominales Crunch", "Core"))
        assertEquals(RealisticExerciseIllustrationGenerator.MuscleGroup.GLUTES, RealisticExerciseIllustrationGenerator.determineMuscleGroup("Hip Thrust", "Glúteos"))
        assertEquals(RealisticExerciseIllustrationGenerator.MuscleGroup.CARDIO, RealisticExerciseIllustrationGenerator.determineMuscleGroup("Trote Suave", "Cardio"))
    }

    @Test
    fun testGetHashConsistency() {
        val hash1 = RealisticExerciseIllustrationGenerator.getHash("Dominadas", "Espalda")
        val hash2 = RealisticExerciseIllustrationGenerator.getHash("Dominadas", "Espalda")
        val hash3 = RealisticExerciseIllustrationGenerator.getHash("Sentadilla", "Piernas")

        assertEquals("Hashes for identical inputs must match", hash1, hash2)
        assertNotEquals("Hashes for different inputs should differ", hash1, hash3)
    }

    @Test
    fun testIntegrationWithDatasetParser_unknownExerciseResolvesToRealisticIllustration() {
        val unknownQuery = "Ejercicio Biomecanico Misterioso 999"
        val targetMuscle = "Pecho"

        val generatedUrl = ExerciseJsonDatasetParser.getFullGifUrl(unknownQuery, targetMuscleHint = targetMuscle)
        assertNotNull(generatedUrl)
        assertTrue("Unknown exercise should resolve to realistic_exercise_ file URL, found: $generatedUrl", generatedUrl.contains("realistic_exercise_"))
        assertTrue("Unknown exercise URL should start with file://, found: $generatedUrl", generatedUrl.startsWith("file://"))

        val exerciseItem = ExerciseJsonDatasetParser.findExercise(unknownQuery, targetMuscleHint = targetMuscle)
        assertNotNull(exerciseItem)
        assertTrue("Parsed exercise item gif_url should contain realistic_exercise_, found: ${exerciseItem?.gif_url}", exerciseItem?.gif_url?.contains("realistic_exercise_") == true)
    }

    @Test
    fun testIntegrationWithWebScraperEngine_fallbackResolvesToRealisticIllustration() {
        val unknownQuery = "Movimiento Desconocido Unico XYZ"
        val targetMuscle = "Espalda"

        val resultUrl = ExerciseWebScraperEngine.findAndStyleExerciseMedia(
            context = null,
            exerciseName = unknownQuery,
            targetMuscle = targetMuscle
        )

        assertNotNull("Scraped media URL should not be null", resultUrl)
        assertTrue("Fallback URL should resolve to realistic_exercise_, found: $resultUrl", resultUrl.contains("realistic_exercise_"))
        assertTrue("URL should start with file://, found: $resultUrl", resultUrl.startsWith("file://"))
    }
}
