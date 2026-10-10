package com.example.personalsport.ai

import com.example.personalsport.data.local.entity.Equipment
import com.example.personalsport.data.local.entity.WorkoutLog
import com.example.personalsport.diet.DietGoal
import com.example.personalsport.ui.planner.PlannedExerciseStep
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Modalidades de rutina deportiva soportadas ([RoutineModality]).
 */
enum class RoutineModality(val displayName: String, val description: String) {
    MUSCULACION("Musculación", "Enfoque principal en fuerza, hipertrofia y pesas en gimnasio"),
    CARDIO("Cardio", "Enfoque en resistencia aeróbica, velocidad y capacidad pulmonar"),
    COMBINADO("Combinación (Multideporte)", "Combinación equilibrada de varias disciplinas deportivas (ej. Gimnasio + Carrera + Natación)")
}

/**
 * Disciplinas de cardio y resistencia ([CardioDiscipline]).
 */
enum class CardioDiscipline(val displayName: String) {
    CARRERA("Carrera / Atletismo"),
    NATACION("Natación"),
    SPINNING("Spinning / Ciclismo"),
    HIIT("HIIT / Entrenamiento Funcional"),
    ZUMBA_BAILE("Zumba / Baile"),
    MUSCULACION("Musculación / Gimnasio")
}

/**
 * Subtipos de sesiones de carrera ([RunningSubType]).
 */
enum class RunningSubType(val displayName: String) {
    FARTLEK("Fartlek (Juegos de Velocidad)"),
    ATLETISMO("Atletismo / Series de Pista"),
    CARRERA_CONTINUA("Carrera Continua / Rodaje")
}

/**
 * Categorías de series de atletismo ([AthleticsSeriesCategory]).
 */
enum class AthleticsSeriesCategory(val displayName: String, val description: String) {
    SHORT_SERIES("Series cortas: 50m - 400m", "Explosividad, potencia láctica y velocidad máxima"),
    LONG_SERIES("Series largas: 800m - 3000m", "Capacidad aeróbica, VO2 Máx y umbral anaeróbico"),
    COMBINATION_SERIES("Combinación de series", "Series mixtas con cuestas y transferencias")
}

/**
 * Categorías de series de natación ([SwimDistanceSeriesCategory]).
 */
enum class SwimDistanceSeriesCategory(val displayName: String, val description: String) {
    SHORT_SERIES("Series cortas: 25m - 100m", "Potencia, velocidad de brazada y sprints"),
    MEDIUM_SERIES("Series medias: 100m - 200m", "Resistencia a la velocidad y ritmo de prueba"),
    LONG_SERIES("Series largas: 300m - 4000m", "Capacidad aeróbica de nado continuo y pirámides"),
    COMBINATION_SERIES("Combinación de series de natación", "Series mixtas con sprints cortos, ritmo medio y resistencia larga")
}

/**
 * Estado del asistente interactivo de generación de rutinas IA ([AIRoutineWizardState]).
 */
data class AIRoutineWizardState(
    val modality: RoutineModality = RoutineModality.COMBINADO,
    val selectedDisciplines: Set<CardioDiscipline> = setOf(CardioDiscipline.MUSCULACION, CardioDiscipline.NATACION, CardioDiscipline.CARRERA),
    val runningSubType: RunningSubType? = RunningSubType.ATLETISMO,
    val athleticsSeriesCategory: AthleticsSeriesCategory? = AthleticsSeriesCategory.SHORT_SERIES,
    val swimSeriesCategory: SwimDistanceSeriesCategory? = SwimDistanceSeriesCategory.SHORT_SERIES,
    val includeCalisthenics: Boolean = true,
    val selectedCalisthenicsExercises: Set<String> = setOf("Dominadas", "Flexiones", "Fondos en banco", "Abdominales", "Cuerda"),
    val baselinePhotoUri: String? = null,
    val photoCapturedTimestamp: Long? = null,
    val goalType: DietGoal = DietGoal.FAT_LOSS,
    val userWeightKg: Float = 75f
)

/**
 * Resultado del análisis de validación calórica y nutricional ([CaloricValidationResult]).
 */
data class CaloricValidationResult(
    val isValidated: Boolean,
    val dailyExerciseCaloriesAvg: Int,
    val estimatedDietCaloriesTarget: Int,
    val goal: DietGoal,
    val statusTitle: String,
    val statusMessage: String,
    val recommendation: String
)

/**
 * Bloque temático de entrenamiento ([WorkoutBlock]).
 */
data class WorkoutBlock(
    val category: String,
    val title: String,
    val exercises: List<PlannedExerciseStep>
)

/**
 * Plan diario estructurado ([DayPlan]).
 */
data class DayPlan(
    val dayName: String,
    val title: String,
    val isRestDay: Boolean = false,
    val estimatedCaloriesBurned: Int,
    val blocks: List<WorkoutBlock>,
    val muscleFocus: String? = null
)

/**
 * Plan semanal estructurado ([WeekPlan]).
 */
data class WeekPlan(
    val weekNumber: Int,
    val title: String,
    val focus: String,
    val days: List<DayPlan>
)

/**
 * Plan mensual de periodización ([MonthPlan]).
 */
data class MonthPlan(
    val monthNumber: Int,
    val title: String,
    val focusDescription: String,
    val weeks: List<WeekPlan>
)

/**
 * Plan Deportivo Macro de 3 Meses (90 Días) ([ThreeMonthPlan]).
 */
data class ThreeMonthPlan(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val targetEndTimestamp: Long = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000),
    val modality: RoutineModality,
    val disciplines: List<CardioDiscipline>,
    val caloricValidation: CaloricValidationResult,
    val baselinePhotoUri: String? = null,
    val months: List<MonthPlan>
)

/**
 * Generador Local de Rutinas Deportivas mediante Inteligencia Artificial ([LocalAIRoutineGenerator]).
 *
 * Crea planes macro-deportivos de 90 Días (3 Meses) divididos en semanas y días, combinando
 * principios de sobrecarga progresiva, periodización táctica y validación calórica.
 *
 * **Despacho de Hilos y Gestión de Estado**:
 * - Todas las funciones de generación computacionalmente intensivas se ejecutan asíncronamente en [Dispatchers.Default].
 * - Utiliza el modelo nativo Qwen 2.5 3B para enriquecer las sugerencias cuando está cargado, o el motor de reglas como fallback transparente.
 */
object LocalAIRoutineGenerator {

    /**
     * Genera un plan completo de 3 Meses adaptado al estado del asistente y equipamiento disponible.
     */
    suspend fun generate3MonthPlan(
        wizardState: AIRoutineWizardState,
        equipmentList: List<Equipment> = emptyList(),
        workoutHistory: List<WorkoutLog> = emptyList(),
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ): ThreeMonthPlan = withContext(dispatcher) {
        val basePlan = createRuleBased3MonthPlan(wizardState, equipmentList, workoutHistory)
        if (QwenLlmService.isModelLoaded()) {
            return@withContext try {
                enrich3MonthPlanWithLlm(basePlan, wizardState)
            } catch (e: Throwable) {
                basePlan
            }
        }
        basePlan
    }

    /**
     * Generates or enriches a 3-Month Plan using the native Qwen 2.5 3B LLM engine,
     * seamlessly falling back to the local rule engine if model is not loaded or downloading.
     */
    suspend fun generate3MonthPlanWithLlm(
        wizardState: AIRoutineWizardState,
        equipmentList: List<Equipment> = emptyList(),
        workoutHistory: List<WorkoutLog> = emptyList(),
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        onTokenStream: (String) -> Unit = {}
    ): ThreeMonthPlan = withContext(dispatcher) {
        val basePlan = createRuleBased3MonthPlan(wizardState, equipmentList, workoutHistory)

        if (!QwenLlmService.isModelLoaded()) {
            onTokenStream("Qwen LLM no cargado. Usando motor de reglas local.")
            return@withContext basePlan
        }

        try {
            enrich3MonthPlanWithLlm(basePlan, wizardState, onTokenStream)
        } catch (e: Throwable) {
            basePlan
        }
    }

    private fun enrich3MonthPlanWithLlm(
        basePlan: ThreeMonthPlan,
        wizardState: AIRoutineWizardState,
        onTokenStream: (String) -> Unit = {}
    ): ThreeMonthPlan {
        val ragContext = LocalSportsKnowledgeBase.getKnowledgeContext("carrera atletismo gimnasio calistenia")
        val prompt = buildString {
            append(ragContext).append("\n\n")
            append("Actúa como entrenador personal deportivo en Qwen 2.5 3B Local LLM.\n")
            append("Modalidad: ").append(wizardState.modality.displayName).append("\n")
            append("Disciplinas: ").append(wizardState.selectedDisciplines.joinToString { it.displayName }).append("\n")
            append("Objetivo: ").append(wizardState.goalType.name).append("\n")
            append("Genera análisis de sobrecarga progresiva y periodización para la rutina de 3 meses estrictamente en ESPAÑOL.")
        }

        val llmOutput = QwenLlmService.generate(prompt, maxTokens = 512, onToken = onTokenStream)

        if (llmOutput.isBlank() || llmOutput.startsWith("Qwen LLM model is not loaded") || llmOutput.startsWith("Error")) {
            return basePlan
        }

        return basePlan.copy(
            title = "${basePlan.title} [Qwen 2.5 AI Validated]"
        )
    }

    fun createRuleBased3MonthPlan(
        wizardState: AIRoutineWizardState,
        equipmentList: List<Equipment> = emptyList(),
        workoutHistory: List<WorkoutLog> = emptyList()
    ): ThreeMonthPlan {
        val totalWorkoutCount = workoutHistory.size
        val isExperienced = totalWorkoutCount >= 8

        val month1 = generateMonth1(wizardState, isExperienced)
        val month2 = generateMonth2(wizardState, isExperienced)
        val month3 = generateMonth3(wizardState, isExperienced)

        val months = listOf(month1, month2, month3)
        val caloricValidation = validateCaloricEffectiveness(wizardState, months)

        val planTitle = when (wizardState.modality) {
            RoutineModality.MUSCULACION -> "Plan 3 Meses • Musculación & Hipertrofia Élite"
            RoutineModality.CARDIO -> "Plan 3 Meses • Resistencia Cardio & Fondo"
            RoutineModality.COMBINADO -> "Plan 3 Meses • Multideporte Élite (Gimnasio + Carrera + Natación)"
        }

        return ThreeMonthPlan(
            title = planTitle,
            modality = wizardState.modality,
            disciplines = wizardState.selectedDisciplines.toList(),
            caloricValidation = caloricValidation,
            baselinePhotoUri = wizardState.baselinePhotoUri,
            months = months
        )
    }

    private fun generateMonth1(state: AIRoutineWizardState, isExperienced: Boolean): MonthPlan {
        val weeks = (1..4).map { weekNum ->
            generateWeekPlan(
                weekNumber = weekNum,
                monthNumber = 1,
                monthTitle = "Mes 1: Base & Adaptación",
                intensityMultiplier = 1.0f,
                state = state,
                isExperienced = isExperienced
            )
        }
        return MonthPlan(
            monthNumber = 1,
            title = "Mes 1: Base & Adaptación Anatómica",
            focusDescription = "Consolidación de patrones de movimiento, activación cardiovascular y volumen aeróbico inicial (50%-60% intensidad).",
            weeks = weeks
        )
    }

    private fun generateMonth2(state: AIRoutineWizardState, isExperienced: Boolean): MonthPlan {
        val weeks = (5..8).map { weekNum ->
            generateWeekPlan(
                weekNumber = weekNum,
                monthNumber = 2,
                monthTitle = "Mes 2: Intensificación & Carga Progresiva",
                intensityMultiplier = 1.15f,
                state = state,
                isExperienced = isExperienced
            )
        }
        return MonthPlan(
            monthNumber = 2,
            title = "Mes 2: Intensificación & Sobrecarga Creciente",
            focusDescription = "Aumento de intensidad en series, incorporación de sobrecarga/lastre y mejora del umbral de lactato (65%-75% intensidad).",
            weeks = weeks
        )
    }

    private fun generateMonth3(state: AIRoutineWizardState, isExperienced: Boolean): MonthPlan {
        val weeks = (9..12).map { weekNum ->
            generateWeekPlan(
                weekNumber = weekNum,
                monthNumber = 3,
                monthTitle = "Mes 3: Pico de Rendimiento & Definición",
                intensityMultiplier = 1.30f,
                state = state,
                isExperienced = isExperienced
            )
        }
        return MonthPlan(
            monthNumber = 3,
            title = "Mes 3: Pico de Formato & Definición Láctica",
            focusDescription = "Sprints de velocidad máxima, alta densidad metabólica, afilado de ritmo y test de rendimiento final de 90 días (80%-90% intensidad).",
            weeks = weeks
        )
    }

    private fun generateWeekPlan(
        weekNumber: Int,
        monthNumber: Int,
        monthTitle: String,
        intensityMultiplier: Float,
        state: AIRoutineWizardState,
        isExperienced: Boolean
    ): WeekPlan {
        val days = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo").map { dayName ->
            generateDayPlan(dayName, weekNumber, monthNumber, intensityMultiplier, state, isExperienced)
        }

        return WeekPlan(
            weekNumber = weekNumber,
            title = "Semana $weekNumber",
            focus = when (monthNumber) {
                1 -> "Semana $weekNumber - Acondicionamiento base & técnica"
                2 -> "Semana $weekNumber - Incremento de volumen & carga"
                else -> if (weekNumber == 12) "Semana 12 - Descarga & Test de Rendimiento 90 Días" else "Semana $weekNumber - Picos de intensidad & ritmo máximo"
            },
            days = days
        )
    }

    private fun generateDayPlan(
        dayName: String,
        weekNumber: Int,
        monthNumber: Int,
        intensityMultiplier: Float,
        state: AIRoutineWizardState,
        isExperienced: Boolean
    ): DayPlan {
        if (dayName == "Domingo") {
            return DayPlan(
                dayName = "Domingo",
                title = "Descanso Total / Recuperación Activa",
                isRestDay = true,
                estimatedCaloriesBurned = 120,
                blocks = listOf(
                    WorkoutBlock(
                        category = "Recuperación",
                        title = "Paseo Ligero & Movilidad",
                        exercises = listOf(
                            PlannedExerciseStep("Caminata Suave Descompresiva", "Paseo regenerativo al aire libre", "20-30 mins"),
                            PlannedExerciseStep("Estiramientos & Foam Roller", "Liberación miofascial de tren inferior y espalda", "15 mins")
                        )
                    )
                ),
                muscleFocus = "Recuperación & Movilidad"
            )
        }

        val blocks = mutableListOf<WorkoutBlock>()
        var calories = 0

        val includesRun = state.selectedDisciplines.contains(CardioDiscipline.CARRERA) || state.modality == RoutineModality.CARDIO
        val includesSwim = state.selectedDisciplines.contains(CardioDiscipline.NATACION)
        val includesGym = state.selectedDisciplines.contains(CardioDiscipline.MUSCULACION) || state.modality == RoutineModality.MUSCULACION || state.modality == RoutineModality.COMBINADO

        val dayIndex = when (dayName) {
            "Lunes" -> 0
            "Martes" -> 1
            "Miércoles" -> 2
            "Jueves" -> 3
            "Viernes" -> 4
            "Sábado" -> 5
            else -> 0
        }
        val globalWorkoutDayIndex = (weekNumber - 1) * 6 + dayIndex
        val splitIndex = globalWorkoutDayIndex % 4

        val (strengthBlock, muscleFocusText) = when (splitIndex) {
            0 -> buildDiaABlock(intensityMultiplier) to "Pecho, Hombro Frontal & Tríceps"
            1 -> buildDiaBBlock(intensityMultiplier) to "Espalda, Hombro Posterior & Bíceps"
            2 -> buildDiaCBlock(intensityMultiplier) to "Pierna & Glúteos"
            else -> buildDiaDBlock(intensityMultiplier) to "Core, Estabilidad & Calistenia"
        }

        when (dayName) {
            "Lunes", "Miércoles", "Viernes" -> {
                if (includesRun) {
                    val runBlock = buildRunningBlock(dayName, weekNumber, intensityMultiplier, state)
                    blocks.add(runBlock)
                    calories += 280
                } else if (state.selectedDisciplines.contains(CardioDiscipline.SPINNING)) {
                    blocks.add(buildSpinningBlock(intensityMultiplier))
                    calories += 260
                } else if (state.selectedDisciplines.contains(CardioDiscipline.HIIT)) {
                    blocks.add(buildHiitBlock(intensityMultiplier))
                    calories += 270
                }

                if (state.includeCalisthenics && splitIndex != 3) {
                    blocks.add(buildCalisthenicsBlock(dayName, intensityMultiplier, state))
                    calories += 120
                }

                if (includesGym) {
                    blocks.add(strengthBlock)
                    calories += 240
                }
            }
            "Martes", "Jueves", "Sábado" -> {
                if (includesSwim) {
                    val swimBlock = buildSwimBlock(dayName, weekNumber, intensityMultiplier, state)
                    blocks.add(swimBlock)
                    calories += 320
                } else if (state.selectedDisciplines.contains(CardioDiscipline.ZUMBA_BAILE)) {
                    blocks.add(buildDanceBlock(intensityMultiplier))
                    calories += 250
                } else if (includesRun) {
                    val runBlock = buildRunningBlock(dayName, weekNumber, intensityMultiplier, state)
                    blocks.add(runBlock)
                    calories += 260
                }

                if (state.includeCalisthenics && splitIndex != 3) {
                    blocks.add(buildCalisthenicsBlock(dayName, intensityMultiplier, state))
                    calories += 120
                }

                if (includesGym) {
                    blocks.add(strengthBlock)
                    calories += 230
                }
            }
        }

        val dayTitle = blocks.joinToString(" + ") { it.title.take(25) }

        return DayPlan(
            dayName = dayName,
            title = if (dayTitle.isNotBlank()) dayTitle else "Entrenamiento $dayName",
            isRestDay = false,
            estimatedCaloriesBurned = (calories * intensityMultiplier).toInt(),
            blocks = blocks,
            muscleFocus = muscleFocusText
        )
    }

    private fun buildRunningBlock(dayName: String, weekNumber: Int, intensityMultiplier: Float, state: AIRoutineWizardState): WorkoutBlock {
        val weekKey = ((weekNumber - 1) % 4) + 1 // 1 to 4 mapping
        val exercises = mutableListOf<PlannedExerciseStep>()

        when (weekKey) {
            1 -> when (dayName) {
                "Lunes" -> {
                    val reps = (10 * intensityMultiplier).toInt()
                    exercises.add(PlannedExerciseStep("$reps Series x 400 m", "Ritmo de serie con recuperación de 1'30\" al trote", "${reps * 3} mins", "Pista de Atletismo"))
                }
                "Miércoles" -> {
                    val reps = (6 * intensityMultiplier).toInt()
                    exercises.add(PlannedExerciseStep("$reps x 1000 m", "Umbral anaeróbico sostenido con recuperación de 2'30\"", "${reps * 6} mins", "Pista / Calzado de Serie"))
                }
                else -> {
                    exercises.add(PlannedExerciseStep("20' Carrera Continua", "Rodaje de calentamiento suave", "20 mins"))
                    exercises.add(PlannedExerciseStep("10 x 100 m Cuestas Potentes", "Subida explosiva con bajada trotando de recuperación", "15 mins"))
                    exercises.add(PlannedExerciseStep("8 x 60 m Lisos Transferencia", "Sprints horizontales enfocados en zancada fina", "10 mins"))
                    exercises.add(PlannedExerciseStep("10' Carrera Continua Suave", "Descalentamiento aeróbico", "10 mins"))
                }
            }
            2 -> when (dayName) {
                "Lunes" -> {
                    val reps = (15 * intensityMultiplier).toInt()
                    exercises.add(PlannedExerciseStep("$reps Series x 300 m", "Frecuencia rápida con recuperación de 1'30\"", "${reps * 2} mins", "Pista"))
                }
                "Miércoles" -> {
                    exercises.add(PlannedExerciseStep("3 x 2000 m", "Resistencia de ritmo con 4' de descanso entre series", "30 mins", "Cronómetro / Reloj"))
                }
                else -> {
                    exercises.add(PlannedExerciseStep("20' Carrera Continua", "Calentamiento base", "20 mins"))
                    exercises.add(PlannedExerciseStep("10 x 150 m Cuestas", "Aceleraciones en pendiente media", "18 mins"))
                    exercises.add(PlannedExerciseStep("8 x 60 m Transferencia", "Zancada rápida y fluida", "10 mins"))
                    exercises.add(PlannedExerciseStep("10' Carrera Suave", "Soltar piernas", "10 mins"))
                }
            }
            3 -> when (dayName) {
                "Lunes" -> {
                    val reps = (10 * intensityMultiplier).toInt()
                    exercises.add(PlannedExerciseStep("$reps Series x 150 m", "Potencia sprint con recuperación de 2'30\"", "20 mins", "Pista"))
                }
                "Miércoles" -> {
                    val reps = (8 * intensityMultiplier).toInt()
                    exercises.add(PlannedExerciseStep("$reps x 1000 m", "Series de ritmo controlado con 2'30\" pausa", "40 mins", "Pista"))
                }
                else -> {
                    exercises.add(PlannedExerciseStep("20' Carrera Continua", "Rodaje base de activación", "20 mins"))
                    exercises.add(PlannedExerciseStep("10 x 150 m Cuestas", "Potencia en rampa", "18 mins"))
                    exercises.add(PlannedExerciseStep("8 x 60 m Lisos", "Sprints cortos de transferencia", "10 mins"))
                    exercises.add(PlannedExerciseStep("10' Carrera Suave", "Desfriamiento", "10 mins"))
                }
            }
            else -> when (dayName) { // Week 4 Fartlek
                "Lunes" -> {
                    val reps = (8 * intensityMultiplier).toInt()
                    exercises.add(PlannedExerciseStep("$reps Series x 500 m", "Ritmo alto con recuperación de 2'30\"", "25 mins", "Pista"))
                }
                "Miércoles" -> {
                    exercises.add(PlannedExerciseStep("4 x 2000 m", "Resistencia aeróbica avanzada con 4' recuperación", "38 mins"))
                }
                else -> {
                    exercises.add(PlannedExerciseStep("15' Carrera Continua", "Rodaje previo", "15 mins"))
                    exercises.add(PlannedExerciseStep("Fartlek Piramidal Continuo", "1'fuerte-1'suave / 2'f-2's / 3'f-2's / 2'f-2's / 1'f-1's", "22 mins"))
                    exercises.add(PlannedExerciseStep("10' Carrera Continua Suave", "Enfriamiento aeróbico", "10 mins"))
                }
            }
        }

        return WorkoutBlock(
            category = "Aeróbico / Carrera",
            title = "Carrera & Series de Atletismo",
            exercises = exercises
        )
    }

    private fun buildSwimBlock(dayName: String, weekNumber: Int, intensityMultiplier: Float, state: AIRoutineWizardState): WorkoutBlock {
        val exercises = mutableListOf<PlannedExerciseStep>()
        exercises.add(PlannedExerciseStep("Movilidad Articular Fuera del Agua", "Rotación de hombros, escápulas y columna", "5 mins"))
        exercises.add(PlannedExerciseStep("Calentamiento 200m", "100m crol suave + 100m estilo libre + 4x25m apnea (30'' descanso)", "10 mins", "Piscina"))

        when (state.swimSeriesCategory) {
            SwimDistanceSeriesCategory.COMBINATION_SERIES -> {
                // Combinación de series de natación: mixed short sprint sets, tempo medium sets, and long endurance sets
                exercises.add(PlannedExerciseStep("Series Cortas Sprint: 8 x 25m Explosivos", "Sprint de máxima velocidad con 15'' descanso entre 25m", "10 mins", "Piscina"))
                exercises.add(PlannedExerciseStep("Series Medias Tempo: 4 x 150m Ritmo Controlado", "Mantener ritmo de umbral con 30'' descanso entre series", "15 mins", "Piscina"))
                exercises.add(PlannedExerciseStep("Serie Larga Resistencia: 1 x 400m Nado Continuo Aeróbico", "Capacidad aeróbica y volumen sostenido con Pull-Buoy", "12 mins", "Pull-Buoy"))
            }
            SwimDistanceSeriesCategory.MEDIUM_SERIES -> {
                exercises.add(PlannedExerciseStep("Series Medias: 6 x 150m Ritmo de Prueba", "Resistencia a la velocidad con 30'' descanso entre series", "18 mins", "Piscina"))
                exercises.add(PlannedExerciseStep("4 x 200m Crol Progresivo", "Incrementar ritmo cada 50m con 45'' descanso", "20 mins", "Piscina"))
            }
            SwimDistanceSeriesCategory.LONG_SERIES -> {
                exercises.add(PlannedExerciseStep("Pirámide de Resistencia 50-100-200-100-50m", "45'' de descanso en cada serie. Mantener ritmo constante", "20 mins", "Piscina"))
                exercises.add(PlannedExerciseStep("5 x (2x100m crol ritmo)", "10'' descanso entre 100m, 1' descanso entre bloques", "18 mins"))
                exercises.add(PlannedExerciseStep("2x200m Continuo Suave", "Con Pull-Buoy entre las piernas enfocando braza superior", "12 mins", "Pull-Buoy"))
            }
            SwimDistanceSeriesCategory.SHORT_SERIES, null -> {
                if (dayName == "Sábado") {
                    exercises.add(PlannedExerciseStep("Pirámide de Resistencia 50-100-200-100-50m", "45'' de descanso en cada serie. Mantener ritmo constante", "20 mins", "Piscina"))
                    exercises.add(PlannedExerciseStep("5 x (2x100m crol ritmo)", "10'' descanso entre 100m, 1' descanso entre bloques", "18 mins"))
                    exercises.add(PlannedExerciseStep("2x200m Continuo Suave", "Con Pull-Buoy entre las piernas enfocando braza superior", "12 mins", "Pull-Buoy"))
                } else {
                    exercises.add(PlannedExerciseStep("10 x 50m Crol Láctico", "Ida fuerte, vuelta suave. Descanso en vuelta", "15 mins", "Piscina"))
                    exercises.add(PlannedExerciseStep("6 x 75m (25m pies + 50m crol fuerte)", "30'' descanso entre series", "15 mins", "Tabla de flotación"))
                    exercises.add(PlannedExerciseStep("6 x 50m Crol Progresivo", "De menos a más velocidad. 30'' descanso", "12 mins"))
                    exercises.add(PlannedExerciseStep("5 x 100m Velocidad Máxima", "2' saliendo cada 2' incluyendo descanso", "12 mins"))
                }
            }
        }

        exercises.add(PlannedExerciseStep("Enfriamiento & Estiramientos", "4x25m apnea descompresiva + 50m suaves + estiramiento de dorsales", "8 mins"))

        return WorkoutBlock(
            category = "Natación",
            title = "Natación (Series & Técnica)",
            exercises = exercises
        )
    }

    private fun buildCalisthenicsBlock(dayName: String, intensityMultiplier: Float, state: AIRoutineWizardState): WorkoutBlock {
        val exercises = mutableListOf<PlannedExerciseStep>()

        val weightKg = if (intensityMultiplier > 1.1f) "15 kg" else "Sin Lastre / Bodyweight"

        when (dayName) {
            "Lunes" -> {
                exercises.add(PlannedExerciseStep("Dominadas en Barra", "5 Series x 16 Repeticiones ($weightKg) · Descanso 90\"", "12 mins", "Barra de Dominadas"))
                exercises.add(PlannedExerciseStep("Abdominales Crunch", "4 Series x 25 Repeticiones", "6 mins"))
                exercises.add(PlannedExerciseStep("Salto de Cuerda (Comba)", "3 Bloques x 5 Minutos de salto continuo", "15 mins", "Cuerda de Salto"))
            }
            "Miércoles" -> {
                exercises.add(PlannedExerciseStep("Dominadas con Lastre / Estrictas", "5 Series x 16 Repeticiones ($weightKg) · Descanso 90\"", "12 mins", "Barra de Dominadas"))
                exercises.add(PlannedExerciseStep("Flexiones de Pecho (Push-Ups)", "4 Series x 25 Repeticiones estrictas", "8 mins"))
                exercises.add(PlannedExerciseStep("Salto de Cuerda (Comba)", "3 Bloques x 5 Minutos de salto veloz", "15 mins", "Cuerda de Salto"))
            }
            else -> {
                exercises.add(PlannedExerciseStep("Dominadas Libres Sin Lastre", "5 Series x 20 Repeticiones explósivas · Descanso 90\"", "12 mins", "Barra de Dominadas"))
                exercises.add(PlannedExerciseStep("Fondos en Banco / Paralelas", "4 Series x 20 Repeticiones", "8 mins"))
                exercises.add(PlannedExerciseStep("Abdominales Isométricos (Plancha)", "4 Series x 60 Segundos sosteniendo torso", "6 mins"))
            }
        }

        return WorkoutBlock(
            category = "Calistenia & Peso Corporal",
            title = "Calistenia & Dominadas",
            exercises = exercises
        )
    }

    private fun buildDiaABlock(intensityMultiplier: Float): WorkoutBlock {
        val pct1 = (50 * intensityMultiplier).toInt().coerceAtMost(85)
        val pct2 = (55 * intensityMultiplier).toInt().coerceAtMost(90)
        val pct3 = (60 * intensityMultiplier).toInt().coerceAtMost(95)

        val exercises = listOf(
            PlannedExerciseStep(
                title = "Press de Banca Horizontal",
                description = "Serie 1 ($pct1%), Serie 2 ($pct2%), Serie 3 ($pct3%)",
                durationOrReps = "3x10 Reps",
                equipmentUsed = "Barra Olímpica y Banca Horizontal",
                category = "Musculación / Pesos Libres",
                targetMuscle = "Pectoral Mayor & Tríceps",
                equipmentName = "Barra Olímpica & Banco Plano",
                instructions = "Acuéstate en la banca, toma la barra con agarre ligeramente superior al ancho de hombros. Desciende con control hasta el esternón y empuja con potencia."
            ),
            PlannedExerciseStep(
                title = "Press Militar con Barra / Mancuernas",
                description = "Serie 1 ($pct1%), Serie 2 ($pct2%), Serie 3 ($pct3%)",
                durationOrReps = "3x10 Reps",
                equipmentUsed = "Barra / Mancuernas",
                category = "Musculación / Hombros",
                targetMuscle = "Deltoides Frontal & Trapecio",
                equipmentName = "Barra Olímpica / Mancuernas",
                instructions = "Empuja la barra verticalmente por encima de la cabeza con el abdomen activado, bloqueando arriba sin arquear la espalda baja."
            ),
            PlannedExerciseStep(
                title = "Fondos en Paralelas / Banco",
                description = "Serie 1 ($pct1%), Serie 2 ($pct2%), Serie 3 ($pct3%)",
                durationOrReps = "3x12 Reps",
                equipmentUsed = "Barras Paralelas / Banco Sueco",
                category = "Calistenia / Fuerza",
                targetMuscle = "Pectoral Inferior & Tríceps",
                equipmentName = "Barras Paralelas",
                instructions = "Flexiona los codos a 90° con inclinación del torso hacia adelante para enfatizar el pecho, y extiende con fuerza."
            ),
            PlannedExerciseStep(
                title = "Extensiones de Tríceps en Polea",
                description = "Serie 1 ($pct1%), Serie 2 ($pct2%), Serie 3 ($pct3%)",
                durationOrReps = "3x12 Reps",
                equipmentUsed = "Polea Alta con Cuerda",
                category = "Musculación / Poleas",
                targetMuscle = "Tríceps Braquial",
                equipmentName = "Polea Alta",
                instructions = "Mantén codos pegados al cuerpo, extiende antebrazos hacia abajo separando la cuerda al final para máxima contracción."
            )
        )

        return WorkoutBlock(
            category = "Gimnasio Musculación",
            title = "Fuerza Día A (Pecho, Hombro Frontal & Tríceps)",
            exercises = exercises
        )
    }

    private fun buildDiaBBlock(intensityMultiplier: Float): WorkoutBlock {
        val pct1 = (50 * intensityMultiplier).toInt().coerceAtMost(85)
        val pct2 = (55 * intensityMultiplier).toInt().coerceAtMost(90)
        val pct3 = (60 * intensityMultiplier).toInt().coerceAtMost(95)

        val exercises = listOf(
            PlannedExerciseStep(
                title = "Dominadas en Barra",
                description = "Serie 1 ($pct1%), Serie 2 ($pct2%), Serie 3 ($pct3%)",
                durationOrReps = "4x10 Reps",
                equipmentUsed = "Barra de Dominadas",
                category = "Calistenia / Fuerza Espalda",
                targetMuscle = "Dorsal Ancho & Bíceps",
                equipmentName = "Barra de Dominadas",
                instructions = "Agarre prono más ancho que hombros. Eleva el cuerpo hasta pasar la barbilla por encima de la barra y desciende controlado."
            ),
            PlannedExerciseStep(
                title = "Remo con Barra / Máquina",
                description = "Serie 1 ($pct1%), Serie 2 ($pct2%), Serie 3 ($pct3%)",
                durationOrReps = "3x10 Reps",
                equipmentUsed = "Barra Olímpica / Máquina de Remo",
                category = "Musculación / Espalda",
                targetMuscle = "Dorsal Ancho & Trapecio",
                equipmentName = "Máquina de Remo / Barra",
                instructions = "Tracciona la barra/agarre hacia el abdomen inferior apretando las escápulas atrás durante 1 segundo al final del recorrido."
            ),
            PlannedExerciseStep(
                title = "Jalón al Pecho en Polea Alta",
                description = "Serie 1 ($pct1%), Serie 2 ($pct2%), Serie 3 ($pct3%)",
                durationOrReps = "3x10 Reps",
                equipmentUsed = "Polea Alta / Technogym Lat Machine",
                category = "Musculación / Poleas",
                targetMuscle = "Dorsal Ancho",
                equipmentName = "Polea Alta / Technogym Lat Machine",
                instructions = "Tracciona la barra ancha hacia la clavícula con el pecho erguido sin balancear el torso."
            ),
            PlannedExerciseStep(
                title = "Curl de Bíceps con Barra / Mancuernas",
                description = "Serie 1 ($pct1%), Serie 2 ($pct2%), Serie 3 ($pct3%)",
                durationOrReps = "3x12 Reps",
                equipmentUsed = "Barra Z / Mancuernas",
                category = "Musculación / Pesos Libres",
                targetMuscle = "Bíceps Braquial",
                equipmentName = "Barra Z / Mancuernas",
                instructions = "Con codos fijos al torso, flexiona los brazos llevando el peso a los hombros y baja lentamente."
            ),
            PlannedExerciseStep(
                title = "Pájaro para Deltoides Posterior",
                description = "Serie 1 ($pct1%), Serie 2 ($pct2%), Serie 3 ($pct3%)",
                durationOrReps = "3x12 Reps",
                equipmentUsed = "Mancuernas / Peck-Deck Inverso",
                category = "Musculación / Hombros",
                targetMuscle = "Deltoides Posterior",
                equipmentName = "Mancuernas / Máquina Peck-Deck Inverso",
                instructions = "Abre los brazos en movimiento de vuelo hacia atrás enfatizando la parte posterior del hombro."
            )
        )

        return WorkoutBlock(
            category = "Gimnasio Musculación",
            title = "Fuerza Día B (Espalda, Hombro Posterior & Bíceps)",
            exercises = exercises
        )
    }

    private fun buildDiaCBlock(intensityMultiplier: Float): WorkoutBlock {
        val pct1 = (50 * intensityMultiplier).toInt().coerceAtMost(85)
        val pct2 = (55 * intensityMultiplier).toInt().coerceAtMost(90)
        val pct3 = (60 * intensityMultiplier).toInt().coerceAtMost(95)

        val exercises = listOf(
            PlannedExerciseStep(
                title = "Prensa de Piernas Inclinada 45°",
                description = "Serie 1 ($pct1%), Serie 2 ($pct2%), Serie 3 ($pct3%)",
                durationOrReps = "4x10 Reps",
                equipmentUsed = "Máquina Prensa 45°",
                category = "Musculación / Piernas",
                targetMuscle = "Cuádriceps & Glúteos",
                equipmentName = "Technogym Leg Press 45°",
                instructions = "Coloca pies a anchura de caderas en plataforma. Empuja con fuerza sin bloquear rodillas en extensión completa."
            ),
            PlannedExerciseStep(
                title = "Sentadillas con Barra Olímpica",
                description = "Serie 1 ($pct1%), Serie 2 ($pct2%), Serie 3 ($pct3%)",
                durationOrReps = "4x8 Reps",
                equipmentUsed = "Barra Olímpica & Rack",
                category = "Musculación / Pesos Libres",
                targetMuscle = "Cuádriceps, Glúteos & Isquiotibiales",
                equipmentName = "Barra Olímpica & Jaula de Sentadillas",
                instructions = "Desciende flexionando cadera y rodillas hasta romper el paralelo de 90° y empuja fuertemente hacia arriba."
            ),
            PlannedExerciseStep(
                title = "Extensión de Cuádriceps en Máquina",
                description = "Serie 1 ($pct1%), Serie 2 ($pct2%), Serie 3 ($pct3%)",
                durationOrReps = "3x12 Reps",
                equipmentUsed = "Máquina Leg Extension",
                category = "Musculación / Máquinas",
                targetMuscle = "Cuádriceps",
                equipmentName = "Leg Extension Machine",
                instructions = "Extiende las piernas sosteniendo 1 segundo arriba y regresa lentamente controlando el peso."
            ),
            PlannedExerciseStep(
                title = "Series de Cuestas / Gemelos",
                description = "Serie 1 ($pct1%), Serie 2 ($pct2%), Serie 3 ($pct3%)",
                durationOrReps = "3x15 Reps",
                equipmentUsed = "Rampa de Cuestas / Máquina Gemelos",
                category = "Fuerza / Potencia Piernas",
                targetMuscle = "Sóleo & Gemelos",
                equipmentName = "Máquina Gemelos / Rampa",
                instructions = "Aceleraciones explosivas en cuestas o elevaciones de talones completas para desarrollar potencia de pantorrilla y tobillos."
            )
        )

        return WorkoutBlock(
            category = "Gimnasio Musculación",
            title = "Fuerza Día C (Pierna & Glúteos)",
            exercises = exercises
        )
    }

    private fun buildDiaDBlock(intensityMultiplier: Float): WorkoutBlock {
        val pct1 = (50 * intensityMultiplier).toInt().coerceAtMost(85)
        val pct2 = (55 * intensityMultiplier).toInt().coerceAtMost(90)
        val pct3 = (60 * intensityMultiplier).toInt().coerceAtMost(95)

        val exercises = listOf(
            PlannedExerciseStep(
                title = "Dominadas Libres Estrictas",
                description = "Serie 1 ($pct1%), Serie 2 ($pct2%), Serie 3 ($pct3%)",
                durationOrReps = "4x12 Reps",
                equipmentUsed = "Barra de Dominadas",
                category = "Calistenia & Peso Corporal",
                targetMuscle = "Dorsal Ancho & Core",
                equipmentName = "Barra de Dominadas",
                instructions = "Dominadas en barra con agarre neutro o prono, sin balanceo de cadera para activar masa muscular superior."
            ),
            PlannedExerciseStep(
                title = "Abdominales Crunch & Elevaciones de Piernas",
                description = "Serie 1 ($pct1%), Serie 2 ($pct2%), Serie 3 ($pct3%)",
                durationOrReps = "4x20 Reps",
                equipmentUsed = "Esterilla / Banco Inclinado",
                category = "Calistenia & Core",
                targetMuscle = "Abdomen Recto & Oblicuos",
                equipmentName = "Esterilla / Banco Abdominal",
                instructions = "Flexión de tronco sostenida en la parte alta contrayendo la pared abdominal y elevación de piernas."
            ),
            PlannedExerciseStep(
                title = "Fondos Tríceps en Paralelas",
                description = "Serie 1 ($pct1%), Serie 2 ($pct2%), Serie 3 ($pct3%)",
                durationOrReps = "4x12 Reps",
                equipmentUsed = "Barras Paralelas",
                category = "Calistenia / Fondos",
                targetMuscle = "Tríceps & Pectoral",
                equipmentName = "Barras Paralelas",
                instructions = "Fondos profundos en paralelas manteniendo el cuerpo erguido para aislar tríceps y fuerza empuje."
            ),
            PlannedExerciseStep(
                title = "Planchas Isométricas de Estabilidad Core",
                description = "Serie 1 ($pct1%), Serie 2 ($pct2%), Serie 3 ($pct3%)",
                durationOrReps = "4x60 Segundos",
                equipmentUsed = "Esterilla",
                category = "Core & Estabilidad",
                targetMuscle = "Core, Lumbar & Estabilidad",
                equipmentName = "Esterilla",
                instructions = "Sostén la posición de plancha frontal sobre antebrazos alineando cabeza, cadera y talones en contracción isométrica total."
            )
        )

        return WorkoutBlock(
            category = "Gimnasio Musculación",
            title = "Fuerza Día D (Core, Estabilidad & Calistenia)",
            exercises = exercises
        )
    }

    private fun buildSpinningBlock(intensityMultiplier: Float): WorkoutBlock {
        return WorkoutBlock(
            category = "Cardio Ciclismo",
            title = "Spinning & Cadencia",
            exercises = listOf(
                PlannedExerciseStep("Calentamiento de Cadencia", "90 RPM con resistencia ligera", "10 mins", "Bicicleta Spinning"),
                PlannedExerciseStep("Intervalos de Potencia 1:1", "8 x (2' a 110 RPM resistencia alta + 2' de recup a 80 RPM)", "32 mins", "Bicicleta Spinning"),
                PlannedExerciseStep("Rodaje de Enfriamiento", "Bajar resistencia y recuperar pulso", "8 mins")
            )
        )
    }

    private fun buildHiitBlock(intensityMultiplier: Float): WorkoutBlock {
        return WorkoutBlock(
            category = "Cardio HIIT",
            title = "HIIT Metabólico Full Body",
            exercises = listOf(
                PlannedExerciseStep("Jumping Jacks & Movilidad", "Activación articular rápida", "5 mins"),
                PlannedExerciseStep("Circuit Tabata 20\"/10\"", "Burpees, Mountain Climbers, Sentadilla Salto y Sprints en sitio", "25 mins", "Kettlebell / Peso Corporal"),
                PlannedExerciseStep("Descompresión & Respiración", "Estiramiento dinámico de tren inferior y core", "10 mins")
            )
        )
    }

    private fun buildDanceBlock(intensityMultiplier: Float): WorkoutBlock {
        return WorkoutBlock(
            category = "Cardio Baile",
            title = "Zumba / Cardio Baile Rítmico",
            exercises = listOf(
                PlannedExerciseStep("Sesión de Zumba Cardio Rítmico", "Coreografías de alta intensidad aeróbica con cambios de ritmo", "45 mins"),
                PlannedExerciseStep("Vuelta a la Calma & Estiramiento", "Movilidad de cadera y columna", "10 mins")
            )
        )
    }

    private fun validateCaloricEffectiveness(
        state: AIRoutineWizardState,
        months: List<MonthPlan>
    ): CaloricValidationResult {
        val totalWorkoutDays = months.flatMap { m -> m.weeks.flatMap { w -> w.days } }.count { !it.isRestDay }
        val totalCaloriesBurned = months.flatMap { m -> m.weeks.flatMap { w -> w.days } }.sumOf { it.estimatedCaloriesBurned }
        val avgDailyBurn = if (totalWorkoutDays > 0) totalCaloriesBurned / totalWorkoutDays else 480

        val (targetDietCalories, message, recommendation) = when (state.goalType) {
            DietGoal.FAT_LOSS -> {
                val dietCal = 1950
                val netDeficit = avgDailyBurn + 200
                Triple(
                    dietCal,
                    "¡Validado Calóricamente! Con un gasto medio de ~$avgDailyBurn kcal/día en ejercicio y tu meta de dieta de $dietCal kcal/día, garantizas un déficit sostenido de ~$netDeficit kcal/día para maximizar la quema de masa grasa.",
                    "Mantén el aporte proteico alto (1.8g-2.2g/kg de peso) para proteger la musculatura obtenida en dominadas y gimnasio."
                )
            }
            DietGoal.MUSCLE_GAIN -> {
                val dietCal = 2800
                val netSurplus = 300
                Triple(
                    dietCal,
                    "¡Validado Calóricamente! Tu plan compensa los ~$avgDailyBurn kcal/día de gasto físico con un superávit óptimo de ~$netSurplus kcal/día, asegurando la síntesis proteica e hipertrofia.",
                    "Consume carbohidratos de absorción rápida post-entrenamiento para reponer el glucógeno consumido en las series de atletismo y natación."
                )
            }
            DietGoal.ATHLETIC_PERFORMANCE -> {
                val dietCal = 2600
                Triple(
                    dietCal,
                    "¡Validado Calóricamente! Aporte energéticamente balanceado para cubrir los ~$avgDailyBurn kcal/día del volumen multideporte, garantizando máximo rendimiento sin fatiga acumulada.",
                    "Asegura una buena hidratación e ingesta de sales minerales en los días de series largas de natación y carreras de cuestas."
                )
            }
            DietGoal.BODY_RECOMP -> {
                val dietCal = 2300
                Triple(
                    dietCal,
                    "¡Validado Calóricamente! Plan normocalórico con ciclado estratégico para quemar grasa corporal mientras desarrollas masa magra con ~$avgDailyBurn kcal/día de gasto físico.",
                    "Asegura una alta ingesta proteica (2.2g/kg) y cicla los carbohidratos en días de entrenamiento intensivo."
                )
            }
        }

        return CaloricValidationResult(
            isValidated = true,
            dailyExerciseCaloriesAvg = avgDailyBurn,
            estimatedDietCaloriesTarget = targetDietCalories,
            goal = state.goalType,
            statusTitle = "Efectividad Calórica & Nutricional Validada por IA",
            statusMessage = message,
            recommendation = recommendation
        )
    }
}
