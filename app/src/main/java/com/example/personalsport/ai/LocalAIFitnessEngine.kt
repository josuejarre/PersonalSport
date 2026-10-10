package com.example.personalsport.ai

import com.example.personalsport.data.local.entity.BodyMetric
import com.example.personalsport.data.local.entity.Equipment
import com.example.personalsport.data.local.entity.WorkoutLog
import com.example.personalsport.ui.planner.GeneratedRoutine
import com.example.personalsport.ui.planner.PlannedExerciseStep
import com.example.personalsport.ui.planner.SportType
import java.util.Locale
import kotlin.math.abs

enum class AthleticsSeriesType(val displayName: String, val description: String) {
    INTERVALS_400M("Intervalos 400m", "Series de 400m a ritmo VO2 Máx con recuperación trotando"),
    REPETITIONS_1000M("Repeticiones 1000m", "Repeticiones de 1km para mejorar el umbral de lactato"),
    PYRAMID_SERIES("Series Piramidales", "Pirámide 200m-400m-800m-400m-200m para potencia y resistencia"),
    FARTLEK("Fartlek / Cambios de Ritmo", "Juego de velocidades alternando tramos rápidos y de recuperación"),
    SPRINT_SERIES("Series de Velocidad / Sprints", "Sprints explosivos de 100m-200m para velocidad máxima"),
    RECOVERY_PACES("Series Aeróbicas de Recuperación", "Rodaje continuo suave para asimilación de carga")
}

enum class InsightTrendType {
    FAT_LOSS,
    MUSCLE_GAIN,
    WEIGHT_LOSS,
    MAINTENANCE,
    RECOVERY_RECOMMENDED
}

data class AiProgressInsight(
    val summary: String,
    val recommendation: String,
    val trendType: InsightTrendType,
    val fatChangeDeltaPct: Float? = null,
    val weightChangeDeltaKg: Float? = null,
    val estimatedLeanMassChangeKg: Float? = null,
    val currentBmi: Float? = null,
    val bmiCategory: String? = null
)

/**
 * Motor de Inteligencia Artificial Local de Acondicionamiento Físico y Atletismo.
 *
 * Algoritmos de prescripción:
 * - Genera rutinas adaptativas basadas en la disponibilidad de material y el historial de entrenamiento.
 * - Realiza análisis determinista de tendencias de recomposición corporal e índice de masa corporal (IMC).
 */
object LocalAIFitnessEngine {

    /**
     * Genera una rutina de entrenamiento optimizada según la disciplina deportiva,
     * la duración objetivo, la intensidad, el equipamiento del usuario y su historial de carga.
     */
    fun generateWorkoutRoutine(
        sportType: SportType,
        athleticsSeriesType: AthleticsSeriesType = AthleticsSeriesType.INTERVALS_400M,
        targetDurationMinutes: Int,
        intensity: String,
        equipmentList: List<Equipment>,
        workoutHistory: List<WorkoutLog>
    ): GeneratedRoutine {
        val totalPastWorkouts = workoutHistory.size
        val recentRunCount = workoutHistory.count { it.workoutName.contains("run", ignoreCase = true) || it.workoutName.contains("atletismo", ignoreCase = true) || it.workoutName.contains("series", ignoreCase = true) }
        val isExperienced = totalPastWorkouts >= 10 || recentRunCount >= 5

        return when (sportType) {
            SportType.ATHLETICS_SERIES -> generateAthleticsRoutine(
                athleticsSeriesType = athleticsSeriesType,
                duration = targetDurationMinutes,
                intensity = intensity,
                equipment = equipmentList,
                isExperienced = isExperienced
            )
            SportType.RUNNING_SHORT -> generateRunningShortRoutine(targetDurationMinutes, intensity, equipmentList, isExperienced)
            SportType.RUNNING_MEDIUM -> generateRunningMediumRoutine(targetDurationMinutes, intensity, equipmentList, isExperienced)
            SportType.RUNNING_LONG -> generateRunningLongRoutine(targetDurationMinutes, intensity, equipmentList, isExperienced)
            SportType.SWIM -> generateSwimRoutine(targetDurationMinutes, intensity, isExperienced)
            SportType.STRENGTH -> generateStrengthRoutine(targetDurationMinutes, intensity, equipmentList, workoutHistory)
        }
    }

    private fun generateAthleticsRoutine(
        athleticsSeriesType: AthleticsSeriesType,
        duration: Int,
        intensity: String,
        equipment: List<Equipment>,
        isExperienced: Boolean
    ): GeneratedRoutine {
        val trackGear = equipment.find {
            it.name.contains("pista", ignoreCase = true) ||
            it.name.contains("zapatillas", ignoreCase = true) ||
            it.name.contains("spikes", ignoreCase = true) ||
            it.name.contains("treadmill", ignoreCase = true) ||
            it.name.contains("reloj", ignoreCase = true)
        }?.name ?: "Pista de Atletismo / Calzado de Serie"

        val warmupSteps = listOf(
            PlannedExerciseStep("Calentamiento Articular & Movilidad", "Rotación de tobillos, rodillas, cadera y brazos", "5 mins"),
            PlannedExerciseStep("Trote Progresivo de Activación", "Trote suave aumentando el ritmo progresivamente", "7 mins", trackGear),
            PlannedExerciseStep("Técnica de Carrera & Dribling", "Skips A/B, impulsos de tobillo y 3 progresiones de 60m", "8 mins")
        )

        val mainSetSteps = mutableListOf<PlannedExerciseStep>()
        val cooldownSteps = listOf(
            PlannedExerciseStep("Trote de Descalentamiento", "Trote muy suave para bajar pulsaciones", "5 mins"),
            PlannedExerciseStep("Estiramientos Estáticos & Movilidad", "Énfasis en isquiotibiales, gemelos y sóleos", "5 mins")
        )

        val seriesMultiplier = if (isExperienced) 1.2f else 1.0f

        val (title, notes) = when (athleticsSeriesType) {
            AthleticsSeriesType.INTERVALS_400M -> {
                val reps = (if (intensity.contains("High", ignoreCase = true)) 8 else (6 * seriesMultiplier).toInt())
                mainSetSteps.add(
                    PlannedExerciseStep(
                        title = "$reps x 400m a Ritmo VO2 Máx",
                        description = "400m intensos (85-90% FC Máx) con 90 segundos de recuperación al trote",
                        durationOrReps = "${reps * 3} mins",
                        equipmentUsed = trackGear
                    )
                )
                mainSetSteps.add(
                    PlannedExerciseStep(
                        title = "Salidas y Transferencia Explosiva",
                        description = "3x 60m salidas potentes enfocadas en la zancada inicial",
                        durationOrReps = "5 mins",
                        equipmentUsed = trackGear
                    )
                )
                Pair(
                    "Atletismo - Series de 400m ($reps Repeticiones)",
                    "AI Recommendation: Optimizado para desarrollo de capacidad glucolítica y potencia aeróbica en $trackGear."
                )
            }
            AthleticsSeriesType.REPETITIONS_1000M -> {
                val reps = (if (intensity.contains("High", ignoreCase = true)) 5 else (4 * seriesMultiplier).toInt())
                mainSetSteps.add(
                    PlannedExerciseStep(
                        title = "$reps x 1000m a Ritmo Umbral Lactato",
                        description = "1km a ritmo sostenido de competición con 2 minutos de recuperación activa caminando/trotando",
                        durationOrReps = "${reps * 5} mins",
                        equipmentUsed = trackGear
                    )
                )
                Pair(
                    "Atletismo - Repeticiones de 1000m ($reps Repeticiones)",
                    "AI Recommendation: Diseñado para elevar el umbral de lactato y ritmo medio de carrera."
                )
            }
            AthleticsSeriesType.PYRAMID_SERIES -> {
                mainSetSteps.add(
                    PlannedExerciseStep(
                        title = "Pirámide Completa: 200m - 400m - 800m - 400m - 200m",
                        description = "200m acelerado, 400m ritmo 1500m, 800m ritmo 3k, 400m acelerado, 200m sprint. Rec 2 min entre series",
                        durationOrReps = "22 mins",
                        equipmentUsed = trackGear
                    )
                )
                Pair(
                    "Atletismo - Series Piramidales",
                    "AI Recommendation: Combina picos de velocidad con resistencia láctica adaptativa."
                )
            }
            AthleticsSeriesType.FARTLEK -> {
                mainSetSteps.add(
                    PlannedExerciseStep(
                        title = "Bloque Fartlek continuo (1m Fuerte / 1m Suave)",
                        description = "Alterna 1 minuto a ritmo de competición con 1 minuto de trote regenerativo sin detenerse",
                        durationOrReps = "${duration - 25} mins",
                        equipmentUsed = trackGear
                    )
                )
                Pair(
                    "Atletismo - Fartlek & Cambios de Ritmo",
                    "AI Recommendation: Ideal para mejorar la recuperación en carrera y adaptabilidad de ritmo."
                )
            }
            AthleticsSeriesType.SPRINT_SERIES -> {
                mainSetSteps.add(
                    PlannedExerciseStep(
                        title = "6x 100m Sprints Máximos",
                        description = "100m al 95-100% de esfuerzo con recuperación completa (2 mins caminando)",
                        durationOrReps = "12 mins",
                        equipmentUsed = trackGear
                    )
                )
                mainSetSteps.add(
                    PlannedExerciseStep(
                        title = "4x 200m Series de Lanzamiento",
                        description = "200m progresivos manteniendo alta frecuencia de zancada",
                        durationOrReps = "10 mins",
                        equipmentUsed = trackGear
                    )
                )
                Pair(
                    "Atletismo - Series de Velocidad Pura",
                    "AI Recommendation: Desarrolla reclutamiento de fibras rápidas y eficiencia biomecánica."
                )
            }
            AthleticsSeriesType.RECOVERY_PACES -> {
                mainSetSteps.add(
                    PlannedExerciseStep(
                        title = "Rodaje Aeróbico Continuo Zona 1-2",
                        description = "Carrera suave y fluida donde se pueda mantener una conversación sin fatiga",
                        durationOrReps = "${duration - 20} mins",
                        equipmentUsed = trackGear
                    )
                )
                mainSetSteps.add(
                    PlannedExerciseStep(
                        title = "4 Rectas Progresivas de 80m",
                        description = "Aceleraciones suaves para soltar piernas sin acumular lactato",
                        durationOrReps = "5 mins",
                        equipmentUsed = trackGear
                    )
                )
                Pair(
                    "Atletismo - Ritmo de Recuperación Aeróbica",
                    "AI Recommendation: Promueve la vascularización muscular y la asimilación de entrenamientos previos."
                )
            }
        }

        return GeneratedRoutine(
            title = title,
            sportType = SportType.ATHLETICS_SERIES,
            targetDurationMinutes = duration,
            intensity = intensity,
            warmup = warmupSteps,
            mainSet = mainSetSteps,
            cooldown = cooldownSteps,
            notes = notes
        )
    }

    private fun generateRunningShortRoutine(
        duration: Int,
        intensity: String,
        equipment: List<Equipment>,
        isExperienced: Boolean
    ): GeneratedRoutine {
        val treadmill = equipment.find { it.name.contains("treadmill", ignoreCase = true) || it.name.contains("cinta", ignoreCase = true) }
        val eqName = treadmill?.name ?: "Pista / Ruta Corta"
        val reps = if (isExperienced) 6 else 5

        val mainSteps = listOf(
            PlannedExerciseStep("${reps}x 400m Intervalos Rápidos", "Ritmo fuerte @ 85-90% esfuerzo con 90s trote suave", "15 mins", eqName),
            PlannedExerciseStep("4x 200m Aceleraciones", "Enfoque en alta cadencia y técnica", "8 mins", eqName),
            PlannedExerciseStep("Bloque de Estabilidad Core", "Planchas y escaladores", "5 mins", "Peso Corporal")
        )

        return GeneratedRoutine(
            title = "Running 3k-5k - Velocidad e Intervalos",
            sportType = SportType.RUNNING_SHORT,
            targetDurationMinutes = duration,
            intensity = intensity,
            warmup = listOf(
                PlannedExerciseStep("Calentamiento Dinámico", "Balanceos de pierna, elevación de rodillas", "5 mins"),
                PlannedExerciseStep("Trote de Calentamiento", "Trote aeróbico liviano", "5 mins", eqName)
            ),
            mainSet = mainSteps,
            cooldown = listOf(
                PlannedExerciseStep("Caminata de Recuperación", "Caminata suave y respiración", "5 mins"),
                PlannedExerciseStep("Estiramiento Isquiotibiales y Gemelos", "Mantener 30s cada estiramiento", "5 mins")
            ),
            notes = "AI Engine: Ideal para mejorar velocidad en 5k y consumo máximo de oxígeno (VO2 Máx)."
        )
    }

    private fun generateRunningMediumRoutine(
        duration: Int,
        intensity: String,
        equipment: List<Equipment>,
        isExperienced: Boolean
    ): GeneratedRoutine {
        val eqName = equipment.find { it.name.contains("treadmill", ignoreCase = true) }?.name ?: "Ruta Media / Asfalto"
        val noteExtra = if (isExperienced) " Nivel Avanzado: Mantener ritmo constante en la segunda mitad." else ""

        val mainSteps = listOf(
            PlannedExerciseStep("Bloque de Carrera a Ritmo Umbral", "Ritmo sostenido @ 80-85% FC Máx", "${duration - 20} mins", eqName),
            PlannedExerciseStep("Ejercicios de Cadencia", "3x 1 minuto a alta frecuencia de zancada", "5 mins", eqName)
        )

        return GeneratedRoutine(
            title = "Running 8k-12k - Ritmo Tempo Sostenido",
            sportType = SportType.RUNNING_MEDIUM,
            targetDurationMinutes = duration,
            intensity = intensity,
            warmup = listOf(PlannedExerciseStep("Trote Progresivo", "Aceleración gradual", "7 mins", eqName)),
            mainSet = mainSteps,
            cooldown = listOf(PlannedExerciseStep("Enfriamiento y Movilidad", "Trote suave y estiramiento de flexores", "8 mins")),
            notes = "AI Engine: Enfocado en mantener ritmo constante y resistencia media.$noteExtra"
        )
    }

    private fun generateRunningLongRoutine(
        duration: Int,
        intensity: String,
        equipment: List<Equipment>,
        isExperienced: Boolean
    ): GeneratedRoutine {
        val eqName = equipment.find { it.name.contains("treadmill", ignoreCase = true) }?.name ?: "Ruta Larga / Trail"
        val noteExtra = if (isExperienced) " Incluye aceleraciones finales sostenidas." else ""

        return GeneratedRoutine(
            title = "Running 15k+ - Tirada Larga Aeróbica",
            sportType = SportType.RUNNING_LONG,
            targetDurationMinutes = duration,
            intensity = intensity,
            warmup = listOf(PlannedExerciseStep("Caminata y Trote Suave", "Inicio progresivo", "10 mins")),
            mainSet = listOf(
                PlannedExerciseStep("Carrera de Resistencia Zona 2", "Ritmo conversacional aeróbico constante", "${duration - 25} mins", eqName),
                PlannedExerciseStep("Aceleración Final", "Últimos 5 minutos aumentando ligeramente el ritmo", "5 mins", eqName)
            ),
            cooldown = listOf(PlannedExerciseStep("Movilidad y Rodillo de Espuma", "Enfoque en cuadríceps, gemelos e isquios", "10 mins")),
            notes = "AI Engine: Construcción de base aeróbica para largas distancias. Mantén buena hidratación.$noteExtra"
        )
    }

    private fun generateSwimRoutine(
        duration: Int,
        intensity: String,
        isExperienced: Boolean
    ): GeneratedRoutine {
        val reps = if (isExperienced) 8 else 6
        return GeneratedRoutine(
            title = "Natación - Intervalos y Técnica",
            sportType = SportType.SWIM,
            targetDurationMinutes = duration,
            intensity = intensity,
            warmup = listOf(
                PlannedExerciseStep("200m Estilo Libre Suave", "Calentamiento articular en agua", "5 mins"),
                PlannedExerciseStep("100m Patada con Tabla", "Aislamiento de patada y rotación de cadera", "5 mins")
            ),
            mainSet = listOf(
                PlannedExerciseStep("${reps}x 50m Series Rápidas", "Ritmo 80%, 20s descanso entre series", "10 mins"),
                PlannedExerciseStep("4x 100m Pull Buoy", "Enfoque en agarre de tracción y fuerza de brazada", "12 mins"),
                PlannedExerciseStep("4x 25m Sprints", "Brazada rápida con descanso completo", "5 mins")
            ),
            cooldown = listOf(
                PlannedExerciseStep("150m Estilo Libre o Espalda Suave", "Recuperación activa y soltura", "5 mins")
            ),
            notes = "AI Engine: Mejora la eficiencia de brazada y la capacidad aeróbica acuática."
        )
    }

    private fun generateStrengthRoutine(
        duration: Int,
        intensity: String,
        equipment: List<Equipment>,
        workoutHistory: List<WorkoutLog>
    ): GeneratedRoutine {
        val mainSteps = mutableListOf<PlannedExerciseStep>()
        val totalPast = workoutHistory.size

        if (equipment.isEmpty()) {
            mainSteps.add(PlannedExerciseStep("Flexiones de Pecho (Pushups)", "3 series de 15-20 repeticiones", "8 mins", "Peso Corporal"))
            mainSteps.add(PlannedExerciseStep("Sentadillas y Sentadillas con Salto", "4 series de 20 repeticiones", "8 mins", "Peso Corporal"))
            mainSteps.add(PlannedExerciseStep("Planchas & Escaladores", "3 series de 60 segundos", "6 mins", "Peso Corporal"))
            mainSteps.add(PlannedExerciseStep("Zancadas Alternas", "3 series de 12 reps por pierna", "6 mins", "Peso Corporal"))
        } else {
            equipment.forEach { eq ->
                val name = eq.name
                when {
                    name.contains("leg extension", ignoreCase = true) || name.contains("extensiones", ignoreCase = true) -> {
                        mainSteps.add(PlannedExerciseStep("Extensión de Cuadríceps", "4 series de 12-15 reps con fase excéntrica lenta", "8 mins", name))
                    }
                    name.contains("treadmill", ignoreCase = true) || name.contains("cinta", ignoreCase = true) || name.contains("bici", ignoreCase = true) -> {
                        mainSteps.add(PlannedExerciseStep("Finisher Cardio HIIT", "5 rondas de 30s sprint / 30s descanso", "6 mins", name))
                    }
                    name.contains("press", ignoreCase = true) || name.contains("pecho", ignoreCase = true) -> {
                        mainSteps.add(PlannedExerciseStep("Press de Pecho / Press Militar", "4 series de 10-12 reps", "8 mins", name))
                    }
                    name.contains("polea", ignoreCase = true) || name.contains("cable", ignoreCase = true) || name.contains("pulley", ignoreCase = true) -> {
                        mainSteps.add(PlannedExerciseStep("Jalón en Polea / Cruces de Cable", "3 series de 12 reps", "7 mins", name))
                    }
                    name.contains("mancuerna", ignoreCase = true) || name.contains("dumb", ignoreCase = true) -> {
                        mainSteps.add(PlannedExerciseStep("Curl de Bíceps & Press de Hombros", "3 series de 12 reps", "8 mins", name))
                    }
                    name.contains("kettle", ignoreCase = true) || name.contains("pesa rusa", ignoreCase = true) -> {
                        mainSteps.add(PlannedExerciseStep("Kettlebell Swings & Sentadilla Goblet", "4 series de 15 reps", "8 mins", name))
                    }
                    name.contains("barra", ignoreCase = true) || name.contains("barbell", ignoreCase = true) -> {
                        mainSteps.add(PlannedExerciseStep("Sentadillas / Peso Muerto con Barra", "4 series de 8-10 reps", "10 mins", name))
                    }
                    else -> {
                        mainSteps.add(PlannedExerciseStep("Ejercicio Específico en $name", "3 series de 10-12 reps", "7 mins", name))
                    }
                }
            }
        }

        val historyNote = if (totalPast > 0) " Basado en $totalPast entrenamientos registrados previo." else ""

        return GeneratedRoutine(
            title = "Fuerza & Acondicionamiento (${equipment.size} Equipos)",
            sportType = SportType.STRENGTH,
            targetDurationMinutes = duration,
            intensity = intensity,
            warmup = listOf(
                PlannedExerciseStep("Movilidad Articular General", "Círculos de brazos, apertura de cadera, cardio suave", "5 mins")
            ),
            mainSet = mainSteps.take(6),
            cooldown = listOf(
                PlannedExerciseStep("Estiramientos e Hipertrofia Recovery", "Estiramiento enfocado en pecho, espalda y piernas", "5 mins")
            ),
            notes = if (equipment.isEmpty()) "AI Engine: Ajustado para peso corporal. Agrega más equipos en el catálogo para personalizar.$historyNote" else "AI Engine: Rutina optimizada utilizando tu inventario de equipamiento guardado.$historyNote"
        )
    }

    /**
     * Performs on-device trend analysis on body metrics (weight, body fat %, estimated muscle mass, BMI)
     * and produces actionable AI insights and recommendations.
     */
    fun analyzeBodyMetrics(
        metrics: List<BodyMetric>,
        userHeightMeters: Float = 1.75f
    ): AiProgressInsight {
        if (metrics.isEmpty()) {
            return AiProgressInsight(
                summary = "Sin métricas registradas - Añade tu peso y % de grasa",
                recommendation = "Ingresa tus métricas corporales en la sección superior para que el motor de IA analice tu progreso y composición corporal.",
                trendType = InsightTrendType.MAINTENANCE
            )
        }

        val sortedMetrics = metrics.sortedBy { it.date }
        val latest = sortedMetrics.last()
        val currentBmi = latest.weightKg / (userHeightMeters * userHeightMeters)
        val bmiCategory = when {
            currentBmi < 18.5f -> "Bajo peso"
            currentBmi in 18.5f..24.99f -> "Peso normal / Saludable"
            currentBmi in 25.0f..29.99f -> "Sobrepeso"
            else -> "Obesidad"
        }

        if (sortedMetrics.size == 1) {
            return AiProgressInsight(
                summary = "Métrica inicial: ${String.format(Locale.US, "%.1f", latest.weightKg)} kg (IMC ${String.format(Locale.US, "%.1f", currentBmi)} - $bmiCategory)",
                recommendation = "Registra periódicamente tu peso y % de grasa para habilitar el análisis comparativo de tendencias de IA.",
                trendType = InsightTrendType.MAINTENANCE,
                currentBmi = currentBmi,
                bmiCategory = bmiCategory
            )
        }

        val earliest = sortedMetrics.first()
        val weightDelta = latest.weightKg - earliest.weightKg

        val latestFat = latest.bodyFatPercentage
        val earliestFat = earliest.bodyFatPercentage

        var fatDelta: Float? = null
        var leanMassDelta: Float? = null

        if (latestFat != null && earliestFat != null) {
            fatDelta = latestFat - earliestFat
            val earliestLeanMass = earliest.weightKg * (1f - (earliestFat / 100f))
            val latestLeanMass = latest.weightKg * (1f - (latestFat / 100f))
            leanMassDelta = latestLeanMass - earliestLeanMass
        }

        return when {
            // Case 1: Muscle Mass Gain rate increase (e.g. lean mass gain >= 0.3kg and weight did not drop significantly)
            leanMassDelta != null && leanMassDelta >= 0.3f && weightDelta >= -0.2f -> {
                AiProgressInsight(
                    summary = "Ganancia muscular estimada de +${String.format(Locale.US, "%.1f", leanMassDelta)} kg - Rendimiento en aumento",
                    recommendation = "Excelente desarrollo de masa muscular magra. Continúa con tu progresión de cargas en gimnasio y rutinas de alta intensidad para maximizar fuerza.",
                    trendType = InsightTrendType.MUSCLE_GAIN,
                    fatChangeDeltaPct = fatDelta,
                    weightChangeDeltaKg = weightDelta,
                    estimatedLeanMassChangeKg = leanMassDelta,
                    currentBmi = currentBmi,
                    bmiCategory = bmiCategory
                )
            }

            // Case 2: Significant Body Fat reduction (e.g., <= -0.5%)
            fatDelta != null && fatDelta <= -0.5f -> {
                val absFatChange = abs(fatDelta)
                AiProgressInsight(
                    summary = "Grasa reducida ${String.format(Locale.US, "%.1f", absFatChange)}% en el periodo analizado - Incrementar trabajo de hipertrofia",
                    recommendation = "Tu porcentaje de grasa corporal ha disminuido favorablemente. Para consolidar el progreso y optimizar la composición corporal, te recomendamos priorizar sesiones de hipertrofia y trabajo de fuerza.",
                    trendType = InsightTrendType.FAT_LOSS,
                    fatChangeDeltaPct = fatDelta,
                    weightChangeDeltaKg = weightDelta,
                    estimatedLeanMassChangeKg = leanMassDelta,
                    currentBmi = currentBmi,
                    bmiCategory = bmiCategory
                )
            }

            // Case 3: Weight loss overall
            weightDelta <= -1.0f -> {
                val absWeight = abs(weightDelta)
                AiProgressInsight(
                    summary = "Reducción de peso de ${String.format(Locale.US, "%.1f", absWeight)} kg - Monitorear hidratación y recuperación",
                    recommendation = "Has reducido peso corporal de manera constante. Mantén un aporte adecuado de proteínas y electrolitos, e incluye sesiones de atletismo o carrera suave.",
                    trendType = InsightTrendType.WEIGHT_LOSS,
                    fatChangeDeltaPct = fatDelta,
                    weightChangeDeltaKg = weightDelta,
                    estimatedLeanMassChangeKg = leanMassDelta,
                    currentBmi = currentBmi,
                    bmiCategory = bmiCategory
                )
            }

            // Case 4: Stable / Maintenance
            else -> {
                AiProgressInsight(
                    summary = "Composición corporal estable (${String.format(Locale.US, "%.1f", latest.weightKg)} kg, IMC ${String.format(Locale.US, "%.1f", currentBmi)})",
                    recommendation = "Tus métricas corporales se mantienen constantes. Buen momento para introducir series de atletismo (400m / 1000m) o aumentar la intensidad en tus entrenamientos.",
                    trendType = InsightTrendType.MAINTENANCE,
                    fatChangeDeltaPct = fatDelta,
                    weightChangeDeltaKg = weightDelta,
                    estimatedLeanMassChangeKg = leanMassDelta,
                    currentBmi = currentBmi,
                    bmiCategory = bmiCategory
                )
            }
        }
    }

    /**
     * Generates a tailored post-workout AI recommendation based on the athlete's
     * latest completed workout (e.g. Running, Swimming, Strength, or Athletics).
     */
    fun generatePostWorkoutRecommendation(latestWorkout: WorkoutLog?): String {
        if (latestWorkout == null) {
            return "Aún no se registran entrenamientos recientes. Completa una sesión o sincroniza tu dispositivo para recibir recomendaciones nutricionales y de recuperación IA personalizadas."
        }

        val name = latestWorkout.workoutName
        val nameLower = name.lowercase(Locale.ROOT)

        val advice = when {
            nameLower.contains("carrera") || nameLower.contains("running") || nameLower.contains("run") || nameLower.contains("maratón") || nameLower.contains("10.5k") || nameLower.contains("5k") || nameLower.contains("10k") -> {
                "Carga 60g de carbohidratos de digestión media, mantén rehidratación con electrolitos y realiza estiramientos de sóleos e isquiotibiales antes de la siguiente sesión de fuerza"
            }
            nameLower.contains("natación") || nameLower.contains("piscina") || nameLower.contains("swim") || nameLower.contains("aguas abiertas") -> {
                "Prioriza rehidratación hipotónica con sales mineralizadas, consume 30g de proteína de rápida absorción con carbohidratos simples y realiza movilidad de manguito rotador y dorsales"
            }
            nameLower.contains("musculación") || nameLower.contains("fuerza") || nameLower.contains("strength") || nameLower.contains("gym") || nameLower.contains("hipertrofia") -> {
                "Asegura 35g de proteína rápida rica en leucina, 5g de monohidrato de creatina y realiza movilidad estática pectoral y dorsal para optimizar la síntesis proteica muscular"
            }
            nameLower.contains("atletismo") || nameLower.contains("series") || nameLower.contains("intervalos") || nameLower.contains("fartlek") || nameLower.contains("sprint") -> {
                "Repón glucógeno muscular con 50g de carbohidratos de absorción media/rápida, rehidrata con electrolitos sódicos y realiza movilidad y descarga de sóleos e isquiotibiales"
            }
            else -> {
                "Optimiza la ventana metabólica con una ingesta equilibrada de proteínas y carbohidratos, rehidrata adecuadamente y asegura reposo activo para la asimilación del estímulo"
            }
        }

        return "Tras tu última sesión ($name): $advice"
    }
}
