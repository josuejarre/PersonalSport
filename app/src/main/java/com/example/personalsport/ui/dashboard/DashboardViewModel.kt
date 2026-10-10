package com.example.personalsport.ui.dashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.personalsport.ai.AIRecoveryCountdown
import com.example.personalsport.ai.AiProgressInsight
import com.example.personalsport.ai.LocalAIFitnessEngine
import com.example.personalsport.ai.LocalAISleepAnalyzer
import com.example.personalsport.ai.RecoveryCountdownResult
import com.example.personalsport.ai.RecoveryInput
import com.example.personalsport.ai.SleepAnalysisInput
import com.example.personalsport.ai.SleepAnalysisResult
import com.example.personalsport.data.local.entity.BodyMetric
import com.example.personalsport.data.local.entity.WorkoutLog
import com.example.personalsport.data.repository.BodyMetricRepository
import com.example.personalsport.data.repository.WorkoutLogRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Estado inmutable de la interfaz de usuario del Panel Principal (Dashboard).
 */
@Immutable
data class DashboardUiState(
    val latestWeight: Float? = null,
    val latestBodyFat: Float? = null,
    val muscleMassKg: Float? = null,
    val visceralFatLevel: Int? = null,
    val boneMassKg: Float? = null,
    val bodyWaterPercentage: Float? = null,
    val bmi: Float? = null,
    val sleepQuality: String? = null,
    val aiInsight: AiProgressInsight? = null,
    val postWorkoutRecommendation: String? = null,
    val latestWorkoutLog: WorkoutLog? = null,
    val recentWorkouts: List<WorkoutLog> = emptyList(),
    val totalWorkoutsCount: Int = 0,
    val totalDurationMinutes: Int = 0,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val sleepAnalysis: SleepAnalysisResult? = null,
    val recoveryCountdown: RecoveryCountdownResult? = null
)

/**
 * ViewModel encargado de procesar y consolidar la información del Panel Principal (Dashboard).
 */
class DashboardViewModel(
    private val bodyMetricRepository: BodyMetricRepository,
    private val workoutLogRepository: WorkoutLogRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>(null)

    private val _currentTimeFlow = flow {
        emit(System.currentTimeMillis())
        while (currentCoroutineContext().isActive) {
            currentCoroutineContext().ensureActive()
            delay(10000L)
            emit(System.currentTimeMillis())
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        bodyMetricRepository.allBodyMetrics,
        workoutLogRepository.allWorkoutLogs,
        _isSyncing,
        _syncMessage,
        _currentTimeFlow
    ) { metrics, logs, isSyncing, syncMsg, currentTime ->
        val latestMetric = metrics.maxByOrNull { it.date }
        val aiInsight = LocalAIFitnessEngine.analyzeBodyMetrics(metrics)

        val latestWorkout = logs.maxByOrNull { it.date }
        val postWorkoutRecommendation = if (latestWorkout != null) {
            LocalAIFitnessEngine.generatePostWorkoutRecommendation(latestWorkout)
        } else {
            null
        }

        val sleepAnalysis = if (logs.isNotEmpty()) {
            val sleepInput = SleepAnalysisInput(
                hoursSlept = 7.75f,
                deepSleepPercentage = 22.0f,
                remSleepPercentage = 20.0f,
                restingHeartRateBpm = 54,
                dinnerTimingHoursBeforeBed = 3.0f,
                hasLateCaffeine = false,
                hasMagnesiumZincIntake = true,
                hydrationLiters = 2.5f,
                workoutIntensity = "ALTA",
                athleteAge = 28,
                unloggedBackgroundSteps = 10400,
                unloggedActiveMovements = "Paseos urbanos, caminata rápida y movilidad espontánea"
            )
            LocalAISleepAnalyzer.analyzeSleep(sleepInput)
        } else {
            null
        }

        val recoveryCountdown = if (latestWorkout != null) {
            val recoveryInput = RecoveryInput(
                lastWorkoutTimestampMillis = latestWorkout.date,
                workoutIntensity = "ALTA",
                workoutDurationMinutes = logs.firstOrNull()?.durationMinutes ?: 60,
                unloggedBackgroundSteps = 10400,
                unloggedNeatMinutes = 45,
                sleepScore = sleepAnalysis?.sleepScore ?: 85,
                restingHeartRateBpm = sleepAnalysis?.restingHeartRateBpm ?: 54,
                athleteAge = 28,
                currentTimeMillis = currentTime
            )
            AIRecoveryCountdown.calculateRecovery(recoveryInput)
        } else {
            null
        }

        val w = latestMetric?.weightKg
        val fat = latestMetric?.bodyFatPercentage
        val muscle = if (w != null && fat != null) (w * (1.0f - (fat / 100f)) * 0.72f) else null
        val visceral = if (latestMetric != null) 4 else null
        val bone = if (w != null) (w * 0.043f) else null
        val water = if (fat != null) ((100f - fat) * 0.73f) else if (w != null) 60.0f else null
        val bmiVal = if (w != null) (w / (1.78f * 1.78f)) else null
        val sleepQual = sleepAnalysis?.let { "${it.sleepScore}% (${it.qualityBadge.displayName})" }

        DashboardUiState(
            latestWeight = w,
            latestBodyFat = fat,
            muscleMassKg = muscle,
            visceralFatLevel = visceral,
            boneMassKg = bone,
            bodyWaterPercentage = water,
            bmi = bmiVal,
            sleepQuality = sleepQual,
            aiInsight = aiInsight,
            postWorkoutRecommendation = postWorkoutRecommendation,
            latestWorkoutLog = latestWorkout,
            recentWorkouts = logs.take(10),
            totalWorkoutsCount = logs.size,
            totalDurationMinutes = logs.sumOf { it.durationMinutes },
            isSyncing = isSyncing,
            syncMessage = syncMsg,
            sleepAnalysis = sleepAnalysis,
            recoveryCountdown = recoveryCountdown
        )
    }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState()
        )

    fun addQuickBodyMetric(weightKg: Float, bodyFatPercentage: Float?) {
        viewModelScope.launch(ioDispatcher) {
            bodyMetricRepository.insert(
                BodyMetric(
                    date = System.currentTimeMillis(),
                    weightKg = weightKg,
                    bodyFatPercentage = bodyFatPercentage
                )
            )
            _syncMessage.value = "Body metrics updated successfully"
        }
    }

    fun setSyncing(syncing: Boolean) {
        _isSyncing.value = syncing
    }

    fun setSyncMessage(msg: String?) {
        _syncMessage.value = msg
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    public override fun onCleared() {
        super.onCleared()
    }
}

class DashboardViewModelFactory(
    private val bodyMetricRepository: BodyMetricRepository,
    private val workoutLogRepository: WorkoutLogRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(bodyMetricRepository, workoutLogRepository, ioDispatcher) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
