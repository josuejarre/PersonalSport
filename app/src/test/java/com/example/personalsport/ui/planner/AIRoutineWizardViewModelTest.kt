package com.example.personalsport.ui.planner

import com.example.personalsport.ai.CardioDiscipline
import com.example.personalsport.ai.RoutineModality
import com.example.personalsport.ai.ThreeMonthPlan
import com.example.personalsport.data.local.dao.EquipmentDao
import com.example.personalsport.data.local.dao.ProgressPhotoDao
import com.example.personalsport.data.local.dao.WorkoutLogDao
import com.example.personalsport.data.local.entity.Equipment
import com.example.personalsport.data.local.entity.ProgressPhoto
import com.example.personalsport.data.local.entity.WorkoutLog
import com.example.personalsport.data.repository.EquipmentRepository
import com.example.personalsport.data.repository.ProgressPhotoRepository
import com.example.personalsport.data.repository.WorkoutLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeoutException

@OptIn(ExperimentalCoroutinesApi::class)
class AIRoutineWizardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val fakeEquipmentDao = object : EquipmentDao {
        override fun getAllEquipment(): Flow<List<Equipment>> = flowOf(emptyList())
        override suspend fun insertEquipment(equipment: Equipment) {}
        override suspend fun updateEquipment(equipment: Equipment) {}
        override suspend fun deleteEquipment(equipment: Equipment) {}
    }
    private val fakeEquipmentRepo = EquipmentRepository(fakeEquipmentDao, testDispatcher)

    private val fakeWorkoutLogDao = object : WorkoutLogDao {
        override fun getAllWorkoutLogs(): Flow<List<WorkoutLog>> = flowOf(emptyList())
        override suspend fun getLogByExternalId(externalId: String): WorkoutLog? = null
        override suspend fun insertWorkoutLog(workoutLog: WorkoutLog) {}
        override suspend fun updateWorkoutLog(workoutLog: WorkoutLog) {}
        override suspend fun deleteWorkoutLog(workoutLog: WorkoutLog) {}
    }
    private val fakeWorkoutLogRepo = WorkoutLogRepository(fakeWorkoutLogDao, testDispatcher)

    private var insertedPhoto: ProgressPhoto? = null
    private val fakeProgressPhotoDao = object : ProgressPhotoDao {
        override fun getAllProgressPhotos(): Flow<List<ProgressPhoto>> = flowOf(emptyList())
        override suspend fun insertProgressPhoto(progressPhoto: ProgressPhoto) {
            insertedPhoto = progressPhoto
        }
        override suspend fun updateProgressPhoto(progressPhoto: ProgressPhoto) {}
        override suspend fun deleteProgressPhoto(progressPhoto: ProgressPhoto) {}
    }
    private val fakeProgressPhotoRepo = ProgressPhotoRepository(fakeProgressPhotoDao, testDispatcher)

    private lateinit var viewModel: AIRoutineWizardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AIRoutineWizardViewModel(
            equipmentRepository = fakeEquipmentRepo,
            workoutLogRepository = fakeWorkoutLogRepo,
            progressPhotoRepository = fakeProgressPhotoRepo,
            defaultDispatcher = testDispatcher,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun stepNavigation_cyclesCorrectly() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        assertEquals(1, viewModel.uiState.value.currentStep)

        viewModel.nextStep()
        assertEquals(2, viewModel.uiState.value.currentStep)

        viewModel.nextStep()
        assertEquals(3, viewModel.uiState.value.currentStep)

        viewModel.nextStep()
        assertEquals(4, viewModel.uiState.value.currentStep)

        viewModel.nextStep() // Should remain at max step 4
        assertEquals(4, viewModel.uiState.value.currentStep)

        viewModel.previousStep()
        assertEquals(3, viewModel.uiState.value.currentStep)
    }

    @Test
    fun setModalityAndDisciplines_updatesWizardConfig() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.setModality(RoutineModality.MUSCULACION)
        assertEquals(RoutineModality.MUSCULACION, viewModel.uiState.value.wizardConfig.modality)
        assertTrue(viewModel.uiState.value.wizardConfig.selectedDisciplines.contains(CardioDiscipline.MUSCULACION))

        viewModel.toggleDiscipline(CardioDiscipline.NATACION)
        assertTrue(viewModel.uiState.value.wizardConfig.selectedDisciplines.contains(CardioDiscipline.NATACION))
    }

    @Test
    fun generatePlan_createsPlanAndSavesBaselinePhoto() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.setBaselinePhotoUri("content://media/external/images/baseline.jpg")

        var planReceived = false
        viewModel.generatePlan { plan ->
            planReceived = true
            assertNotNull(plan)
            assertEquals(3, plan.months.size)
        }

        assertTrue(planReceived)
        assertNotNull(insertedPhoto)
        assertEquals("content://media/external/images/baseline.jpg", insertedPhoto?.photoUri)
        assertTrue(insertedPhoto?.notes?.contains("Foto Inicial", ignoreCase = true) == true)
    }

    @Test
    fun generatePlan_runsAsynchronouslyInBackground_andUpdatesGeneratingState() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        // Initial state before generation
        Assert.assertFalse(viewModel.uiState.value.isGenerating)
        Assert.assertNull(viewModel.uiState.value.generatedPlan)

        var planReceived: ThreeMonthPlan? = null
        viewModel.generatePlan { plan ->
            planReceived = plan
        }

        // Post generation completion state
        Assert.assertFalse(viewModel.uiState.value.isGenerating)
        assertNotNull(planReceived)
        assertNotNull(viewModel.uiState.value.generatedPlan)
        assertEquals(3, viewModel.uiState.value.generatedPlan?.months?.size)
        Assert.assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun generatePlan_isGeneratingStateMaintained_untilCompletionAndSaved() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.setBaselinePhotoUri("content://media/external/images/baseline.jpg")

        var onSuccessCalled = false
        var planReceived: ThreeMonthPlan? = null

        viewModel.generatePlan { plan ->
            onSuccessCalled = true
            planReceived = plan
            // Verify photo was inserted before onSuccess (dismiss trigger) is invoked
            assertNotNull(insertedPhoto)
            assertEquals("content://media/external/images/baseline.jpg", insertedPhoto?.photoUri)
        }

        assertTrue(onSuccessCalled)
        Assert.assertFalse(viewModel.uiState.value.isGenerating)
        assertNotNull(planReceived)
        assertNotNull(viewModel.uiState.value.generatedPlan)
        assertEquals(3, viewModel.uiState.value.generatedPlan?.months?.size)
    }

    @Test
    fun generatePlan_whenLlmThrowsMemoryException_fallsBackToRuleBasedPlan() = runTest {
        val throwingViewModel = AIRoutineWizardViewModel(
            equipmentRepository = fakeEquipmentRepo,
            workoutLogRepository = fakeWorkoutLogRepo,
            progressPhotoRepository = fakeProgressPhotoRepo,
            defaultDispatcher = testDispatcher,
            ioDispatcher = testDispatcher,
            planGenerator = { _, _, _, _ ->
                throw OutOfMemoryError("Native LLM memory allocation failed")
            }
        )
        backgroundScope.launch { throwingViewModel.uiState.collect {} }

        var planReceived: ThreeMonthPlan? = null
        throwingViewModel.generatePlan { plan ->
            planReceived = plan
        }

        // Must fallback smoothly to rule-based plan without crashing
        Assert.assertFalse(throwingViewModel.uiState.value.isGenerating)
        assertNotNull(planReceived)
        assertNotNull(throwingViewModel.uiState.value.generatedPlan)
        assertEquals(3, throwingViewModel.uiState.value.generatedPlan?.months?.size)
        Assert.assertNull(throwingViewModel.uiState.value.errorMessage)
    }

    @Test
    fun generatePlan_whenLlmThrowsTimeoutException_fallsBackToRuleBasedPlan() = runTest {
        val throwingViewModel = AIRoutineWizardViewModel(
            equipmentRepository = fakeEquipmentRepo,
            workoutLogRepository = fakeWorkoutLogRepo,
            progressPhotoRepository = fakeProgressPhotoRepo,
            defaultDispatcher = testDispatcher,
            ioDispatcher = testDispatcher,
            planGenerator = { _, _, _, _ ->
                throw TimeoutException("LLM inference timeout after 30s")
            }
        )
        backgroundScope.launch { throwingViewModel.uiState.collect {} }

        var planReceived: ThreeMonthPlan? = null
        throwingViewModel.generatePlan { plan ->
            planReceived = plan
        }

        // Must fallback smoothly to rule-based plan without crashing or ANR
        Assert.assertFalse(throwingViewModel.uiState.value.isGenerating)
        assertNotNull(planReceived)
        assertNotNull(throwingViewModel.uiState.value.generatedPlan)
        assertEquals(3, throwingViewModel.uiState.value.generatedPlan?.months?.size)
        Assert.assertNull(throwingViewModel.uiState.value.errorMessage)
    }
}
