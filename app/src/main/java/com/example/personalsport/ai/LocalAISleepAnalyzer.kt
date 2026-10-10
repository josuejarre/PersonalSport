package com.example.personalsport.ai

import java.util.Locale

/**
 * Insignias cualitativas de calidad de descanso ([SleepQualityBadge]).
 */
enum class SleepQualityBadge(val displayName: String) {
    EXCELENTE("Excelente"),
    BUENA("Buena"),
    REGULAR("Regular"),
    NECESITA_RECUPERACION("Necesita Recuperación")
}

/**
 * Datos de entrada para el análisis de descanso y recuperación ([SleepAnalysisInput]).
 *
 * @property hoursSlept Horas totales dormidas.
 * @property deepSleepPercentage Porcentaje de sueño profundo (NREM).
 * @property remSleepPercentage Porcentaje de sueño REM.
 * @property restingHeartRateBpm Frecuencia cardíaca en reposo (RHR) al despertar.
 * @property dinnerTimingHoursBeforeBed Horas de diferencia entre la cena y la hora de acostarse.
 * @property hasLateCaffeine `true` si hubo consumo de cafeína pasadas las 16:00.
 * @property hasMagnesiumZincIntake `true` si se consumió suplementación con Magnesio + Zinc.
 * @property hydrationLiters Litros de agua consumidos durante el día.
 * @property workoutIntensity Intensidad del entrenamiento realizado ("ALTA", "MODERADA", "BAJA").
 * @property fatiguedMuscleZones Lista de zonas musculares fatigadas.
 * @property athleteAge Edad del atleta.
 * @property unloggedBackgroundSteps Pasos no registrados capturados en segundo plano por el wearable.
 * @property unloggedActiveMovements Descripción del movimiento activo secundario.
 */
data class SleepAnalysisInput(
    val hoursSlept: Float = 7.75f,
    val deepSleepPercentage: Float = 22.0f,
    val remSleepPercentage: Float = 20.0f,
    val restingHeartRateBpm: Int = 54,
    val dinnerTimingHoursBeforeBed: Float = 3.0f,
    val hasLateCaffeine: Boolean = false,
    val hasMagnesiumZincIntake: Boolean = true,
    val hydrationLiters: Float = 2.5f,
    val workoutIntensity: String = "ALTA",
    val fatiguedMuscleZones: List<String> = listOf("Cuádriceps", "Isquiotibiales"),
    val athleteAge: Int = 28,
    val unloggedBackgroundSteps: Int = 10400,
    val unloggedActiveMovements: String = "Paseos urbanos, caminata rápida y movilidad espontánea"
)

/**
 * Dictamen y recomendaciones del análisis de sueño y recuperación ([SleepAnalysisResult]).
 *
 * @property sleepScore Puntuación de descanso (0 a 100).
 * @property qualityBadge Insignia cualitativa ([SleepQualityBadge]).
 * @property hoursSlept Horas dormidas evaluadas.
 * @property restingHeartRateBpm Frecuencia cardíaca en reposo.
 * @property deepSleepPercentage Porcentaje de sueño profundo.
 * @property remSleepPercentage Porcentaje de sueño REM.
 * @property summary Resumen cuantitativo traducido al español.
 * @property recommendations Lista de consejos prácticos para mejorar el descanso.
 * @property neatImpactSummary Impacto de los pasos libres NEAT en la recuperación.
 * @property dietSupplementFeedback Evaluación de la cena, suplementación e hidratación.
 */
data class SleepAnalysisResult(
    val sleepScore: Int,
    val qualityBadge: SleepQualityBadge,
    val hoursSlept: Float,
    val restingHeartRateBpm: Int,
    val deepSleepPercentage: Float,
    val remSleepPercentage: Float,
    val summary: String,
    val recommendations: List<String>,
    val neatImpactSummary: String,
    val dietSupplementFeedback: String
)

/**
 * Analizador Local de Sueño, Descanso y Recuperación Neuro-Muscular ([LocalAISleepAnalyzer]).
 *
 * Evalúa la calidad del descanso considerando horas de sueño, arquitectura (profundo/REM),
 * frecuencia cardíaca en reposo, horario de cena, suplementos y actividad física en segundo plano (NEAT).
 *
 * **Gestión de Estado e Inferencia**:
 * - Algoritmo determinista local de reglas combinable con la inferencia de Qwen 2.5 3B LLM.
 * - Inmutable y seguro para ejecución en cualquier hilo de fondo.
 */
object LocalAISleepAnalyzer {

    /**
     * Analiza las métricas de descanso y devuelve el dictamen cuantitativo y consejos.
     */
    fun analyzeSleep(input: SleepAnalysisInput): SleepAnalysisResult {
        val baseResult = computeBaseSleepAnalysis(input)
        if (QwenLlmService.isModelLoaded()) {
            return enrichSleepAnalysisWithLlm(baseResult, input)
        }
        return baseResult
    }

    /**
     * Analiza el sueño integrando las recomendaciones generadas por Qwen 2.5 3B LLM.
     */
    fun analyzeSleepWithLlm(
        input: SleepAnalysisInput,
        onTokenStream: (String) -> Unit = {}
    ): SleepAnalysisResult {
        val baseResult = computeBaseSleepAnalysis(input)

        if (!QwenLlmService.isModelLoaded()) {
            onTokenStream("Qwen LLM no cargado. Usando análisis de sueño por reglas local.")
            return baseResult
        }

        return enrichSleepAnalysisWithLlm(baseResult, input, onTokenStream)
    }

    private fun enrichSleepAnalysisWithLlm(
        baseResult: SleepAnalysisResult,
        input: SleepAnalysisInput,
        onTokenStream: (String) -> Unit = {}
    ): SleepAnalysisResult {
        val ragContext = LocalSportsKnowledgeBase.getKnowledgeContext("nutricion carrera atletismo")
        val prompt = buildString {
            append(ragContext).append("\n\n")
            append("Actúa como especialista en fisiología del descanso deportivo en Qwen 2.5 3B Local LLM.\n")
            append("Horas dormidas: ").append(input.hoursSlept).append("h, Sueño Profundo: ").append(input.deepSleepPercentage).append("%, REM: ").append(input.remSleepPercentage).append("%, RHR: ").append(input.restingHeartRateBpm).append(" bpm.\n")
            append("Pasos NEAT segundo plano: ").append(input.unloggedBackgroundSteps).append(". Cena previa: ").append(input.dinnerTimingHoursBeforeBed).append("h.\n")
            append("Genera dictamen de recuperación y sugerencias optimizadas de sueño estrictamente en ESPAÑOL.")
        }

        val llmOutput = QwenLlmService.generate(prompt, maxTokens = 512, onToken = onTokenStream)

        if (llmOutput.isBlank() || llmOutput.startsWith("Qwen LLM model is not loaded") || llmOutput.startsWith("Error")) {
            return baseResult
        }

        val enrichedSummary = "${baseResult.summary}\n\n[Qwen 2.5 LLM Insight]:\n${llmOutput.take(300)}"
        return baseResult.copy(summary = enrichedSummary)
    }

    private fun computeBaseSleepAnalysis(input: SleepAnalysisInput): SleepAnalysisResult {
        var score = 0

        // 1. Puntuación por duración de sueño (máx 35 pts)
        val targetSleepHours = if (input.workoutIntensity.contains("ALTA", ignoreCase = true)) 7.5f else 7.0f
        val durationPts = when {
            input.hoursSlept >= 8.0f -> 35
            input.hoursSlept >= 7.0f -> 30
            input.hoursSlept >= 6.0f -> 20
            input.hoursSlept >= 5.0f -> 10
            else -> 5
        }
        score += durationPts

        // 2. Arquitectura de sueño profundo y REM (máx 25 pts)
        val deepPts = when {
            input.deepSleepPercentage >= 20.0f -> 15
            input.deepSleepPercentage >= 15.0f -> 10
            else -> 5
        }
        val remPts = when {
            input.remSleepPercentage >= 20.0f -> 10
            input.remSleepPercentage >= 15.0f -> 7
            else -> 3
        }
        score += deepPts + remPts

        // 3. Frecuencia cardíaca en reposo RHR (máx 20 pts)
        val rhrPts = when {
            input.restingHeartRateBpm < 55 -> 20
            input.restingHeartRateBpm in 55..64 -> 16
            input.restingHeartRateBpm in 65..74 -> 10
            else -> 4
        }
        score += rhrPts

        // 4. Modificador de cena, suplementos e hidratación (+/- 10 pts)
        var dietSuppPts = 0
        if (input.dinnerTimingHoursBeforeBed >= 2.5f) dietSuppPts += 3 else dietSuppPts -= 3
        if (!input.hasLateCaffeine) dietSuppPts += 3 else dietSuppPts -= 4
        if (input.hasMagnesiumZincIntake) dietSuppPts += 2
        if (input.hydrationLiters >= 2.2f) dietSuppPts += 2 else dietSuppPts -= 2
        score += dietSuppPts

        // 5. Ajuste por carga NEAT / Pasos libres (+/- 10 pts)
        if (input.unloggedBackgroundSteps > 10000 && input.hoursSlept < 6.5f) {
            score -= 5
        } else if (input.hoursSlept >= targetSleepHours) {
            score += 5
        }

        val finalScore = score.coerceIn(0, 100)

        val badge = when {
            finalScore >= 85 -> SleepQualityBadge.EXCELENTE
            finalScore >= 70 -> SleepQualityBadge.BUENA
            finalScore >= 50 -> SleepQualityBadge.REGULAR
            else -> SleepQualityBadge.NECESITA_RECUPERACION
        }

        val hoursInt = input.hoursSlept.toInt()
        val minutesInt = ((input.hoursSlept - hoursInt) * 60).toInt()
        val durationFormatted = "${hoursInt}h ${minutesInt}m"

        val summary = "Calidad de descanso: ${badge.displayName} ($finalScore/100). Duración: $durationFormatted | Reposo: ${input.restingHeartRateBpm} bpm."

        val recommendations = mutableListOf<String>()

        if (input.hoursSlept < 7.0f) {
            recommendations.add("Incrementa el descanso nocturno a un mínimo de 7.5h. La carga física actual (${input.workoutIntensity}) requiere mayor tiempo en fase REM y NREM.")
        } else {
            recommendations.add("Mantienes una ventana de descanso adecuada ($durationFormatted), favoreciendo la secreción de hormona del crecimiento y reparación proteica.")
        }

        if (input.deepSleepPercentage < 18.0f) {
            recommendations.add("Optimiza el sueño profundo (${String.format(Locale.getDefault(), "%.1f", input.deepSleepPercentage)}% actual) reduciendo pantallas brillantes 45m antes de dormir.")
        }

        if (input.hasLateCaffeine) {
            recommendations.add("Se detectó consumo de cafeína tardío. Se aconseja adelantar la última toma a antes de las 16:00 para proteger el sueño REM.")
        }

        if (input.dinnerTimingHoursBeforeBed < 2.5f) {
            recommendations.add("Procura cenar al menos 2.5 a 3 horas antes de dormir para prevenir elevación de frecuencia cardíaca nocturna por digestión.")
        }

        if (input.hasMagnesiumZincIntake) {
            recommendations.add("El aporte de Magnesio & Zinc en la cena favorece la relajación neuromuscular y atenúa el cortisol nocturno.")
        } else {
            recommendations.add("Añade suplementación con Magnesio (bisglicinato) + Zinc para potenciar la asimilación proteica y calidad del sueño.")
        }

        if (input.hydrationLiters < 2.2f) {
            recommendations.add("Eleva la hidratación diurna a mínimo 2.5L. La deshidratación leve eleva la frecuencia cardíaca en reposo.")
        }

        if (input.unloggedBackgroundSteps >= 8000) {
            recommendations.add("Reloj detectó alta actividad no registrada: ${input.unloggedBackgroundSteps} pasos en segundo plano. Incrementa reposo y recarga de glucógeno.")
        }

        val neatImpactSummary = "Reloj detectó ${input.unloggedBackgroundSteps} pasos libres (${input.unloggedActiveMovements}). Carga adicional NEAT asimilada por el motor IA."

        val dietFeedback = buildString {
            append("Cena: ")
            if (input.dinnerTimingHoursBeforeBed >= 2.5f) append("Tiempo adecuado (>${input.dinnerTimingHoursBeforeBed}h previo). ") else append("Cena muy cercana al descanso. ")
            append("Suplementación: ")
            if (input.hasMagnesiumZincIntake) append("Magnesio + Zinc Activo. ") else append("Sin suplemento nocturno. ")
            append("Hidratación: ${String.format(Locale.getDefault(), "%.1f", input.hydrationLiters)}L/día.")
        }

        return SleepAnalysisResult(
            sleepScore = finalScore,
            qualityBadge = badge,
            hoursSlept = input.hoursSlept,
            restingHeartRateBpm = input.restingHeartRateBpm,
            deepSleepPercentage = input.deepSleepPercentage,
            remSleepPercentage = input.remSleepPercentage,
            summary = summary,
            recommendations = recommendations,
            neatImpactSummary = neatImpactSummary,
            dietSupplementFeedback = dietFeedback
        )
    }
}
