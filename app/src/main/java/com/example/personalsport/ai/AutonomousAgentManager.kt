package com.example.personalsport.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.personalsport.data.local.dao.BodyMetricDao
import com.example.personalsport.data.local.dao.WorkoutLogDao
import com.example.personalsport.data.repository.UserProfileRepository
import com.example.personalsport.diet.BodyProfile
import com.example.personalsport.diet.DietGoal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlin.math.abs

data class AgentBodyState(
    val weightKg: Float = 75.0f,
    val bodyFatPercentage: Float = 15.5f,
    val muscleMassKg: Float = 44.5f,
    val activeCaloriesBurned: Int = 350,
    val lastSyncTimestamp: Long = 0L,
    val lastRenewalTimestamp: Long = 0L,
    val baselineWeightKg: Float = 75.0f,
    val baselineBodyFatPercentage: Float = 15.5f,
    val baselineMuscleMassKg: Float = 44.5f,
    val isRenewalRequired: Boolean = false,
    val lastRenewalReason: String? = null,
    val active3MonthPlanTitle: String? = null,
    val active30DayDietPlanTitle: String? = null
)

data class AgentSyncResult(
    val success: Boolean,
    val isPlanRenewed: Boolean,
    val renewalReason: String?,
    val bodyState: AgentBodyState,
    val message: String
)

/**
 * Agente Autónomo Inteligente encargado de la supervisión contínua de la progresión física del atleta.
 *
 * Funciones clave:
 * - Evalúa desviaciones significativas en el porcentaje de grasa, peso o masa muscular tras cada sincronización de dispositivos.
 * - Trigger automático para renovación o ajuste de planes de entrenamiento y dietas cuando se detectan mesetas o cambios biomecánicos.
 */
class AutonomousAgentManager(
    private val context: Context? = null,
    private val bodyMetricDao: BodyMetricDao? = null,
    private val workoutLogDao: WorkoutLogDao? = null,
    private val userProfileRepository: UserProfileRepository? = null,
    private val prefs: SharedPreferences? = try {
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    } catch (_: Exception) {
        null
    }
) {
    companion object {
        private const val PREFS_NAME = "autonomous_agent_prefs"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val KEY_LAST_RENEWAL = "last_renewal_timestamp"
        private const val KEY_BASELINE_WEIGHT = "baseline_weight_kg"
        private const val KEY_BASELINE_FAT = "baseline_fat_pct"
        private const val KEY_BASELINE_MUSCLE = "baseline_muscle_kg"
        private const val KEY_LAST_REASON = "last_renewal_reason"

        const val NINETY_DAYS_MS = 90L * 24 * 60 * 60 * 1000L // 90 days in millis
        const val SIGNIFICANT_WEIGHT_SHIFT_KG = 2.0f // > 2kg shift
    }

    private val _agentState = MutableStateFlow(loadInitialState())
    val agentState: StateFlow<AgentBodyState> = _agentState.asStateFlow()

    var active3MonthPlan: ThreeMonthPlan? = null
        private set

    var active30DayDietPlan: ThirtyDayDietPlan? = null
        private set

    /**
     * Evaluates whether auto-renewal is needed based on time (90 days) or metric shifts (>2kg weight shift).
     */
    fun evaluateRenewalNeeded(
        currentWeightKg: Float,
        baselineWeightKg: Float,
        lastRenewalTimeMs: Long,
        currentTimeMs: Long,
        hasExistingPlan: Boolean
    ): Pair<Boolean, String?> {
        if (!hasExistingPlan) {
            return Pair(true, "Inicialización de Plan de 3 Meses & Dieta Flexible por Agente Autónomo")
        }

        val timeElapsedMs = currentTimeMs - lastRenewalTimeMs
        if (timeElapsedMs >= NINETY_DAYS_MS) {
            return Pair(true, "Renovación Trimestral Automática (90 días transcurridos desde el último plan)")
        }

        val weightShift = abs(currentWeightKg - baselineWeightKg)
        if (weightShift >= SIGNIFICANT_WEIGHT_SHIFT_KG) {
            return Pair(
                true,
                "Renovación Automática por Cambio Significativo de Peso (${"%.1f".format(weightShift)} kg de variación respecto a la base de ${"%.1f".format(baselineWeightKg)} kg)"
            )
        }

        return Pair(false, null)
    }

    /**
     * Called on every device sync (Huawei Watch GT, Scales, Amazfit, Xiaomi, Garmin, Fitbit, etc.).
     * Automatically updates user body profile state and recalculates 3-Month Plan & Diets if required.
     */
    suspend fun onDeviceSyncTriggered(currentTimeMs: Long = System.currentTimeMillis()): AgentSyncResult {
        // 1. Fetch latest body metrics from DAO / Repository
        val latestMetric = try {
            bodyMetricDao?.getAllBodyMetrics()?.first()?.maxByOrNull { it.date }
        } catch (_: Exception) {
            null
        }

        val currentWeight = latestMetric?.weightKg ?: _agentState.value.weightKg
        val currentFatPct = latestMetric?.bodyFatPercentage ?: _agentState.value.bodyFatPercentage
        val estimatedMuscleMass = (currentWeight * (1.0f - (currentFatPct / 100f)) * 0.72f)

        // 2. Fetch recent workouts to estimate active calories burned
        val recentLogs = try {
            workoutLogDao?.getAllWorkoutLogs()?.first() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val calculatedActiveCalories = if (recentLogs.isNotEmpty()) {
            val avgMinutes = recentLogs.take(5).map { it.durationMinutes }.average()
            (avgMinutes * 7.5f).toInt().coerceIn(200, 1200)
        } else {
            350
        }

        val hasPlan = active3MonthPlan != null && active30DayDietPlan != null
        val currentState = _agentState.value

        // 3. Evaluate renewal condition
        val (needsRenewal, reason) = evaluateRenewalNeeded(
            currentWeightKg = currentWeight,
            baselineWeightKg = currentState.baselineWeightKg,
            lastRenewalTimeMs = currentState.lastRenewalTimestamp,
            currentTimeMs = currentTimeMs,
            hasExistingPlan = hasPlan
        )

        var planRenewed = false
        var updatedBaselineWeight = currentState.baselineWeightKg
        var updatedBaselineFat = currentState.baselineBodyFatPercentage
        var updatedBaselineMuscle = currentState.baselineMuscleMassKg
        var updatedRenewalTime = currentState.lastRenewalTimestamp
        var updatedReason = currentState.lastRenewalReason

        // 4. Perform auto-renewal if triggered
        if (needsRenewal) {
            val goal = if (currentWeight > currentState.baselineWeightKg && currentFatPct > 18f) {
                DietGoal.FAT_LOSS
            } else if (currentWeight < currentState.baselineWeightKg && currentFatPct < 12f) {
                DietGoal.MUSCLE_GAIN
            } else {
                DietGoal.BODY_RECOMP
            }

            val wizardState = AIRoutineWizardState(
                userWeightKg = currentWeight,
                goalType = goal
            )

            // Recalculate 3-Month Plan
            active3MonthPlan = LocalAIRoutineGenerator.generate3MonthPlan(wizardState)

            // Recalculate 30-Day Flexible Diet Plan & Recipes
            val dietBodyProfile = BodyProfile(
                weightKg = currentWeight,
                bodyFatPercentage = currentFatPct,
                workoutActiveCalories = calculatedActiveCalories
            )
            active30DayDietPlan = LocalAIDietEngine.generate30DayPlan(
                profile = dietBodyProfile,
                goal = goal
            )

            planRenewed = true
            updatedBaselineWeight = currentWeight
            updatedBaselineFat = currentFatPct
            updatedBaselineMuscle = estimatedMuscleMass
            updatedRenewalTime = currentTimeMs
            updatedReason = reason
        }

        // 5. Update state & persist
        val newState = AgentBodyState(
            weightKg = currentWeight,
            bodyFatPercentage = currentFatPct,
            muscleMassKg = estimatedMuscleMass,
            activeCaloriesBurned = calculatedActiveCalories,
            lastSyncTimestamp = currentTimeMs,
            lastRenewalTimestamp = updatedRenewalTime,
            baselineWeightKg = updatedBaselineWeight,
            baselineBodyFatPercentage = updatedBaselineFat,
            baselineMuscleMassKg = updatedBaselineMuscle,
            isRenewalRequired = false,
            lastRenewalReason = updatedReason,
            active3MonthPlanTitle = active3MonthPlan?.title,
            active30DayDietPlanTitle = active30DayDietPlan?.let { "Dieta Flexible 30 Días (${it.goal.title})" }
        )

        _agentState.value = newState
        saveStateToPrefs(newState)

        val message = if (planRenewed) {
            "⚡ Agente Autónomo: ¡Perfil actualizado y Planes de 3 Meses y Dieta de 30 Días renovados con éxito! Motivo: $updatedReason"
        } else {
            "⚡ Agente Autónomo: Estado de perfil actualizado (${currentWeight} kg, ${currentFatPct}% grasa, ${calculatedActiveCalories} kcal activas). Planes vigentes alineados 100% con tu progreso."
        }

        return AgentSyncResult(
            success = true,
            isPlanRenewed = planRenewed,
            renewalReason = updatedReason,
            bodyState = newState,
            message = message
        )
    }

    /**
     * Force immediate recalculation of 3-Month Plan and 30-Day Diet Plan.
     */
    suspend fun forceRecalculatePlanAndDiet(reason: String = "Renovación manual solicitada"): AgentSyncResult {
        val currentState = _agentState.value
        val now = System.currentTimeMillis()

        val wizardState = AIRoutineWizardState(
            userWeightKg = currentState.weightKg,
            goalType = DietGoal.BODY_RECOMP
        )
        active3MonthPlan = LocalAIRoutineGenerator.generate3MonthPlan(wizardState)

        val dietBodyProfile = BodyProfile(
            weightKg = currentState.weightKg,
            bodyFatPercentage = currentState.bodyFatPercentage,
            workoutActiveCalories = currentState.activeCaloriesBurned
        )
        active30DayDietPlan = LocalAIDietEngine.generate30DayPlan(
            profile = dietBodyProfile,
            goal = DietGoal.BODY_RECOMP
        )

        val newState = currentState.copy(
            lastSyncTimestamp = now,
            lastRenewalTimestamp = now,
            baselineWeightKg = currentState.weightKg,
            baselineBodyFatPercentage = currentState.bodyFatPercentage,
            baselineMuscleMassKg = currentState.muscleMassKg,
            lastRenewalReason = reason,
            active3MonthPlanTitle = active3MonthPlan?.title,
            active30DayDietPlanTitle = active30DayDietPlan?.let { "Dieta Flexible 30 Días (${it.goal.title})" }
        )

        _agentState.value = newState
        saveStateToPrefs(newState)

        return AgentSyncResult(
            success = true,
            isPlanRenewed = true,
            renewalReason = reason,
            bodyState = newState,
            message = "Renovación forzada completada: 3-Month Plan y Dieta de 30 Días actualizados."
        )
    }

    private fun loadInitialState(): AgentBodyState {
        val prefs = this.prefs ?: return AgentBodyState()
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L)
        val lastRenewal = prefs.getLong(KEY_LAST_RENEWAL, 0L)
        val baseWeight = prefs.getFloat(KEY_BASELINE_WEIGHT, 75.0f)
        val baseFat = prefs.getFloat(KEY_BASELINE_FAT, 15.5f)
        val baseMuscle = prefs.getFloat(KEY_BASELINE_MUSCLE, 44.5f)
        val lastReason = prefs.getString(KEY_LAST_REASON, null)

        return AgentBodyState(
            weightKg = baseWeight,
            bodyFatPercentage = baseFat,
            muscleMassKg = baseMuscle,
            lastSyncTimestamp = lastSync,
            lastRenewalTimestamp = lastRenewal,
            baselineWeightKg = baseWeight,
            baselineBodyFatPercentage = baseFat,
            baselineMuscleMassKg = baseMuscle,
            lastRenewalReason = lastReason
        )
    }

    private fun saveStateToPrefs(state: AgentBodyState) {
        prefs?.edit {
            putLong(KEY_LAST_SYNC, state.lastSyncTimestamp)
            putLong(KEY_LAST_RENEWAL, state.lastRenewalTimestamp)
            putFloat(KEY_BASELINE_WEIGHT, state.baselineWeightKg)
            putFloat(KEY_BASELINE_FAT, state.baselineBodyFatPercentage)
            putFloat(KEY_BASELINE_MUSCLE, state.baselineMuscleMassKg)
            putString(KEY_LAST_REASON, state.lastRenewalReason)
        }
    }
}
