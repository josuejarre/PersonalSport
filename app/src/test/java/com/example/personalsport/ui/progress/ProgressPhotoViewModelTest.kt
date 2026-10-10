package com.example.personalsport.ui.progress

import com.example.personalsport.data.local.dao.ProgressPhotoDao
import com.example.personalsport.data.local.entity.ProgressPhoto
import com.example.personalsport.data.repository.ProgressPhotoRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressPhotoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeProgressPhotoDao : ProgressPhotoDao {
        val photos = MutableStateFlow<List<ProgressPhoto>>(emptyList())

        override fun getAllProgressPhotos(): Flow<List<ProgressPhoto>> = photos

        override suspend fun insertProgressPhoto(progressPhoto: ProgressPhoto) {
            val list = photos.value.toMutableList()
            list.add(progressPhoto)
            photos.value = list
        }

        override suspend fun updateProgressPhoto(progressPhoto: ProgressPhoto) {
            val list = photos.value.map { if (it.id == progressPhoto.id) progressPhoto else it }
            photos.value = list
        }

        override suspend fun deleteProgressPhoto(progressPhoto: ProgressPhoto) {
            val list = photos.value.filterNot { it.id == progressPhoto.id }
            photos.value = list
        }
    }

    private lateinit var fakeDao: FakeProgressPhotoDao
    private lateinit var repository: ProgressPhotoRepository
    private lateinit var viewModel: ProgressPhotoViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeProgressPhotoDao()
        repository = ProgressPhotoRepository(fakeDao, testDispatcher)
        viewModel = ProgressPhotoViewModel(repository, null, testDispatcher, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCameraModeToggle() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.setCameraMode(true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.cameraState.isCameraMode)

        viewModel.setCameraMode(false)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.cameraState.isCameraMode)
    }

    @Test
    fun testPhotoComparisonSelection() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val photo1 = ProgressPhoto(id = 1, date = 1000L, photoUri = "file:///p1", notes = "Initial")
        val photo2 = ProgressPhoto(id = 2, date = 2000L, photoUri = "file:///p2", notes = "After 1 week")
        fakeDao.insertProgressPhoto(photo1)
        fakeDao.insertProgressPhoto(photo2)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.togglePhotoForComparison(photo1)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.selectedForComparison.size)

        viewModel.togglePhotoForComparison(photo2)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.selectedForComparison.size)
        assertEquals(photo1.id, viewModel.uiState.value.selectedForComparison[0].id)
        assertEquals(photo2.id, viewModel.uiState.value.selectedForComparison[1].id)

        viewModel.clearComparisonSelection()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.selectedForComparison.isEmpty())
    }

    @Test
    fun testVisualAiAnalysisTriggerAndClear() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val photo1 = ProgressPhoto(id = 1, date = 1000L, photoUri = "file:///p1", notes = "Baseline")
        val photo2 = ProgressPhoto(id = 2, date = 2000000000L, photoUri = "file:///p2", notes = "Cut defin abs")
        fakeDao.insertProgressPhoto(photo1)
        fakeDao.insertProgressPhoto(photo2)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.togglePhotoForComparison(photo1)
        viewModel.togglePhotoForComparison(photo2)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.runVisualAiAnalysis()
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.uiState.value.visualAnalysisResult
        assertNotNull(result)
        assertFalse(viewModel.uiState.value.isAnalyzingProgress)

        viewModel.clearVisualAiAnalysis()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.visualAnalysisResult)
    }
}
