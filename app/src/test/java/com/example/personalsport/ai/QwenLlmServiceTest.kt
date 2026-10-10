package com.example.personalsport.ai

import com.example.personalsport.data.local.entity.ProgressPhoto
import com.example.personalsport.diet.BodyProfile
import com.example.personalsport.diet.DietGoal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class QwenLlmServiceTest {

    @Before
    fun setUp() {
        QwenLlmService.releaseModel()
    }

    @Test
    fun testGenerateFallbackWhenModelNotLoaded() {
        var tokenReceived = ""
        val result = QwenLlmService.generate("Test prompt") { token ->
            tokenReceived = token
        }

        assertNotNull(result)
        assertTrue(result.contains("Qwen LLM model is not loaded") || result.contains("Please download"))
        assertEquals(result, tokenReceived)
    }

    @Test
    fun testIsModelLoadedInitialState() {
        val isLoaded = QwenLlmService.isModelLoaded()
        assertFalse(isLoaded)
    }

    @Test
    fun testSimulatedUnitTestExecution() {
        assertFalse(QwenLlmService.isModelLoaded())

        val initSuccess = QwenLlmService.initModel(modelFileName = "dummy.gguf")
        assertTrue(initSuccess)
        assertTrue(QwenLlmService.isModelLoaded())

        var tokenReceived = ""
        val result = QwenLlmService.generate("Test prompt simulated") { token ->
            tokenReceived = token
        }
        assertNotNull(result)
        assertTrue(result.contains("simulada") || result.contains("Qwen"))
        assertEquals(result, tokenReceived)

        QwenLlmService.releaseModel()
        assertFalse(QwenLlmService.isModelLoaded())
    }

    @Test
    fun testLocalAiEnginesFallbackWhenLlmNotLoaded() = runBlocking {
        // Test Sleep Analyzer fallback
        val sleepInput = SleepAnalysisInput(hoursSlept = 7.5f)
        var sleepToken = ""
        val sleepResult = LocalAISleepAnalyzer.analyzeSleepWithLlm(sleepInput) { token ->
            sleepToken = token
        }
        assertNotNull(sleepResult)
        assertTrue(sleepResult.sleepScore > 0)
        assertTrue(sleepToken.contains("Qwen LLM no cargado") || sleepToken.contains("reglas local"))

        // Test Routine Generator fallback
        val wizardState = AIRoutineWizardState()
        var routineToken = ""
        val routinePlan = LocalAIRoutineGenerator.generate3MonthPlanWithLlm(wizardState) { token ->
            routineToken = token
        }
        assertNotNull(routinePlan)
        assertTrue(routinePlan.months.isNotEmpty())
        assertTrue(routineToken.contains("Qwen LLM no cargado") || routineToken.contains("reglas local"))

        // Test Diet Engine fallback
        val profile = BodyProfile(weightKg = 75f, heightCm = 175f, age = 28)
        var dietToken = ""
        val dietPlan = LocalAIDietEngine.generate30DayPlanWithLlm(profile, DietGoal.FAT_LOSS) { token ->
            dietToken = token
        }
        assertNotNull(dietPlan)
        assertTrue(dietPlan.days.isNotEmpty())
        assertTrue(dietToken.contains("Qwen LLM no disponible") || dietToken.contains("reglas local"))

        // Test Visual Body Analyzer fallback
        val photo1 = ProgressPhoto(id = 1, photoUri = "", date = System.currentTimeMillis() - 864000000)
        val photo2 = ProgressPhoto(id = 2, photoUri = "", date = System.currentTimeMillis())
        var visualToken = ""
        val visualResult = VisualAIBodyAnalyzer.analyzeProgressPhotosWithLlm(photo1, photo2) { token ->
            visualToken = token
        }
        assertNotNull(visualResult)
        assertTrue(visualResult.confidenceScore > 0)
        assertTrue(visualToken.contains("Qwen LLM no disponible") || visualToken.contains("reglas local"))
    }
}
