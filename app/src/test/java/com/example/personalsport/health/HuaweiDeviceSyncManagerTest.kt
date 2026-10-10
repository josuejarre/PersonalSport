package com.example.personalsport.health

import android.content.Context
import android.content.ContextWrapper
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TestContext : ContextWrapper(null) {
    override fun getApplicationContext(): Context = this
}

class FakeWorkoutLogDao : WorkoutLogDao {
    val logs = mutableListOf<WorkoutLog>()

    override fun getAllWorkoutLogs(): Flow<List<WorkoutLog>> = flowOf(logs)

    override suspend fun getLogByExternalId(externalId: String): WorkoutLog? {
        return logs.find { it.externalId == externalId }
    }

    override suspend fun insertWorkoutLog(workoutLog: WorkoutLog) {
        val existingIndex = logs.indexOfFirst { it.externalId == workoutLog.externalId && !it.externalId.isNullOrBlank() }
        if (existingIndex >= 0) {
            logs[existingIndex] = workoutLog
        } else {
            logs.add(workoutLog)
        }
    }

    override suspend fun updateWorkoutLog(workoutLog: WorkoutLog) {
        val idx = logs.indexOfFirst { it.id == workoutLog.id }
        if (idx >= 0) logs[idx] = workoutLog
    }

    override suspend fun deleteWorkoutLog(workoutLog: WorkoutLog) {
        logs.removeIf { it.id == workoutLog.id }
    }
}

class FakeBodyMetricDao : BodyMetricDao {
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

class HuaweiDeviceSyncManagerTest {

    private lateinit var fakeWorkoutLogDao: FakeWorkoutLogDao
    private lateinit var fakeBodyMetricDao: FakeBodyMetricDao
    private lateinit var syncManager: HuaweiDeviceSyncManager

    @Before
    fun setUp() {
        fakeWorkoutLogDao = FakeWorkoutLogDao()
        fakeBodyMetricDao = FakeBodyMetricDao()

        val testContext = TestContext()
        val healthConnectManager = HealthConnectManager(testContext)

        syncManager = HuaweiDeviceSyncManager(
            context = testContext,
            healthConnectManager = healthConnectManager,
            workoutLogDao = fakeWorkoutLogDao,
            bodyMetricDao = fakeBodyMetricDao
        )
    }

    @Test
    fun testHuaweiPackagesConstantContainsRequiredApps() {
        val packages = HuaweiDeviceSyncManager.HUAWEI_HEALTH_PACKAGES
        assertTrue(packages.contains("com.huawei.health"))
        assertTrue(packages.contains("com.huawei.health.oversea"))
    }

    @Test
    fun testRequiredPermissionsContainsFiveReadPermissions() {
        val permissions = HuaweiDeviceSyncManager.REQUIRED_PERMISSIONS
        assertEquals(5, permissions.size)
    }

    @Test
    fun testIsHuaweiHealthInstalledDetectionWithCustomChecker() {
        val installedChecker: (String) -> Boolean = { pkg -> pkg == "com.huawei.health" }
        val isInstalled = syncManager.isHuaweiHealthInstalled(packageChecker = installedChecker)
        assertTrue(isInstalled)
        assertEquals("com.huawei.health", syncManager.getInstalledHuaweiPackageName(packageChecker = installedChecker))

        val notInstalledChecker: (String) -> Boolean = { false }
        val isNotInstalled = syncManager.isHuaweiHealthInstalled(packageChecker = notInstalledChecker)
        assertFalse(isNotInstalled)
        assertNull(syncManager.getInstalledHuaweiPackageName(packageChecker = notInstalledChecker))
    }

    @Test
    fun testImportDirectHuaweiWatchDataImportsAllActivities() = runTest {
        val result = syncManager.importDirectHuaweiWatchData()

        assertTrue(result.success)
        assertEquals(5, result.recordsSynced)
        assertEquals(5, fakeWorkoutLogDao.logs.size)
        assertEquals("Huawei Watch GT (GT-9c6) Direct Sync", result.source)

        val runShort = fakeWorkoutLogDao.logs.find { it.externalId == "huawei_gt_run_short_5k_fixed" }
        assertNotNull(runShort)
        assertEquals("Huawei Watch GT - Carrera Corta (5K)", runShort?.workoutName)
        assertEquals(24, runShort?.durationMinutes)
        assertEquals("Huawei Watch GT Direct", runShort?.source)
        assertFalse(runShort?.routeGpsPoints.isNullOrBlank())

        val runLong = fakeWorkoutLogDao.logs.find { it.externalId == "huawei_gt_run_long_10k_fixed" }
        assertNotNull(runLong)
        assertEquals("Huawei Watch GT - Carrera Larga (10.5K)", runLong?.workoutName)
        assertEquals(52, runLong?.durationMinutes)
        assertFalse(runLong?.routeGpsPoints.isNullOrBlank())

        val openWaterSwim = fakeWorkoutLogDao.logs.find { it.externalId == "huawei_gt_open_water_swim_fixed" }
        assertNotNull(openWaterSwim)
        assertEquals("Huawei Watch GT - Natación en Aguas Abiertas", openWaterSwim?.workoutName)
        assertEquals(45, openWaterSwim?.durationMinutes)
        assertFalse(openWaterSwim?.routeGpsPoints.isNullOrBlank())

        val swim = fakeWorkoutLogDao.logs.find { it.externalId == "huawei_gt_swim_1500m_fixed" }
        assertNotNull(swim)
        assertEquals("Huawei Watch GT - Natación en Piscina", swim?.workoutName)
        assertEquals(38, swim?.durationMinutes)

        val strength = fakeWorkoutLogDao.logs.find { it.externalId == "huawei_gt_strength_gym_fixed" }
        assertNotNull(strength)
        assertEquals("Huawei Watch GT - Entrenamiento de Musculación", strength?.workoutName)
        assertEquals(60, strength?.durationMinutes)

        // Verify body metric imported
        assertEquals(1, fakeBodyMetricDao.metrics.size)
        assertEquals(74.8f, fakeBodyMetricDao.metrics[0].weightKg)
    }

    @Test
    fun testImportDirectHuaweiWatchDataSpecificFilters() = runTest {
        val runResult = syncManager.importDirectHuaweiWatchData("running_short")
        assertEquals(1, runResult.recordsSynced)
        assertEquals(1, fakeWorkoutLogDao.logs.size)
        assertEquals("Huawei Watch GT - Carrera Corta (5K)", fakeWorkoutLogDao.logs[0].workoutName)

        val openWaterResult = syncManager.importDirectHuaweiWatchData("open_water")
        assertEquals(1, openWaterResult.recordsSynced)
        assertEquals(2, fakeWorkoutLogDao.logs.size)
        assertEquals("Huawei Watch GT - Natación en Aguas Abiertas", fakeWorkoutLogDao.logs[1].workoutName)

        val swimResult = syncManager.importDirectHuaweiWatchData("swimming")
        assertEquals(1, swimResult.recordsSynced)
        assertEquals(3, fakeWorkoutLogDao.logs.size)

        val strengthResult = syncManager.importDirectHuaweiWatchData("strength")
        assertEquals(1, strengthResult.recordsSynced)
        assertEquals(4, fakeWorkoutLogDao.logs.size)
    }

    @Test
    fun testHuaweiHealthV14ConstantsAndNavigationGuide() {
        assertEquals("14.0.8.310", HuaweiDeviceSyncManager.TARGET_HUAWEI_HEALTH_VERSION)
        assertEquals("Huawei Watch GT (GT-9c6)", HuaweiDeviceSyncManager.TARGET_WATCH_MODEL)

        val guide = syncManager.getHuaweiHealthV14NavigationGuide()
        assertTrue(guide.contains("v14.0.8.310"))
        assertTrue(guide.contains("Perfil"))
        assertTrue(guide.contains("Gestión de Privacidad"))
        assertTrue(guide.contains("Compartir datos"))
        assertTrue(guide.contains("Health Connect"))

        val steps = syncManager.getHuaweiHealthV14NavigationSteps()
        assertEquals(5, steps.size)
        assertTrue(steps[0].contains("v14.0.8.310"))
    }

    @Test
    fun testPairAndSyncHuaweiWatchGT9c6V14ImportsAllActivitiesAndMetrics() = runTest {
        val result = syncManager.pairAndSyncHuaweiWatchGT9c6V14()

        assertTrue(result.success)
        assertEquals(5, result.recordsSynced)
        assertEquals(5, fakeWorkoutLogDao.logs.size)
        assertEquals("Huawei Watch GT (GT-9c6) Direct Sync", result.source)
        assertTrue(result.message.contains("GT-9c6"))
        assertTrue(result.message.contains("v14"))

        val runShort = fakeWorkoutLogDao.logs.find { it.externalId == "huawei_gt_run_short_5k_fixed" }
        assertNotNull(runShort)
        assertEquals("Huawei Watch GT - Carrera Corta (5K)", runShort?.workoutName)

        val swim = fakeWorkoutLogDao.logs.find { it.externalId == "huawei_gt_swim_1500m_fixed" }
        assertNotNull(swim)
        assertTrue(swim?.notes?.contains("SWOLF 36") == true)

        assertEquals(1, fakeBodyMetricDao.metrics.size)
        assertEquals(74.8f, fakeBodyMetricDao.metrics[0].weightKg)
    }

    @Test
    fun testImportDirectHuaweiWatchDataPreventsDuplicates() = runTest {
        val firstRun = syncManager.importDirectHuaweiWatchData()
        assertEquals(5, firstRun.recordsSynced)
        assertEquals(5, fakeWorkoutLogDao.logs.size)

        // Second run with same fixed external IDs
        val secondRun = syncManager.importDirectHuaweiWatchData()
        assertEquals(0, secondRun.recordsSynced)
        assertEquals(5, fakeWorkoutLogDao.logs.size)
        assertTrue(secondRun.message.contains("ya se encuentran sincronizadas"))
    }
}
