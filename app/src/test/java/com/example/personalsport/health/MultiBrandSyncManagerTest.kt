package com.example.personalsport.health

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MultiBrandSyncManagerTest {

    private lateinit var fakeWorkoutLogDao: FakeWorkoutLogDao
    private lateinit var fakeBodyMetricDao: FakeBodyMetricDao
    private lateinit var syncManager: MultiBrandSyncManager

    @Before
    fun setUp() {
        fakeWorkoutLogDao = FakeWorkoutLogDao()
        fakeBodyMetricDao = FakeBodyMetricDao()

        val testContext = TestContext()
        val healthConnectManager = HealthConnectManager(testContext)

        syncManager = MultiBrandSyncManager(
            context = testContext,
            healthConnectManager = healthConnectManager,
            workoutLogDao = fakeWorkoutLogDao,
            bodyMetricDao = fakeBodyMetricDao
        )
    }

    @Test
    fun testAllWatchesEnumContainsRequiredBrands() {
        val watches = MultiBrandSyncManager.ALL_WATCHES
        assertEquals(5, watches.size)
        assertTrue(watches.contains(BrandDevice.HUAWEI_WATCH))
        assertTrue(watches.contains(BrandDevice.AMAZFIT))
        assertTrue(watches.contains(BrandDevice.XIAOMI_WATCH))
        assertTrue(watches.contains(BrandDevice.GARMIN))
        assertTrue(watches.contains(BrandDevice.FITBIT))
    }

    @Test
    fun testAllScalesEnumContainsRequiredBrands() {
        val scales = MultiBrandSyncManager.ALL_SCALES
        assertEquals(4, scales.size)
        assertTrue(scales.contains(BrandDevice.XIAOMI_SCALE))
        assertTrue(scales.contains(BrandDevice.HUAWEI_SCALE))
        assertTrue(scales.contains(BrandDevice.HONOR_SCALE))
        assertTrue(scales.contains(BrandDevice.TANITA_SCALE))
    }

    @Test
    fun testCompanionPackageDetectionForWatchBrands() {
        val packageMap = mapOf(
            BrandDevice.HUAWEI_WATCH to "com.huawei.health",
            BrandDevice.AMAZFIT to "com.huami.watch.hmwatchmanager",
            BrandDevice.XIAOMI_WATCH to "com.mi.health",
            BrandDevice.GARMIN to "com.garmin.android.apps.connectmobile",
            BrandDevice.FITBIT to "com.fitbit.FitbitMobile"
        )

        for ((brand, expectedPkg) in packageMap) {
            val checker: (String) -> Boolean = { pkg -> pkg == expectedPkg }
            assertTrue(syncManager.isCompanionAppInstalled(brand, packageChecker = checker))
            assertEquals(expectedPkg, syncManager.getInstalledPackageName(brand, packageChecker = checker))
        }
    }

    @Test
    fun testCompanionPackageDetectionForScaleBrands() {
        val packageMap = mapOf(
            BrandDevice.XIAOMI_SCALE to "com.mi.health",
            BrandDevice.HUAWEI_SCALE to "com.huawei.health",
            BrandDevice.HONOR_SCALE to "com.hihonor.health",
            BrandDevice.TANITA_SCALE to "jp.co.tanita.healthplanet"
        )

        for ((brand, expectedPkg) in packageMap) {
            val checker: (String) -> Boolean = { pkg -> pkg == expectedPkg }
            assertTrue(syncManager.isCompanionAppInstalled(brand, packageChecker = checker))
            assertEquals(expectedPkg, syncManager.getInstalledPackageName(brand, packageChecker = checker))
        }

        val emptyChecker: (String) -> Boolean = { false }
        assertFalse(syncManager.isCompanionAppInstalled(BrandDevice.TANITA_SCALE, packageChecker = emptyChecker))
        assertNull(syncManager.getInstalledPackageName(BrandDevice.TANITA_SCALE, packageChecker = emptyChecker))
    }

    @Test
    fun testConnectionStepsGuidesGeneratedForEachBrand() {
        val allBrands = BrandDevice.entries.toTypedArray()
        for (brand in allBrands) {
            val steps = syncManager.getConnectionSteps(brand)
            assertTrue("Steps for ${brand.displayName} should not be empty", steps.isNotEmpty())
            assertTrue("Steps should mention Health Connect or app settings", steps.any { it.contains("Health Connect", ignoreCase = true) || it.contains("Ajustes", ignoreCase = true) })
        }
    }

    @Test
    fun testImportDirectScaleDataXiaomiSmartScale() = runTest {
        val result = syncManager.importDirectScaleData(BrandDevice.XIAOMI_SCALE)

        assertTrue(result.success)
        assertEquals(1, result.recordsSynced)
        assertNotNull(result.scaleDetails)

        val details = result.scaleDetails!!
        assertEquals(BrandDevice.XIAOMI_SCALE, details.brand)
        assertEquals(72.5f, details.weightKg)
        assertEquals(16.2f, details.bodyFatPercentage)
        assertEquals(58.4f, details.muscleMassPercentage)
        assertEquals(4, details.visceralFatLevel)
        assertEquals(22.8f, details.bmi)

        // Verify inserted into database
        assertEquals(1, fakeBodyMetricDao.metrics.size)
        assertEquals(72.5f, fakeBodyMetricDao.metrics[0].weightKg)
        assertEquals(16.2f, fakeBodyMetricDao.metrics[0].bodyFatPercentage)
    }

    @Test
    fun testImportDirectScaleDataHuaweiSmartScale() = runTest {
        val result = syncManager.importDirectScaleData(BrandDevice.HUAWEI_SCALE)

        assertTrue(result.success)
        assertEquals(1, result.recordsSynced)
        assertNotNull(result.scaleDetails)

        val details = result.scaleDetails!!
        assertEquals(74.8f, details.weightKg)
        assertEquals(15.8f, details.bodyFatPercentage)
        assertEquals(60.1f, details.muscleMassPercentage)
        assertEquals(5, details.visceralFatLevel)
        assertEquals(23.4f, details.bmi)
    }

    @Test
    fun testImportDirectScaleDataHonorSmartScale() = runTest {
        val result = syncManager.importDirectScaleData(BrandDevice.HONOR_SCALE)

        assertTrue(result.success)
        assertEquals(1, result.recordsSynced)
        assertNotNull(result.scaleDetails)

        val details = result.scaleDetails!!
        assertEquals(73.2f, details.weightKg)
        assertEquals(16.5f, details.bodyFatPercentage)
        assertEquals(59.0f, details.muscleMassPercentage)
        assertEquals(4, details.visceralFatLevel)
        assertEquals(23.0f, details.bmi)
    }

    @Test
    fun testImportDirectScaleDataTanitaSmartScale() = runTest {
        val result = syncManager.importDirectScaleData(BrandDevice.TANITA_SCALE)

        assertTrue(result.success)
        assertEquals(1, result.recordsSynced)
        assertNotNull(result.scaleDetails)

        val details = result.scaleDetails!!
        assertEquals(75.0f, details.weightKg)
        assertEquals(14.9f, details.bodyFatPercentage)
        assertEquals(61.2f, details.muscleMassPercentage)
        assertEquals(3, details.visceralFatLevel)
        assertEquals(23.5f, details.bmi)
    }

    @Test
    fun testImportDirectScaleDataDuplicatePrevention() = runTest {
        val firstRun = syncManager.importDirectScaleData(BrandDevice.TANITA_SCALE)
        assertEquals(1, firstRun.recordsSynced)

        val secondRun = syncManager.importDirectScaleData(BrandDevice.TANITA_SCALE)
        assertEquals(0, secondRun.recordsSynced)
        assertTrue(secondRun.message.contains("ya están registrados"))
    }

    @Test
    fun testImportDirectWatchDataAmazfit() = runTest {
        val result = syncManager.importDirectWatchData(BrandDevice.AMAZFIT)

        assertTrue(result.success)
        assertEquals(5, result.recordsSynced)
        assertEquals(5, fakeWorkoutLogDao.logs.size)

        val runShort = fakeWorkoutLogDao.logs.find { it.externalId == "amazfit_run_short_5k_direct" }
        assertNotNull(runShort)
        assertTrue(runShort!!.workoutName.contains("Amazfit"))
        assertEquals(24, runShort.durationMinutes)
        assertFalse(runShort.routeGpsPoints.isNullOrBlank())

        val openWaterSwim = fakeWorkoutLogDao.logs.find { it.externalId == "amazfit_open_water_swim_direct" }
        assertNotNull(openWaterSwim)
        assertTrue(openWaterSwim!!.workoutName.contains("Aguas Abiertas"))
        assertFalse(openWaterSwim.routeGpsPoints.isNullOrBlank())

        val swim = fakeWorkoutLogDao.logs.find { it.externalId == "amazfit_swim_1500m_direct" }
        assertNotNull(swim)
        assertTrue(swim!!.workoutName.contains("Piscina"))

        val strength = fakeWorkoutLogDao.logs.find { it.externalId == "amazfit_strength_gym_direct" }
        assertNotNull(strength)
        assertTrue(strength!!.workoutName.contains("Musculación"))
    }

    @Test
    fun testImportDirectWatchDataGarmin() = runTest {
        val result = syncManager.importDirectWatchData(BrandDevice.GARMIN)

        assertTrue(result.success)
        assertEquals(5, result.recordsSynced)

        val runShort = fakeWorkoutLogDao.logs.find { it.externalId == "garmin_run_short_5k_direct" }
        assertNotNull(runShort)
        assertTrue(runShort!!.workoutName.contains("Garmin"))
    }

    @Test
    fun testImportDirectWatchDataXiaomi() = runTest {
        val result = syncManager.importDirectWatchData(BrandDevice.XIAOMI_WATCH)

        assertTrue(result.success)
        assertEquals(5, result.recordsSynced)

        val runShort = fakeWorkoutLogDao.logs.find { it.externalId == "xiaomi_watch_run_short_5k_direct" }
        assertNotNull(runShort)
        assertTrue(runShort!!.workoutName.contains("Xiaomi"))
    }

    @Test
    fun testImportDirectWatchDataFitbit() = runTest {
        val result = syncManager.importDirectWatchData(BrandDevice.FITBIT)

        assertTrue(result.success)
        assertEquals(5, result.recordsSynced)

        val runShort = fakeWorkoutLogDao.logs.find { it.externalId == "fitbit_run_short_5k_direct" }
        assertNotNull(runShort)
        assertTrue(runShort!!.workoutName.contains("Fitbit"))
    }

    @Test
    fun testImportDirectWatchDataHuaweiGT() = runTest {
        val result = syncManager.importDirectWatchData(BrandDevice.HUAWEI_WATCH)

        assertTrue(result.success)
        assertEquals(5, result.recordsSynced)

        val runShort = fakeWorkoutLogDao.logs.find { it.externalId == "huawei_gt_run_short_5k_fixed" }
        assertNotNull(runShort)
        assertEquals("Huawei Watch GT - Carrera Corta (5K)", runShort?.workoutName)
    }

    @Test
    fun testImportDirectWatchDataDuplicatePrevention() = runTest {
        val firstRun = syncManager.importDirectWatchData(BrandDevice.GARMIN)
        assertEquals(5, firstRun.recordsSynced)

        val secondRun = syncManager.importDirectWatchData(BrandDevice.GARMIN)
        assertEquals(0, secondRun.recordsSynced)
        assertTrue(secondRun.message.contains("ya se encuentran sincronizadas"))
    }
}
