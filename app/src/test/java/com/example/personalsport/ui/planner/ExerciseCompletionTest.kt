package com.example.personalsport.ui.planner

import com.example.personalsport.data.local.dao.CompletedExerciseDao
import com.example.personalsport.data.local.dao.EquipmentDao
import com.example.personalsport.data.local.dao.WorkoutLogDao
import com.example.personalsport.data.local.entity.CompletedExerciseRecord
import com.example.personalsport.data.local.entity.Equipment
import com.example.personalsport.data.local.entity.WorkoutLog
import com.example.personalsport.data.model.ExerciseItem
import com.example.personalsport.data.repository.CompletedExerciseRepository
import com.example.personalsport.data.repository.EquipmentRepository
import com.example.personalsport.data.repository.WorkoutLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeCompletedExerciseDao : CompletedExerciseDao {
    private val records = MutableStateFlow<List<CompletedExerciseRecord>>(emptyList())

    override fun getAllCompletedExercises(): Flow<List<CompletedExerciseRecord>> = records

    override suspend fun insertOrUpdate(record: CompletedExerciseRecord) {
        val current = records.value.toMutableList()
        current.removeAll { it.exerciseId == record.exerciseId }
        current.add(record)
        records.value = current
    }

    override suspend fun deleteByExerciseId(exerciseId: String) {
        val current = records.value.toMutableList()
        current.removeAll { it.exerciseId == exerciseId }
        records.value = current
    }

    override suspend fun isExerciseCompleted(exerciseId: String): Boolean {
        return records.value.any { it.exerciseId == exerciseId && it.isCompleted }
    }
}

class FakeEquipmentDao : EquipmentDao {
    override fun getAllEquipment(): Flow<List<Equipment>> = MutableStateFlow(emptyList())
    override suspend fun insertEquipment(equipment: Equipment) {}
    override suspend fun updateEquipment(equipment: Equipment) {}
    override suspend fun deleteEquipment(equipment: Equipment) {}
}

class FakeWorkoutLogDao : WorkoutLogDao {
    override fun getAllWorkoutLogs(): Flow<List<WorkoutLog>> = MutableStateFlow(emptyList())
    override suspend fun getLogByExternalId(externalId: String): WorkoutLog? = null
    override suspend fun insertWorkoutLog(workoutLog: WorkoutLog) {}
    override suspend fun updateWorkoutLog(workoutLog: WorkoutLog) {}
    override suspend fun deleteWorkoutLog(workoutLog: WorkoutLog) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseCompletionTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testExerciseItemDataModelDefaults() {
        val exercise = ExerciseItem(
            title = "Leg Press Technogym",
            category = "Musculación",
            targetMuscle = "Cuádriceps",
            equipmentName = "Technogym Leg Press",
            instructions = "Desciende la plataforma flexionando las rodillas a 90 grados y empuja con fuerza centrada en los talones.",
            durationOrReps = "4 series x 12 reps"
        )

        assertEquals("Leg Press Technogym", exercise.title)
        assertEquals("Musculación", exercise.category)
        assertEquals("Cuádriceps", exercise.targetMuscle)
        assertEquals("Technogym Leg Press", exercise.equipmentName)
        assertFalse(exercise.isCompleted)
        assertNotNull(exercise.id)
    }

    @Test
    fun testPlannedExerciseStepToExerciseItemConversion() {
        val step = PlannedExerciseStep(
            title = "Jalón en Polea Alta",
            description = "Enfoque en dorsal ancho",
            durationOrReps = "4x10",
            equipmentUsed = "Technogym Lat Machine",
            category = "Poleas",
            targetMuscle = "Dorsales",
            equipmentName = "Technogym Lat Machine",
            instructions = "Tracciona la barra al esternón juntando las escápulas.",
            isCompleted = true
        )

        val item = step.toExerciseItem()
        assertEquals(step.id, item.id)
        assertEquals("Jalón en Polea Alta", item.title)
        assertEquals("Enfoque en dorsal ancho", item.description)
        assertEquals("Poleas", item.category)
        assertEquals("Dorsales", item.targetMuscle)
        assertEquals("Technogym Lat Machine", item.equipmentName)
        assertTrue(item.isCompleted)
    }

    @Test
    fun testCompletedExerciseRepositoryToggle() = runTest(testDispatcher) {
        val fakeDao = FakeCompletedExerciseDao()
        val repository = CompletedExerciseRepository(fakeDao, testDispatcher)

        val exerciseId = "ex_leg_press_101"
        val exerciseTitle = "Leg Press Technogym"

        // Initially empty
        var completedList = repository.completedExercises.first()
        assertTrue(completedList.isEmpty())

        // Toggle ON
        repository.toggleExerciseCompletion(exerciseId, exerciseTitle, currentStatus = false)
        completedList = repository.completedExercises.first()
        assertEquals(1, completedList.size)
        assertEquals(exerciseId, completedList[0].exerciseId)
        assertTrue(completedList[0].isCompleted)

        // Toggle OFF
        repository.toggleExerciseCompletion(exerciseId, exerciseTitle, currentStatus = true)
        completedList = repository.completedExercises.first()
        assertTrue(completedList.isEmpty())
    }

    @Test
    fun testPlannerViewModelExerciseCompletionToggling() = runTest(testDispatcher) {
        val fakeDao = FakeCompletedExerciseDao()
        val completedRepo = CompletedExerciseRepository(fakeDao, testDispatcher)
        val equipmentRepo = EquipmentRepository(FakeEquipmentDao(), testDispatcher)
        val workoutLogRepo = WorkoutLogRepository(FakeWorkoutLogDao(), testDispatcher)

        val viewModel = PlannerViewModel(
            equipmentRepository = equipmentRepo,
            workoutLogRepository = workoutLogRepo,
            completedExerciseRepository = completedRepo,
            ioDispatcher = testDispatcher
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        testScheduler.advanceUntilIdle()

        val step = PlannedExerciseStep(
            title = "Press de Pecho en Banco",
            description = "Fuerza básica",
            durationOrReps = "3x10",
            equipmentUsed = "Barra Olímpica",
            category = "Musculación",
            targetMuscle = "Pectoral Mayor",
            equipmentName = "Barra Olímpica & Banco Plano"
        )

        // Toggle step completion
        viewModel.toggleExerciseCompletion(step)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.completedExerciseIds.contains(step.id))

        // Toggle again to unmark
        viewModel.toggleExerciseCompletion(step)
        testScheduler.advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertFalse(updatedState.completedExerciseIds.contains(step.id))

        collectJob.cancel()
    }
}
