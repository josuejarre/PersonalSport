package com.example.personalsport.data.provider

import org.junit.Assert.*
import org.junit.Test

class ProAnatomicalDiagramGeneratorTest {

    @Test
    fun testGenerateProAnatomicalDiagram_returnsValidFileUrl() {
        val exerciseName = "Saltos con Cuerda de Alta Velocidad"
        val targetMuscle = "Pantorrillas & Cuádriceps"

        val diagramUrl = ProAnatomicalDiagramGenerator.generateProAnatomicalDiagram(null, exerciseName, targetMuscle)

        assertNotNull("Generated diagram URL should not be null", diagramUrl)
        assertTrue("Generated diagram URL should start with file://, found: $diagramUrl", diagramUrl.startsWith("file://"))
        assertTrue("Generated diagram URL should contain realistic_exercise_, found: $diagramUrl", diagramUrl.contains("realistic_exercise_"))
    }

    @Test
    fun testIsProAnatomicalDiagram() {
        val proUrl = "file:///data/user/0/com.example.personalsport/cache/pro_anatomical_abc123.png"
        val realisticUrl = "file:///data/user/0/com.example.personalsport/cache/realistic_exercise_abc123.png"
        val normalUrl = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/videos/0001-barbell_bench_press.gif"

        assertTrue("Should return true for Pro Anatomical diagram URL", ProAnatomicalDiagramGenerator.isProAnatomicalDiagram(proUrl))
        assertTrue("Should return true for Realistic diagram URL", ProAnatomicalDiagramGenerator.isProAnatomicalDiagram(realisticUrl))
        assertFalse("Should return false for standard dataset GIF URL", ProAnatomicalDiagramGenerator.isProAnatomicalDiagram(normalUrl))
        assertFalse("Should return false for null URL", ProAnatomicalDiagramGenerator.isProAnatomicalDiagram(null))
        assertFalse("Should return false for empty URL", ProAnatomicalDiagramGenerator.isProAnatomicalDiagram(""))
    }

    @Test
    fun testIntegrationWithExerciseJsonDatasetParser_unknownExercise() {
        val unknownQuery = "Salto Doble con Cuerda Pesada Custom"
        val targetMuscle = "Pantorrillas"

        val generatedUrl = ExerciseJsonDatasetParser.getFullGifUrl(unknownQuery, targetMuscleHint = targetMuscle)
        assertNotNull(generatedUrl)
        assertTrue("Unknown exercise should resolve to realistic_exercise_ diagram URL, found: $generatedUrl", generatedUrl.contains("realistic_exercise_"))
        assertTrue("URL should start with file://, found: $generatedUrl", generatedUrl.startsWith("file://"))

        val exerciseItem = ExerciseJsonDatasetParser.findExercise(unknownQuery, targetMuscleHint = targetMuscle)
        assertNotNull(exerciseItem)
        assertTrue("Parsed exercise item gif_url should contain realistic_exercise_, found: ${exerciseItem?.gif_url}", exerciseItem?.gif_url?.contains("realistic_exercise_") == true)
    }

    @Test
    fun testIntegrationWithExerciseWebScraperEngine_fallbackToProAnatomicalDiagram() {
        val customQuery = "Movimiento Complejo Especial 999"
        val targetMuscle = "Hombros"

        val resultUrl = ExerciseWebScraperEngine.findAndStyleExerciseMedia(null, customQuery, targetMuscle)
        assertNotNull(resultUrl)
        assertTrue("Web Scraper fallback should return realistic_exercise_ diagram URL, found: $resultUrl", resultUrl.contains("realistic_exercise_"))
        assertTrue("Result URL should start with file://, found: $resultUrl", resultUrl.startsWith("file://"))
    }
}
