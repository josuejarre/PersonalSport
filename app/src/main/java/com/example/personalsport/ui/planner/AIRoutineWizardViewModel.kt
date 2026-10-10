package com.example.personalsport.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.personalsport.ai.AIRoutineWizardState
import com.example.personalsport.ai.AthleticsSeriesCategory
import com.example.personalsport.ai.CardioDiscipline
import com.example.personalsport.ai.LocalAIRoutineGenerator
import com.example.personalsport.ai.RoutineModality
import com.example.personalsport.ai.RunningSubType
import com.example.personalsport.ai.SwimDistanceSeriesCategory
import com.example.personalsport.ai.ThreeMonthPlan
import com.example.personalsport.data.local.entity.Equipment
import com.example.personalsport.data.local.entity.ProgressPhoto
import com.example.personalsport.data.local.entity.WorkoutLog
import com.example.personalsport.data.repository.EquipmentRepository
import com.example.personalsport.data.repository.ProgressPhotoRepository
import com.example.personalsport.data.repository.WorkoutLogRepository
import com.example.personalsport.diet.DietGoal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AIRoutineWizardUiState(
    val currentStep: Int = 1,
    val totalSteps: Int = 4,
    val wizardConfig: AIRoutineWizardState = AIRoutineWizardState(),
    val availableEquipment: List<Equipment> = emptyList(),
    val workoutHistory: List<WorkoutLog> = emptyList(),
    val generatedPlan: ThreeMonthPlan? = null,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null
)

class AIRoutineWizardViewModel(
    equipmentRepository: EquipmentRepository,
    workoutLogRepository: WorkoutLogRepository,
    private val progressPhotoRepository: ProgressPhotoRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val planGenerator: suspend (AIRoutineWizardState, List<Equipment>, List<WorkoutLog>, CoroutineDispatcher) -> ThreeMonthPlan =
        { config, equipment, logs, dispatcher ->
            LocalAIRoutineGenerator.generate3MonthPlan(config, equipment, logs, dispatcher)
        }
) : ViewModel() {

    private val _currentStep = MutableStateFlow(1)
    private val _wizardConfig = MutableStateFlow(AIRoutineWizardState())
    private val _generatedPlan = MutableStateFlow<ThreeMonthPlan?>(null)
    private val _isGenerating = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private data class InternalState(
        val step: Int,
        val config: AIRoutineWizardState,
        val plan: ThreeMonthPlan?,
        val generating: Boolean,
        val error: String?
    )

    private val _internalState = combine(
        _currentStep,
        _wizardConfig,
        _generatedPlan,
        _isGenerating,
        _errorMessage
    ) { step, config, plan, generating, error ->
        InternalState(step, config, plan, generating, error)
    }

    val uiState: StateFlow<AIRoutineWizardUiState> = combine(
        _internalState,
        equipmentRepository.allEquipment,
        workoutLogRepository.allWorkoutLogs
    ) { internal, equipment, logs ->
        AIRoutineWizardUiState(
            currentStep = internal.step,
            totalSteps = 4,
            wizardConfig = internal.config,
            availableEquipment = equipment,
            workoutHistory = logs,
            generatedPlan = internal.plan,
            isGenerating = internal.generating,
            errorMessage = internal.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AIRoutineWizardUiState()
    )

    fun nextStep() {
        if (_currentStep.value < 4) {
            _currentStep.value += 1
        }
    }

    fun previousStep() {
        if (_currentStep.value > 1) {
            _currentStep.value -= 1
        }
    }

    fun goToStep(step: Int) {
        if (step in 1..4) {
            _currentStep.value = step
        }
    }

    fun setModality(modality: RoutineModality) {
        _wizardConfig.value = _wizardConfig.value.copy(
            modality = modality,
            selectedDisciplines = when (modality) {
                RoutineModality.MUSCULACION -> setOf(CardioDiscipline.MUSCULACION)
                RoutineModality.CARDIO -> setOf(CardioDiscipline.CARRERA)
                RoutineModality.COMBINADO -> setOf(CardioDiscipline.MUSCULACION, CardioDiscipline.NATACION, CardioDiscipline.CARRERA)
            }
        )
    }

    fun toggleDiscipline(discipline: CardioDiscipline) {
        val current = _wizardConfig.value.selectedDisciplines.toMutableSet()
        if (current.contains(discipline)) {
            if (current.size > 1) {
                current.remove(discipline)
            }
        } else {
            if (current.size < 3) { // Allow up to 3 combined disciplines
                current.add(discipline)
            }
        }
        _wizardConfig.value = _wizardConfig.value.copy(selectedDisciplines = current)
    }

    fun setRunningSubType(subType: RunningSubType) {
        _wizardConfig.value = _wizardConfig.value.copy(runningSubType = subType)
    }

    fun setAthleticsSeriesCategory(category: AthleticsSeriesCategory) {
        _wizardConfig.value = _wizardConfig.value.copy(athleticsSeriesCategory = category)
    }

    fun setSwimSeriesCategory(category: SwimDistanceSeriesCategory) {
        _wizardConfig.value = _wizardConfig.value.copy(swimSeriesCategory = category)
    }

    fun toggleCalisthenics(include: Boolean) {
        _wizardConfig.value = _wizardConfig.value.copy(includeCalisthenics = include)
    }

    fun toggleCalisthenicsExercise(exercise: String) {
        val current = _wizardConfig.value.selectedCalisthenicsExercises.toMutableSet()
        if (current.contains(exercise)) {
            current.remove(exercise)
        } else {
            current.add(exercise)
        }
        _wizardConfig.value = _wizardConfig.value.copy(selectedCalisthenicsExercises = current)
    }

    fun setBaselinePhotoUri(photoUri: String?) {
        _wizardConfig.value = _wizardConfig.value.copy(
            baselinePhotoUri = photoUri,
            photoCapturedTimestamp = if (photoUri != null) System.currentTimeMillis() else null
        )
    }

    fun setGoalType(goal: DietGoal) {
        _wizardConfig.value = _wizardConfig.value.copy(goalType = goal)
    }

    fun generatePlan(onSuccess: (ThreeMonthPlan) -> Unit) {
        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            try {
                val state = uiState.value
                val plan = withContext(defaultDispatcher) {
                    try {
                        planGenerator(
                            state.wizardConfig,
                            state.availableEquipment,
                            state.workoutHistory,
                            defaultDispatcher
                        )
                    } catch (e: Throwable) {
                        LocalAIRoutineGenerator.createRuleBased3MonthPlan(
                            state.wizardConfig,
                            state.availableEquipment,
                            state.workoutHistory
                        )
                    }
                }

                // Save baseline photo to repository if taken
                state.wizardConfig.baselinePhotoUri?.let { uri ->
                    withContext(ioDispatcher) {
                        progressPhotoRepository.insert(
                            ProgressPhoto(
                                date = System.currentTimeMillis(),
                                photoUri = uri,
                                notes = "Foto Inicial Wizard (Meta 3 Meses / 90 Días)"
                            )
                        )
                    }
                }

                _generatedPlan.value = plan
                _isGenerating.value = false
                onSuccess(plan)
            } catch (e: Exception) {
                _isGenerating.value = false
                _errorMessage.value = e.localizedMessage ?: "Error al generar la rutina de 3 meses."
            }
        }
    }
}

class AIRoutineWizardViewModelFactory(
    private val equipmentRepository: EquipmentRepository,
    private val workoutLogRepository: WorkoutLogRepository,
    private val progressPhotoRepository: ProgressPhotoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AIRoutineWizardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AIRoutineWizardViewModel(equipmentRepository, workoutLogRepository, progressPhotoRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
