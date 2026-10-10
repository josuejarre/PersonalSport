package com.example.personalsport.ai

import com.example.personalsport.data.local.dao.BodyMetricDao
import com.example.personalsport.data.local.dao.WorkoutLogDao
import com.example.personalsport.data.local.entity.BodyMetric
import com.example.personalsport.data.local.entity.WorkoutLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AgentFakeBodyMetricDao : BodyMetricDao {
    val metrics = mutableListOf<BodyMetric>()

    override fun getAllBodyMetrics(): Flow<List<BodyMetric>> = flowOf(metrics)

    override suspend fun getMetricByDate(date: Long): BodyMetric? {
        return metrics.find { it.date == date }
    }

    override suspend fun insertBodyMetric(bodyMetric: BodyMetric) {
        val idx = metrics.indexOfFirst { it.date == bodyMetric.date }
        if (idx >= 0) metrics[idx] = bodyMetric else metrics.add(bodyMetric)
    }

    override suspend fun updateBodyMetric(bodyMetric: BodyMetric) {
        val idx = metrics.indexOfFirst { it.id == bodyMetric.id }
        if (idx >= 0) metrics[idx] = bodyMetric
    }

    override suspend fun deleteBodyMetric(bodyMetric: BodyMetric) {
        metrics.removeIf { it.id == bodyMetric.id }
    }
}

class AgentFakeWorkoutLogDao : WorkoutLogDao {
    val logs = mutableListOf<WorkoutLog>()

    override fun getAllWorkoutLogs(): Flow<List<WorkoutLog>> = flowOf(logs)

    override suspend fun getLogByExternalId(externalId: String): WorkoutLog? {
        return logs.find { it.externalId == externalId }
    }

    override suspend fun insertWorkoutLog(workoutLog: WorkoutLog) {
        val idx = logs.indexOfFirst { it.externalId == workoutLog.externalId && !it.externalId.isNullOrBlank() }
        if (idx >= 0) logs[idx] = workoutLog else logs.add(workoutLog)
    }

    override suspend fun updateWorkoutLog(workoutLog: WorkoutLog) {
        val idx = logs.indexOfFirst { it.id == workoutLog.id }
        if (idx >= 0) logs[idx] = workoutLog
    }

    override suspend fun deleteWorkoutLog(workoutLog: WorkoutLog) {
        logs.removeIf { it.id == workoutLog.id }
    }
}

class AutonomousAgentManagerTest {

    private lateinit var fakeBodyMetricDao: AgentFakeBodyMetricDao
    private lateinit var fakeWorkoutLogDao: AgentFakeWorkoutLogDao
    private lateinit var agentManager: AutonomousAgentManager

    @Before
    fun setUp() {
        fakeBodyMetricDao = AgentFakeBodyMetricDao()
        fakeWorkoutLogDao = AgentFakeWorkoutLogDao()
        agentManager = AutonomousAgentManager(
            bodyMetricDao = fakeBodyMetricDao,
            workoutLogDao = fakeWorkoutLogDao
        )
    }

    @Test
    fun testInitialStateDefaults() {
        val state = agentManager.agentState.value
        assertEquals(75.0f, state.weightKg, 0.01f)
        assertEquals(15.5f, state.bodyFatPercentage, 0.01f)
        assertFalse(state.isRenewalRequired)
    }

    @Test
    fun testEvaluateRenewalNeededNoPlanFirstTime() {
        val (needsRenewal, reason) = agentManager.evaluateRenewalNeeded(
            currentWeightKg = 75.0f,
            baselineWeightKg = 75.0f,
            lastRenewalTimeMs = System.currentTimeMillis() - 1000L,
            currentTimeMs = System.currentTimeMillis(),
            hasExistingPlan = false
        )

        assertTrue(needsRenewal)
        assertNotNull(reason)
        assertTrue(reason?.contains("Inicialización") == true)
    }

    @Test
    fun testEvaluateRenewalNeeded90DaysElapsed() {
        val ninetyDaysAgo = System.currentTimeMillis() - AutonomousAgentManager.NINETY_DAYS_MS - 10000L
        val (needsRenewal, reason) = agentManager.evaluateRenewalNeeded(
            currentWeightKg = 75.0f,
            baselineWeightKg = 75.0f,
            lastRenewalTimeMs = ninetyDaysAgo,
            currentTimeMs = System.currentTimeMillis(),
            hasExistingPlan = true
        )

        assertTrue(needsRenewal)
        assertNotNull(reason)
        assertTrue(reason?.contains("Renovación Trimestral Automática") == true)
    }

    @Test
    fun testEvaluateRenewalNeededSignificantWeightShift() {
        val now = System.currentTimeMillis()
        val (needsRenewal, reason) = agentManager.evaluateRenewalNeeded(
            currentWeightKg = 77.5f, // 2.5 kg weight gain (>2kg)
            baselineWeightKg = 75.0f,
            lastRenewalTimeMs = now - 86400000L,
            currentTimeMs = now,
            hasExistingPlan = true
        )

        assertTrue(needsRenewal)
        assertNotNull(reason)
        assertTrue(reason?.contains("Cambio Significativo de Peso") == true)
    }

    @Test
    fun testEvaluateRenewalNeededSmallWeightShiftNoRenewal() {
        val now = System.currentTimeMillis()
        val (needsRenewal, reason) = agentManager.evaluateRenewalNeeded(
            currentWeightKg = 75.8f, // 0.8 kg weight change (<2kg)
            baselineWeightKg = 75.0f,
            lastRenewalTimeMs = now - 86400000L,
            currentTimeMs = now,
            hasExistingPlan = true
        )

        assertFalse(needsRenewal)
        assertEquals(null, reason)
    }

    @Test
    fun testOnDeviceSyncTriggersPlanAndDietRenewal() = runTest {
        val now = System.currentTimeMillis()
        fakeBodyMetricDao.insertBodyMetric(
            BodyMetric(date = now, weightKg = 78.0f, bodyFatPercentage = 16.0f)
        )
        fakeWorkoutLogDao.insertWorkoutLog(
            WorkoutLog(date = now, workoutName = "Entrenamiento Gym", durationMinutes = 60)
        )

        val result = agentManager.onDeviceSyncTriggered(currentTimeMs = now)

        assertTrue(result.success)
        assertTrue(result.isPlanRenewed) // Renewed because initial run had no plan
        assertEquals(78.0f, result.bodyState.weightKg, 0.01f)
        assertEquals(16.0f, result.bodyState.bodyFatPercentage, 0.01f)
        assertNotNull(agentManager.active3MonthPlan)
        assertNotNull(agentManager.active30DayDietPlan)
        assertEquals(3, agentManager.active3MonthPlan?.months?.size)
        assertEquals(30, agentManager.active30DayDietPlan?.days?.size)
    }

    @Test
    fun testForceRecalculatePlanAndDiet() = runTest {
        val result = agentManager.forceRecalculatePlanAndDiet("Prueba manual")

        assertTrue(result.success)
        assertTrue(result.isPlanRenewed)
        assertEquals("Prueba manual", result.renewalReason)
        assertNotNull(agentManager.active3MonthPlan)
        assertNotNull(agentManager.active30DayDietPlan)
    }
}
