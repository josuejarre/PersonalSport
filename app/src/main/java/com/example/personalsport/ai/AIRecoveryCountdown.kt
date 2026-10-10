package com.example.personalsport.ai

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecoveryInput(
    val lastWorkoutTimestampMillis: Long = System.currentTimeMillis() - (19.5 * 3600 * 1000L).toLong(), // 19.5h ago default
    val workoutIntensity: String = "ALTA", // "ALTA", "MEDIA", "SUAVE"
    val workoutDurationMinutes: Int = 60,
    val muscleZonesTrained: List<String> = listOf("Cuádriceps", "Isquiotibiales", "Gemelos"),
    val unloggedBackgroundSteps: Int = 10400,
    val unloggedNeatMinutes: Int = 45,
    val unloggedMovementSummary: String = "Paseos urbanos, caminata activa y escaleras",
    val sleepScore: Int = 78,
    val restingHeartRateBpm: Int = 54,
    val athleteAge: Int = 28,
    val currentTimeMillis: Long = System.currentTimeMillis()
)

data class RecoveryCountdownResult(
    val totalRecoveryRequiredHours: Int,
    val targetOptimalWorkoutTimeMillis: Long,
    val remainingTimeMillis: Long,
    val isFullyRecovered: Boolean,
    val formattedRemainingTime: String, // e.g. "Restan: 04h 25m"
    val neatAnalysisSummary: String,
    val recommendedTimeWindow: String,
    val recommendedNextSessionIntensity: String,
    val recommendationBadgeText: String,
    val muscleRecoveryPercentage: Int, // 0..100
    val targetTimeString: String // e.g. "Hoy 18:30h"
)

object AIRecoveryCountdown {

    fun calculateRecovery(input: RecoveryInput): RecoveryCountdownResult {
        // Base recovery hours based on workout intensity and duration
        var requiredHours = when {
            input.workoutIntensity.contains("ALTA", ignoreCase = true) || input.workoutIntensity.contains("INTENSE", ignoreCase = true) -> {
                if (input.workoutDurationMinutes >= 60) 36 else 24
            }
            input.workoutIntensity.contains("MEDIA", ignoreCase = true) || input.workoutIntensity.contains("MODERATE", ignoreCase = true) -> {
                if (input.workoutDurationMinutes >= 60) 24 else 18
            }
            else -> 14
        }

        // Adjust for unlogged background steps & active NEAT
        if (input.unloggedBackgroundSteps >= 12000 || input.unloggedNeatMinutes >= 60) {
            requiredHours += 6
        } else if (input.unloggedBackgroundSteps >= 8000 || input.unloggedNeatMinutes >= 30) {
            requiredHours += 4
        } else if (input.unloggedBackgroundSteps >= 5000) {
            requiredHours += 2
        }

        // Adjust for sleep quality and resting heart rate
        if (input.sleepScore >= 85 && input.restingHeartRateBpm < 58) {
            requiredHours -= 4 // Accelerated recovery
        } else if (input.sleepScore < 60 || input.restingHeartRateBpm >= 72) {
            requiredHours += 6 // Delayed recovery
        }

        // Adjust for age
        if (input.athleteAge > 40) {
            requiredHours += 4
        } else if (input.athleteAge > 30) {
            requiredHours += 2
        }

        val finalRequiredHours = requiredHours.coerceIn(12, 60)

        val targetOptimalWorkoutTimeMillis = input.lastWorkoutTimestampMillis + (finalRequiredHours * 3600 * 1000L)
        val remainingMillis = targetOptimalWorkoutTimeMillis - input.currentTimeMillis

        val isFullyRecovered = remainingMillis <= 0
        val muscleRecoveryPercentage = if (isFullyRecovered) {
            100
        } else {
            val totalDurationMillis = finalRequiredHours * 3600 * 1000L
            val elapsedMillis = totalDurationMillis - remainingMillis
            ((elapsedMillis.toDouble() / totalDurationMillis) * 100).toInt().coerceIn(0, 99)
        }

        val formattedRemainingTime = if (isFullyRecovered) {
            "Restan: 00h 00m"
        } else {
            val totalSecs = remainingMillis / 1000
            val hours = totalSecs / 3600
            val mins = (totalSecs % 3600) / 60
            String.format(Locale.getDefault(), "Restan: %02dh %02dm", hours, mins)
        }

        val sdf = SimpleDateFormat("EEE HH:mm'h'", Locale.getDefault())
        val targetTimeString = sdf.format(Date(targetOptimalWorkoutTimeMillis))

        val (timeWindow, nextIntensity, badgeText) = when {
            isFullyRecovered -> Triple(
                "¡Ventana de entrenamiento abierta! Musculatura 100% regenerada.",
                "Fuerza Explosiva, Series VO2Max o Carga Máxima",
                "100% Recuperado • Máximo Rendimiento"
            )
            remainingMillis <= 4 * 3600 * 1000L -> Triple(
                "Ventana óptima estimada a las $targetTimeString",
                "Fuerza Moderada o Carrera Progresiva",
                "Recuperación Avanzada ($muscleRecoveryPercentage%)"
            )
            remainingMillis <= 12 * 3600 * 1000L -> Triple(
                "Recomendado entrenar a partir de $targetTimeString",
                "Trote Suave Regenerativo, Natación o Movilidad",
                "Asimilación de Carga ($muscleRecoveryPercentage%)"
            )
            else -> Triple(
                "Ventana de súper-compensación: $targetTimeString",
                "Descanso Activo, Movilidad y Estiramientos",
                "Fase de Regeneración Muscular ($muscleRecoveryPercentage%)"
            )
        }

        val neatSummary = "Reloj: ${input.unloggedBackgroundSteps} pasos libres (${input.unloggedNeatMinutes}m de movimiento voluntario: ${input.unloggedMovementSummary}). Carga extra asimilada: +${if (input.unloggedBackgroundSteps >= 8000) 4 else 2}h."

        return RecoveryCountdownResult(
            totalRecoveryRequiredHours = finalRequiredHours,
            targetOptimalWorkoutTimeMillis = targetOptimalWorkoutTimeMillis,
            remainingTimeMillis = remainingMillis,
            isFullyRecovered = isFullyRecovered,
            formattedRemainingTime = formattedRemainingTime,
            neatAnalysisSummary = neatSummary,
            recommendedTimeWindow = timeWindow,
            recommendedNextSessionIntensity = nextIntensity,
            recommendationBadgeText = badgeText,
            muscleRecoveryPercentage = muscleRecoveryPercentage,
            targetTimeString = targetTimeString
        )
    }
}
