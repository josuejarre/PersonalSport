package com.example.personalsport.ui.planner

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.personalsport.ai.AthleticsSeriesType
import com.example.personalsport.ai.DayPlan
import com.example.personalsport.ai.LocalAIFitnessEngine
import com.example.personalsport.ai.ThreeMonthPlan
import com.example.personalsport.data.local.entity.Equipment
import com.example.personalsport.data.local.entity.WorkoutLog
import com.example.personalsport.data.model.ExerciseItem
import com.example.personalsport.data.repository.CompletedExerciseRepository
import com.example.personalsport.data.repository.EquipmentRepository
import com.example.personalsport.data.repository.ProgressPhotoRepository
import com.example.personalsport.data.repository.WorkoutLogRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SportType(val displayName: String, val category: String) {
    ATHLETICS_SERIES("Atletismo / Series de Carrera", "Athletics"),
    RUNNING_SHORT("Running - Short (3k-5k)", "Running"),
    RUNNING_MEDIUM("Running - Medium (8k-12k)", "Running"),
    RUNNING_LONG("Running - Long (15k+)", "Running"),
    SWIM("Swim (Pool & Drills)", "Swim"),
    STRENGTH("Strength & Gym Routine", "Strength")
}

@Immutable
data class PlannedExerciseStep(
    val title: String,
    val description: String,
    val durationOrReps: String,
    val equipmentUsed: String? = null,
    val id: String = title.lowercase().trim().replace(Regex("[^a-z0-9]"), "_"),
    val category: String = "Fuerza & Acondicionamiento",
    val targetMuscle: String = "Cuerpo Completo",
    val equipmentName: String = equipmentUsed ?: "Peso Corporal",
    val imageUrl: String? = null,
    val exerciseIllustration: String? = null,
    val instructions: String = "Ejecuta el ejercicio manteniendo alineación biomecánica adecuada, rango de movimiento completo y control respiratorio en cada fase.",
    val isCompleted: Boolean = false
) {
    fun toExerciseItem(): ExerciseItem = ExerciseItem(
        id = id,
        title = title,
        description = description,
        category = category,
        targetMuscle = targetMuscle,
        equipmentName = equipmentName,
        imageUrl = imageUrl,
        exerciseIllustration = exerciseIllustration,
        instructions = instructions,
        durationOrReps = durationOrReps,
        isCompleted = isCompleted
    )
}

@Immutable
data class GeneratedRoutine(
    val title: String,
    val sportType: SportType,
    val targetDurationMinutes: Int,
    val intensity: String,
    val warmup: List<PlannedExerciseStep>,
    val mainSet: List<PlannedExerciseStep>,
    val cooldown: List<PlannedExerciseStep>,
    val notes: String
)

@Immutable
data class PlannerUiState(
    val availableEquipment: List<Equipment> = emptyList(),
    val selectedSportType: SportType = SportType.ATHLETICS_SERIES,
    val selectedAthleticsType: AthleticsSeriesType = AthleticsSeriesType.INTERVALS_400M,
    val targetDurationMinutes: Int = 45,
    val selectedIntensity: String = "Moderate",
    val generatedRoutine: GeneratedRoutine? = null,
    val active3MonthPlan: ThreeMonthPlan? = null,
    val selectedWeekNumber: Int = 1,
    val isWizardOpen: Boolean = false,
    val isSavedToLog: Boolean = false,
    val completedExerciseIds: Set<String> = emptySet()
)

private data class RoutineParams(
    val sport: SportType,
    val athleticsType: AthleticsSeriesType,
    val duration: Int,
    val intensity: String,
    val isSaved: Boolean,
    val active3MonthPlan: ThreeMonthPlan?,
    val selectedWeekNumber: Int,
    val isWizardOpen: Boolean
)

class PlannerViewModel(
    equipmentRepository: EquipmentRepository,
    private val workoutLogRepository: WorkoutLogRepository,
    val progressPhotoRepository: ProgressPhotoRepository? = null,
    private val completedExerciseRepository: CompletedExerciseRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _selectedSportType = MutableStateFlow(SportType.ATHLETICS_SERIES)
    private val _selectedAthleticsType = MutableStateFlow(AthleticsSeriesType.INTERVALS_400M)
    private val _targetDurationMinutes = MutableStateFlow(45)
    private val _selectedIntensity = MutableStateFlow("Moderate")
    private val _isSavedToLog = MutableStateFlow(false)

    private val _active3MonthPlan = MutableStateFlow<ThreeMonthPlan?>(null)
    private val _selectedWeekNumber = MutableStateFlow(1)
    private val _isWizardOpen = MutableStateFlow(false)
    private val _inMemoryCompletedIds = MutableStateFlow<Set<String>>(emptySet())

    private val completedIdsFlow = completedExerciseRepository?.completedExercises
        ?.map { list -> list.map { it.exerciseId }.toSet() }
        ?: _inMemoryCompletedIds

    private val _params = combine(
        _selectedSportType,
        _selectedAthleticsType,
        _targetDurationMinutes,
        _selectedIntensity,
        _isSavedToLog,
        _active3MonthPlan,
        _selectedWeekNumber,
        _isWizardOpen
    ) { args: Array<Any?> ->
        RoutineParams(
            sport = args[0] as SportType,
            athleticsType = args[1] as AthleticsSeriesType,
            duration = args[2] as Int,
            intensity = args[3] as String,
            isSaved = args[4] as Boolean,
            active3MonthPlan = args[5] as ThreeMonthPlan?,
            selectedWeekNumber = args[6] as Int,
            isWizardOpen = args[7] as Boolean
        )
    }

    val uiState: StateFlow<PlannerUiState> = combine(
        equipmentRepository.allEquipment,
        workoutLogRepository.allWorkoutLogs,
        completedIdsFlow,
        _params
    ) { equipment, logs, completedIds, params ->
        val rawRoutine = LocalAIFitnessEngine.generateWorkoutRoutine(
            sportType = params.sport,
            athleticsSeriesType = params.athleticsType,
            targetDurationMinutes = params.duration,
            intensity = params.intensity,
            equipmentList = equipment,
            workoutHistory = logs
        )

        val updatedRoutine = rawRoutine.copy(
            warmup = rawRoutine.warmup.map { it.copy(isCompleted = completedIds.contains(it.id)) },
            mainSet = rawRoutine.mainSet.map { it.copy(isCompleted = completedIds.contains(it.id)) },
            cooldown = rawRoutine.cooldown.map { it.copy(isCompleted = completedIds.contains(it.id)) }
        )

        PlannerUiState(
            availableEquipment = equipment,
            selectedSportType = params.sport,
            selectedAthleticsType = params.athleticsType,
            targetDurationMinutes = params.duration,
            selectedIntensity = params.intensity,
            generatedRoutine = updatedRoutine,
            active3MonthPlan = params.active3MonthPlan,
            selectedWeekNumber = params.selectedWeekNumber,
            isWizardOpen = params.isWizardOpen,
            isSavedToLog = params.isSaved,
            completedExerciseIds = completedIds
        )
    }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlannerUiState()
        )

    fun toggleExerciseCompletion(step: PlannedExerciseStep) {
        viewModelScope.launch(ioDispatcher) {
            val isCurrentlyCompleted = uiState.value.completedExerciseIds.contains(step.id)
            if (completedExerciseRepository != null) {
                completedExerciseRepository.toggleExerciseCompletion(
                    exerciseId = step.id,
                    exerciseTitle = step.title,
                    currentStatus = isCurrentlyCompleted
                )
            } else {
                val newSet = _inMemoryCompletedIds.value.toMutableSet()
                if (isCurrentlyCompleted) newSet.remove(step.id) else newSet.add(step.id)
                _inMemoryCompletedIds.value = newSet
            }
        }
    }

    fun selectSportType(sportType: SportType) {
        _selectedSportType.value = sportType
        _isSavedToLog.value = false
    }

    fun selectAthleticsType(athleticsSeriesType: AthleticsSeriesType) {
        _selectedAthleticsType.value = athleticsSeriesType
        _isSavedToLog.value = false
    }

    fun selectDuration(durationMinutes: Int) {
        _targetDurationMinutes.value = durationMinutes
        _isSavedToLog.value = false
    }

    fun selectIntensity(intensity: String) {
        _selectedIntensity.value = intensity
        _isSavedToLog.value = false
    }

    fun openWizard() {
        _isWizardOpen.value = true
    }

    fun closeWizard() {
        _isWizardOpen.value = false
    }

    fun set3MonthPlan(plan: ThreeMonthPlan) {
        _active3MonthPlan.value = plan
        _selectedWeekNumber.value = 1
        _isWizardOpen.value = false
    }

    fun selectWeekNumber(weekNum: Int) {
        if (weekNum in 1..12) {
            _selectedWeekNumber.value = weekNum
        }
    }

    fun saveDayToLog(dayPlan: DayPlan) {
        viewModelScope.launch(ioDispatcher) {
            val totalBlocks = dayPlan.blocks.size
            workoutLogRepository.insert(
                WorkoutLog(
                    date = System.currentTimeMillis(),
                    workoutName = "${dayPlan.dayName}: ${dayPlan.title}",
                    durationMinutes = 60,
                    notes = "Plan 3 Meses • $totalBlocks bloques. Est. ~${dayPlan.estimatedCaloriesBurned} kcal quemadas.",
                    source = "Plan 3 Meses (IA)"
                )
            )
            _isSavedToLog.value = true
        }
    }

    fun saveRoutineToLog(routine: GeneratedRoutine) {
        viewModelScope.launch(ioDispatcher) {
            val totalSteps = routine.mainSet.size
            workoutLogRepository.insert(
                WorkoutLog(
                    date = System.currentTimeMillis(),
                    workoutName = routine.title,
                    durationMinutes = routine.targetDurationMinutes,
                    notes = "${routine.sportType.displayName} (${routine.intensity}) · $totalSteps main exercises. ${routine.notes}",
                    source = "Planner (AI)"
                )
            )
            _isSavedToLog.value = true
        }
    }
}

class PlannerViewModelFactory(
    private val equipmentRepository: EquipmentRepository,
    private val workoutLogRepository: WorkoutLogRepository,
    private val progressPhotoRepository: ProgressPhotoRepository? = null,
    private val completedExerciseRepository: CompletedExerciseRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlannerViewModel(
                equipmentRepository,
                workoutLogRepository,
                progressPhotoRepository,
                completedExerciseRepository,
                ioDispatcher
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
