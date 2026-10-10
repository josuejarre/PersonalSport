package com.example.personalsport.ui.diet

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.personalsport.ai.DishAlternative
import com.example.personalsport.ai.LocalAIDietEngine
import com.example.personalsport.ai.MealSlotType
import com.example.personalsport.ai.ProjectedDayPlan
import com.example.personalsport.ai.ThirtyDayDietPlan
import com.example.personalsport.data.repository.BodyMetricRepository
import com.example.personalsport.data.repository.WorkoutLogRepository
import com.example.personalsport.diet.ActivityLevel
import com.example.personalsport.diet.BodyProfile
import com.example.personalsport.diet.DietEngine
import com.example.personalsport.diet.DietGoal
import com.example.personalsport.diet.DietSupplementSyncManager
import com.example.personalsport.diet.Gender
import com.example.personalsport.diet.GroceryCategory
import com.example.personalsport.diet.MacroTarget
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class DietProfileState(
    val goal: DietGoal = DietGoal.BODY_RECOMP,
    val weightKg: Float = 75f,
    val heightCm: Float = 175f,
    val age: Int = 26,
    val gender: Gender = Gender.MALE,
    val bodyFatPercentage: Float? = 15f,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val workoutActiveCalories: Int = 350
)

@Immutable
data class DietUiViewState(
    val selectedDayIndex: Int = 0,
    val activeSlotAlternativeIndices: Map<MealSlotType, Int> = emptyMap(),
    val showGroceryDialog: Boolean = false,
    val isCustomizingMetrics: Boolean = false,
    val selectedSlotForAlternatives: MealSlotType? = null,
    val selectedDishForRecipeDetails: DishAlternative? = null
)

@Immutable
data class DietUiState(
    val profile: BodyProfile = BodyProfile(),
    val goal: DietGoal = DietGoal.BODY_RECOMP,
    val bmr: Int = 1800,
    val tdee: Int = 2600,
    val targetMacros: MacroTarget = MacroTarget(calories = 2080, proteinGrams = 172, carbsGrams = 195, fatGrams = 65),
    val thirtyDayPlan: ThirtyDayDietPlan? = null,
    val selectedDayIndex: Int = 0,
    val currentDayPlan: ProjectedDayPlan? = null,
    val activeSlotAlternativeIndices: Map<MealSlotType, Int> = emptyMap(),
    val groceryList: List<GroceryCategory> = emptyList(),
    val showGroceryDialog: Boolean = false,
    val isCustomizingMetrics: Boolean = false,
    val selectedSlotForAlternatives: MealSlotType? = null,
    val selectedDishForRecipeDetails: DishAlternative? = null
)

class DietViewModel(
    bodyMetricRepository: BodyMetricRepository? = null,
    workoutLogRepository: WorkoutLogRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _profileState = MutableStateFlow(DietProfileState())
    private val _viewState = MutableStateFlow(DietUiViewState())

    init {
        bodyMetricRepository?.allBodyMetrics?.let { metricsFlow ->
            viewModelScope.launch(ioDispatcher) {
                metricsFlow.collect { metrics ->
                    metrics.maxByOrNull { it.date }?.let { latest ->
                        _profileState.value = _profileState.value.copy(
                            weightKg = latest.weightKg,
                            bodyFatPercentage = latest.bodyFatPercentage ?: _profileState.value.bodyFatPercentage
                        )
                    }
                }
            }
        }

        workoutLogRepository?.allWorkoutLogs?.let { logsFlow ->
            viewModelScope.launch(ioDispatcher) {
                logsFlow.collect { logs ->
                    if (logs.isNotEmpty()) {
                        val recent = logs.take(3)
                        val avgMinutes = recent.map { it.durationMinutes }.average()
                        if (avgMinutes > 0) {
                            val estimatedCals = (avgMinutes * 7.5f).toInt().coerceIn(150, 1000)
                            _profileState.value = _profileState.value.copy(workoutActiveCalories = estimatedCals)
                        }
                    }
                }
            }
        }
    }

    val uiState: StateFlow<DietUiState> = combine(
        _profileState,
        _viewState
    ) { profileState, viewState ->
        val profile = BodyProfile(
            weightKg = profileState.weightKg,
            heightCm = profileState.heightCm,
            age = profileState.age,
            gender = profileState.gender,
            bodyFatPercentage = profileState.bodyFatPercentage,
            activityLevel = profileState.activityLevel,
            workoutActiveCalories = profileState.workoutActiveCalories
        )

        val bmr = DietEngine.calculateBmr(profile)
        val tdee = DietEngine.calculateTdee(profile)
        val macros = DietEngine.calculateTargetMacros(profile, profileState.goal)

        val thirtyDayPlan = LocalAIDietEngine.generate30DayPlan(profile, profileState.goal)

        DietSupplementSyncManager.updateSyncState(
            goal = profileState.goal,
            supplements = thirtyDayPlan.recommendedSupplements
        )

        val safeDayIdx = viewState.selectedDayIndex.coerceIn(0, thirtyDayPlan.days.lastIndex)
        val rawDayPlan = thirtyDayPlan.days[safeDayIdx]

        val customDaySlots = rawDayPlan.mealSlots.map { slot ->
            val customIndex = viewState.activeSlotAlternativeIndices[slot.slotType] ?: slot.activeAlternativeIndex
            slot.copy(activeAlternativeIndex = customIndex)
        }
        val currentDayPlan = rawDayPlan.copy(mealSlots = customDaySlots)

        val currentLegacyPlan = DietEngine.generateDailyMealPlan(profile, profileState.goal)
        val groceryList = DietEngine.generateGroceryList(currentLegacyPlan)

        DietUiState(
            profile = profile,
            goal = profileState.goal,
            bmr = bmr,
            tdee = tdee,
            targetMacros = macros,
            thirtyDayPlan = thirtyDayPlan,
            selectedDayIndex = safeDayIdx,
            currentDayPlan = currentDayPlan,
            activeSlotAlternativeIndices = viewState.activeSlotAlternativeIndices,
            groceryList = groceryList,
            showGroceryDialog = viewState.showGroceryDialog,
            isCustomizingMetrics = viewState.isCustomizingMetrics,
            selectedSlotForAlternatives = viewState.selectedSlotForAlternatives,
            selectedDishForRecipeDetails = viewState.selectedDishForRecipeDetails
        )
    }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DietUiState()
        )

    fun selectGoal(goal: DietGoal) {
        _profileState.value = _profileState.value.copy(goal = goal)
    }

    fun selectDay(dayIndex: Int) {
        _viewState.value = _viewState.value.copy(selectedDayIndex = dayIndex.coerceIn(0, 29))
    }

    fun selectAlternativeDish(slotType: MealSlotType, alternativeIndex: Int) {
        val current = _viewState.value.activeSlotAlternativeIndices.toMutableMap()
        current[slotType] = alternativeIndex
        _viewState.value = _viewState.value.copy(activeSlotAlternativeIndices = current)
    }

    fun openAlternativesDialog(slotType: MealSlotType) {
        _viewState.value = _viewState.value.copy(selectedSlotForAlternatives = slotType)
    }

    fun closeAlternativesDialog() {
        _viewState.value = _viewState.value.copy(selectedSlotForAlternatives = null)
    }

    fun openRecipeDetails(dish: DishAlternative) {
        _viewState.value = _viewState.value.copy(selectedDishForRecipeDetails = dish)
    }

    fun closeRecipeDetails() {
        _viewState.value = _viewState.value.copy(selectedDishForRecipeDetails = null)
    }

    fun updateWeight(weightKg: Float) {
        _profileState.value = _profileState.value.copy(weightKg = weightKg.coerceIn(30f, 250f))
    }

    fun updateHeight(heightCm: Float) {
        _profileState.value = _profileState.value.copy(heightCm = heightCm.coerceIn(100f, 230f))
    }

    fun updateAge(age: Int) {
        _profileState.value = _profileState.value.copy(age = age.coerceIn(14, 100))
    }

    fun updateGender(gender: Gender) {
        _profileState.value = _profileState.value.copy(gender = gender)
    }

    fun updateBodyFat(bodyFatPercentage: Float?) {
        _profileState.value = _profileState.value.copy(bodyFatPercentage = bodyFatPercentage?.coerceIn(3f, 60f))
    }

    fun updateActivityLevel(activityLevel: ActivityLevel) {
        _profileState.value = _profileState.value.copy(activityLevel = activityLevel)
    }

    fun updateWorkoutActiveCalories(calories: Int) {
        _profileState.value = _profileState.value.copy(workoutActiveCalories = calories.coerceIn(0, 3000))
    }

    fun setGroceryDialogVisible(visible: Boolean) {
        _viewState.value = _viewState.value.copy(showGroceryDialog = visible)
    }

    fun toggleCustomizingMetrics() {
        _viewState.value = _viewState.value.copy(isCustomizingMetrics = !_viewState.value.isCustomizingMetrics)
    }
}

class DietViewModelFactory(
    private val bodyMetricRepository: BodyMetricRepository? = null,
    private val workoutLogRepository: WorkoutLogRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DietViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DietViewModel(bodyMetricRepository, workoutLogRepository, ioDispatcher) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
