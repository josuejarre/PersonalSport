package com.example.personalsport.ai

import com.example.personalsport.data.local.entity.BodyMetric
import com.example.personalsport.data.local.entity.ProgressPhoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualAIBodyAnalyzerTest {

    @Test
    fun testAnthropometricRatiosCalculation() {
        val photoBefore = ProgressPhoto(id = 1, date = 1000L, photoUri = "file:///p1", notes = "Baseline")
        val ratios = VisualAIBodyAnalyzer.extractPhotoRatios(null, photoBefore)

        assertNotNull(ratios)
        assertTrue(ratios.vTaperIndex > 0f)
        assertTrue(ratios.chestToWaistRatio > 0f)
        assertTrue(ratios.silhouetteDefinitionIndex in 0f..100f)
        assertEquals(ratios.shoulderWidthPx / ratios.waistWidthPx, ratios.vTaperIndex, 0.01f)
        assertEquals(ratios.chestWidthPx / ratios.waistWidthPx, ratios.chestToWaistRatio, 0.01f)
    }

    @Test
    fun testMultiModalDataFusion_FatLoss() {
        val now = System.currentTimeMillis()
        val beforePhoto = ProgressPhoto(id = 1, date = now - 30 * 86400000L, photoUri = "file:///b1", notes = "Start photo")
        val afterPhoto = ProgressPhoto(id = 2, date = now, photoUri = "file:///a1", notes = "Cut phase definition abs")

        val beforeMetric = BodyMetric(id = 1, date = beforePhoto.date, weightKg = 82.0f, bodyFatPercentage = 22.0f)
        val afterMetric = BodyMetric(id = 2, date = afterPhoto.date, weightKg = 79.5f, bodyFatPercentage = 19.5f)

        val result = VisualAIBodyAnalyzer.analyzeProgressPhotos(
            beforePhoto = beforePhoto,
            afterPhoto = afterPhoto,
            beforeMetric = beforeMetric,
            afterMetric = afterMetric
        )

        assertNotNull(result)
        assertEquals(TransformationOutcome.FAT_LOSS, result.outcome)
        assertTrue(result.confidenceScore in 50..100)
        assertTrue(result.explanationBreakdown.contains("Pérdida") || result.explanationBreakdown.contains("Grasa") || result.explanationBreakdown.contains("Definición") || result.explanationBreakdown.contains("V-Taper"))
        assertNotNull(result.multiModalData.weightChangeKg)
        assertEquals(-2.5f, result.multiModalData.weightChangeKg!!, 0.1f)
        assertEquals(-2.5f, result.multiModalData.fatPercentageChangePct!!, 0.1f)
        assertTrue(result.trainingAdvice.isNotEmpty())
        assertTrue(result.nutritionAdvice.isNotEmpty())
        assertTrue(result.isLocalProcessingConfirmed)
    }

    @Test
    fun testMultiModalDataFusion_MuscleGain() {
        val now = System.currentTimeMillis()
        val beforePhoto = ProgressPhoto(id = 1, date = now - 60 * 86400000L, photoUri = "file:///b2", notes = "Start bulk")
        val afterPhoto = ProgressPhoto(id = 2, date = now, photoUri = "file:///a2", notes = "Gain bulk fuerza masa")

        val beforeMetric = BodyMetric(id = 1, date = beforePhoto.date, weightKg = 72.0f, bodyFatPercentage = 14.0f)
        val afterMetric = BodyMetric(id = 2, date = afterPhoto.date, weightKg = 75.2f, bodyFatPercentage = 14.2f)

        val result = VisualAIBodyAnalyzer.analyzeProgressPhotos(
            beforePhoto = beforePhoto,
            afterPhoto = afterPhoto,
            beforeMetric = beforeMetric,
            afterMetric = afterMetric
        )

        assertNotNull(result)
        assertEquals(TransformationOutcome.MUSCLE_GAIN, result.outcome)
        assertTrue(result.confidenceScore >= 80)
        assertEquals(3.2f, result.multiModalData.weightChangeKg!!, 0.1f)
        assertTrue(result.trainingAdvice.contains("sobrecarga progresiva") || result.trainingAdvice.contains("multiarticulares"))
        assertTrue(result.nutritionAdvice.contains("superávit calórico"))
    }

    @Test
    fun testMultiModalDataFusion_BodyRecomposition() {
        val now = System.currentTimeMillis()
        val beforePhoto = ProgressPhoto(id = 1, date = now - 45 * 86400000L, photoUri = "file:///b3", notes = "Recomp baseline")
        val afterPhoto = ProgressPhoto(id = 2, date = now, photoUri = "file:///a3", notes = "Cut gain bulk defin abs")

        val beforeMetric = BodyMetric(id = 1, date = beforePhoto.date, weightKg = 76.0f, bodyFatPercentage = 18.0f)
        val afterMetric = BodyMetric(id = 2, date = afterPhoto.date, weightKg = 75.8f, bodyFatPercentage = 16.2f) // Weight stable (-0.2kg), fat down (-1.8%)

        val result = VisualAIBodyAnalyzer.analyzeProgressPhotos(
            beforePhoto = beforePhoto,
            afterPhoto = afterPhoto,
            beforeMetric = beforeMetric,
            afterMetric = afterMetric
        )

        assertNotNull(result)
        assertEquals(TransformationOutcome.BODY_RECOMPOSITION, result.outcome)
        assertTrue(result.confidenceScore in 75..98)
        assertTrue(result.trainingAdvice.isNotEmpty())
        assertTrue(result.nutritionAdvice.contains("normocalórica") || result.nutritionAdvice.contains("proteína"))
    }

    @Test
    fun testMultiModalDataFusion_Maintenance() {
        val now = System.currentTimeMillis()
        val beforePhoto = ProgressPhoto(id = 1, date = now - 10 * 86400000L, photoUri = "file:///b4", notes = "Standard")
        val afterPhoto = ProgressPhoto(id = 2, date = now, photoUri = "file:///a4", notes = "Standard")

        val beforeMetric = BodyMetric(id = 1, date = beforePhoto.date, weightKg = 70.0f, bodyFatPercentage = 15.0f)
        val afterMetric = BodyMetric(id = 2, date = afterPhoto.date, weightKg = 70.1f, bodyFatPercentage = 15.0f)

        val result = VisualAIBodyAnalyzer.analyzeProgressPhotos(
            beforePhoto = beforePhoto,
            afterPhoto = afterPhoto,
            beforeMetric = beforeMetric,
            afterMetric = afterMetric
        )

        assertNotNull(result)
        assertEquals(TransformationOutcome.MAINTENANCE, result.outcome)
        assertTrue(result.explanationBreakdown.contains("Confianza de IA Local"))
    }

    @Test
    fun testConfidenceScoreAndExplanationFormatting() {
        val now = System.currentTimeMillis()
        val photo1 = ProgressPhoto(id = 1, date = now - 35 * 86400000L, photoUri = "file:///1", notes = "Initial")
        val photo2 = ProgressPhoto(id = 2, date = now, photoUri = "file:///2", notes = "Cut phase")

        val metric1 = BodyMetric(id = 1, date = photo1.date, weightKg = 80.0f, bodyFatPercentage = 20.0f)
        val metric2 = BodyMetric(id = 2, date = photo2.date, weightKg = 77.0f, bodyFatPercentage = 17.5f)

        val resultWithMetrics = VisualAIBodyAnalyzer.analyzeProgressPhotos(photo1, photo2, metric1, metric2)
        val resultWithoutMetrics = VisualAIBodyAnalyzer.analyzeProgressPhotos(photo1, photo2)

        assertTrue(resultWithMetrics.confidenceScore > resultWithoutMetrics.confidenceScore)
        assertTrue(resultWithMetrics.explanationBreakdown.contains("V-Taper"))
        assertTrue(resultWithMetrics.explanationBreakdown.contains("80.0 kg ➔ 77.0 kg"))
    }

    @Test
    fun testDirectRatiosFusionAnalysis() {
        val beforeRatios = AnthropometricRatios(120f, 100f, 85f, 1.41f, 1.17f, 50f)
        val afterRatios = AnthropometricRatios(122f, 102f, 80f, 1.525f, 1.275f, 65f)

        val photo1 = ProgressPhoto(id = 1, date = 1000L, photoUri = "file:///1")
        val photo2 = ProgressPhoto(id = 2, date = 1000000000L, photoUri = "file:///2")

        val result = VisualAIBodyAnalyzer.computeFusionAnalysis(
            beforeRatios = beforeRatios,
            afterRatios = afterRatios,
            beforePhoto = photo1,
            afterPhoto = photo2
        )

        assertNotNull(result)
        assertTrue(result.ratioChanges.vTaperChangePct > 5.0f)
        assertTrue(result.ratioChanges.definitionChangePct > 10.0f)
        assertTrue(result.outcome == TransformationOutcome.MUSCLE_GAIN || result.outcome == TransformationOutcome.BODY_RECOMPOSITION || result.outcome == TransformationOutcome.FAT_LOSS)
    }
}
