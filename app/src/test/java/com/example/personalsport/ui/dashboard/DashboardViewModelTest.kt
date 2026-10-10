package com.example.personalsport.ui.dashboard

import com.example.personalsport.data.local.dao.BodyMetricDao
import com.example.personalsport.data.local.dao.WorkoutLogDao
import com.example.personalsport.data.local.entity.BodyMetric
import com.example.personalsport.data.local.entity.WorkoutLog
import com.example.personalsport.data.repository.BodyMetricRepository
import com.example.personalsport.data.repository.WorkoutLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    class FakeBodyMetricDao : BodyMetricDao {
        private val _metrics = MutableStateFlow<List<BodyMetric>>(emptyList())
        override fun getAllBodyMetrics(): Flow<List<BodyMetric>> = _metrics

        override suspend fun getMetricByDate(date: Long): BodyMetric? {
            return _metrics.value.find { it.date == date }
        }

        override suspend fun insertBodyMetric(bodyMetric: BodyMetric) {
            val current = _metrics.value.toMutableList()
            current.add(bodyMetric)
            _metrics.value = current
        }

        override suspend fun updateBodyMetric(bodyMetric: BodyMetric) {
            insertBodyMetric(bodyMetric)
        }

        override suspend fun deleteBodyMetric(bodyMetric: BodyMetric) {
            _metrics.value = _metrics.value.filterNot { it.date == bodyMetric.date }
        }

        fun setMetrics(metrics: List<BodyMetric>) {
            _metrics.value = metrics
        }
    }

    class FakeWorkoutLogDao : WorkoutLogDao {
        private val _logs = MutableStateFlow<List<WorkoutLog>>(emptyList())
        override fun getAllWorkoutLogs(): Flow<List<WorkoutLog>> = _logs

        override suspend fun getLogByExternalId(externalId: String): WorkoutLog? {
            return _logs.value.find { it.externalId == externalId }
        }

        override suspend fun insertWorkoutLog(workoutLog: WorkoutLog) {
            val current = _logs.value.toMutableList()
            current.removeAll { it.id == workoutLog.id && workoutLog.id != 0 }
            current.add(workoutLog)
            _logs.value = current
        }

        override suspend fun updateWorkoutLog(workoutLog: WorkoutLog) {
            insertWorkoutLog(workoutLog)
        }

        override suspend fun deleteWorkoutLog(workoutLog: WorkoutLog) {
            _logs.value = _logs.value.filterNot { it.id == workoutLog.id }
        }

        fun setLogs(logs: List<WorkoutLog>) {
            _logs.value = logs
        }
    }

    private lateinit var fakeBodyMetricDao: FakeBodyMetricDao
    private lateinit var fakeWorkoutLogDao: FakeWorkoutLogDao
    private lateinit var bodyMetricRepository: BodyMetricRepository
    private lateinit var workoutLogRepository: WorkoutLogRepository
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeBodyMetricDao = FakeBodyMetricDao()
        fakeWorkoutLogDao = FakeWorkoutLogDao()
        bodyMetricRepository = BodyMetricRepository(fakeBodyMetricDao, testDispatcher)
        workoutLogRepository = WorkoutLogRepository(fakeWorkoutLogDao, testDispatcher)
        viewModel = DashboardViewModel(bodyMetricRepository, workoutLogRepository, testDispatcher)
    }

    @After
    fun tearDown() {
        viewModel.onCleared()
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiState_withNoWorkoutLogsAndNoMetrics_emitsCleanEmptyState() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertNull("Weight should be null initially", state.latestWeight)
        assertNull("Body Fat should be null initially", state.latestBodyFat)
        assertNull("Muscle Mass should be null initially", state.muscleMassKg)
        assertNull("Visceral Fat should be null initially", state.visceralFatLevel)
        assertNull("Bone Mass should be null initially", state.boneMassKg)
        assertNull("Body Water should be null initially", state.bodyWaterPercentage)
        assertNull("BMI should be null initially", state.bmi)
        assertEquals("Total Workout Logs should be 0", 0, state.totalWorkoutsCount)
        assertEquals("Total Duration Minutes should be 0", 0, state.totalDurationMinutes)
        assertNull("Sleep Quality should be null initially", state.sleepQuality)
        assertNull("Post workout recommendation should be null when no logs exist", state.postWorkoutRecommendation)
        assertNull("Sleep analysis should be null when no logs exist", state.sleepAnalysis)
        assertNull("Recovery countdown should be null when no logs exist", state.recoveryCountdown)
        assertTrue("Recent workouts should be empty initially", state.recentWorkouts.isEmpty())
        assertNotNull("AI insight should be present with empty prompt summary", state.aiInsight)
        assertTrue(
            "AI insight should indicate no metrics registered",
            state.aiInsight?.summary?.contains("Sin métricas registradas") == true
        )
    }

    @Test
    fun testPostWorkoutRecommendation_withRunningWorkout() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val runningLog = WorkoutLog(
            id = 1,
            date = System.currentTimeMillis(),
            workoutName = "Huawei Watch GT - Carrera Larga 10.5K",
            durationMinutes = 52,
            notes = "Carrera Larga 10.5 km"
        )
        fakeWorkoutLogDao.setLogs(listOf(runningLog))
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        val recommendation = state.postWorkoutRecommendation
        assertNotNull(recommendation)
        assertTrue(recommendation!!.contains("Tras tu última sesión (Huawei Watch GT - Carrera Larga 10.5K)"))
        assertTrue(recommendation.contains("carbohidratos"))
        assertTrue(recommendation.contains("sóleos e isquiotibiales"))
    }

    @Test
    fun testPostWorkoutRecommendation_withSwimmingWorkout() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val swimLog = WorkoutLog(
            id = 2,
            date = System.currentTimeMillis(),
            workoutName = "Huawei Watch GT - Natación en Piscina",
            durationMinutes = 40,
            notes = "1500m crol"
        )
        fakeWorkoutLogDao.setLogs(listOf(swimLog))
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        val recommendation = state.postWorkoutRecommendation
        assertNotNull(recommendation)
        assertTrue(recommendation!!.contains("Tras tu última sesión (Huawei Watch GT - Natación en Piscina)"))
        assertTrue(recommendation.contains("rehidratación hipotónica"))
        assertTrue(recommendation.contains("manguito rotador"))
    }

    @Test
    fun testPostWorkoutRecommendation_withStrengthWorkout() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val strengthLog = WorkoutLog(
            id = 3,
            date = System.currentTimeMillis(),
            workoutName = "Huawei Watch GT - Entrenamiento de Musculación",
            durationMinutes = 60,
            notes = "Hipertrofia"
        )
        fakeWorkoutLogDao.setLogs(listOf(strengthLog))
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        val recommendation = state.postWorkoutRecommendation
        assertNotNull(recommendation)
        assertTrue(recommendation!!.contains("Tras tu última sesión (Huawei Watch GT - Entrenamiento de Musculación)"))
        assertTrue(recommendation.contains("proteína rápida"))
        assertTrue(recommendation.contains("creatina"))
    }

    @Test
    fun testPostWorkoutRecommendation_withAthleticsWorkout() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val athleticsLog = WorkoutLog(
            id = 4,
            date = System.currentTimeMillis(),
            workoutName = "Series 400m - Atletismo",
            durationMinutes = 35,
            notes = "Series en pista"
        )
        fakeWorkoutLogDao.setLogs(listOf(athleticsLog))
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        val recommendation = state.postWorkoutRecommendation
        assertNotNull(recommendation)
        assertTrue(recommendation!!.contains("Tras tu última sesión (Series 400m - Atletismo)"))
        assertTrue(recommendation.contains("glucógeno muscular"))
    }

    @Test
    fun testPostWorkoutRecommendation_selectsMostRecentWorkout() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val now = System.currentTimeMillis()
        val olderLog = WorkoutLog(
            id = 1,
            date = now - 86400000L,
            workoutName = "Entrenamiento Antiguo - Musculación",
            durationMinutes = 45
        )
        val newerLog = WorkoutLog(
            id = 2,
            date = now,
            workoutName = "Huawei Watch GT - Carrera Corta (5K)",
            durationMinutes = 25
        )
        fakeWorkoutLogDao.setLogs(listOf(olderLog, newerLog))
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        val recommendation = state.postWorkoutRecommendation
        assertNotNull(recommendation)
        assertTrue(
            "Recommendation should be based on the most recent workout",
            recommendation!!.contains("Carrera Corta (5K)")
        )
    }

    @Test
    fun testAddQuickBodyMetric_updatesMetricsAndSyncMessage() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.addQuickBodyMetric(80.0f, 18.0f)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(80.0f, state.latestWeight)
        assertEquals(18.0f, state.latestBodyFat)
        assertEquals("Body metrics updated successfully", state.syncMessage)
    }

    @Test
    fun testSetSyncingAndSyncMessageManagement() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.setSyncing(true)
        viewModel.setSyncMessage("Sincronizando datos...")
        testDispatcher.scheduler.runCurrent()

        var state = viewModel.uiState.value
        assertTrue(state.isSyncing)
        assertEquals("Sincronizando datos...", state.syncMessage)

        viewModel.clearSyncMessage()
        testDispatcher.scheduler.runCurrent()

        state = viewModel.uiState.value
        assertNull(state.syncMessage)
    }
}
