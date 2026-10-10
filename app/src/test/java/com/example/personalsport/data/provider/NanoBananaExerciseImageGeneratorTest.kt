package com.example.personalsport.data.provider

import org.junit.Assert.*
import org.junit.Test

class NanoBananaExerciseImageGeneratorTest {

    @Test
    fun testGenerateImageForExercise_returnsValidFileUrl() {
        val exerciseName = "Prensa Hack Invertida Custom"
        val targetMuscle = "Cuádriceps"

        val imageUrl = NanoBananaExerciseImageGenerator.generateImageForExercise(null, exerciseName, targetMuscle)

        assertNotNull("Generated image URL should not be null", imageUrl)
        assertTrue("Generated image URL should start with file://, found: $imageUrl", imageUrl.startsWith("file://"))
        assertTrue("Generated image URL should contain realistic_exercise_, found: $imageUrl", imageUrl.contains("realistic_exercise_"))
    }

    @Test
    fun testIsNanoBananaImage() {
        val nanoBananaUrl = "file:///data/user/0/com.example.personalsport/cache/nano_banana_prensa_hack.png"
        val realisticUrl = "file:///data/user/0/com.example.personalsport/cache/realistic_exercise_a1b2c3.png"
        val datasetUrl = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/videos/0001-barbell_bench_press.gif"

        assertTrue("Should return true for Nano Banana image URL", NanoBananaExerciseImageGenerator.isNanoBananaImage(nanoBananaUrl))
        assertTrue("Should return true for Realistic image URL", NanoBananaExerciseImageGenerator.isNanoBananaImage(realisticUrl))
        assertFalse("Should return false for standard dataset GIF URL", NanoBananaExerciseImageGenerator.isNanoBananaImage(datasetUrl))
        assertFalse("Should return false for null URL", NanoBananaExerciseImageGenerator.isNanoBananaImage(null))
        assertFalse("Should return false for empty URL", NanoBananaExerciseImageGenerator.isNanoBananaImage(""))
    }

    @Test
    fun testGenerateSvgForExercise_containsBadgeAndMuscleName() {
        val exerciseName = "Prensa Hack Invertida Custom"
        val targetMuscle = "Cuádriceps"

        val svgContent = NanoBananaExerciseImageGenerator.generateSvgForExercise(exerciseName, targetMuscle)

        assertNotNull(svgContent)
        assertTrue("SVG content should contain badge text", svgContent.contains(NanoBananaExerciseImageGenerator.BADGE_TEXT))
        assertTrue("SVG content should contain uppercase exercise title", svgContent.contains("PRENSA HACK INVERTIDA CUSTOM"))
        assertTrue("SVG content should contain target muscle name", svgContent.contains("Cuádriceps"))
        assertTrue("SVG content should contain SVG root tag", svgContent.contains("<svg"))
    }

    @Test
    fun testIntegrationWithExerciseJsonDatasetParser_unknownExercise() {
        val unknownQuery = "Ejercicio Unico Misterioso Custom 123"
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
    fun testIntegrationWithExerciseGifDatasetProvider_unknownExercise() {
        val unknownQuery = "Movimiento Inexistente Raro XYZ 99"
        val targetMuscle = "Cardio"

        val generatedUrl = ExerciseGifDatasetProvider.getGifUrlForExercise(unknownQuery, targetMuscleHint = targetMuscle)
        assertNotNull(generatedUrl)
        assertTrue("ExerciseGifDatasetProvider should return realistic_exercise_ URL for unknown exercise, found: $generatedUrl", generatedUrl.contains("realistic_exercise_") || generatedUrl.contains("scraped_exercises"))
        assertTrue("ExerciseGifDatasetProvider URL should start with file://, found: $generatedUrl", generatedUrl.startsWith("file://"))
    }
}
