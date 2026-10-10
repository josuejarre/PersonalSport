package com.example.personalsport.ui.progress

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.personalsport.ai.VisualAIBodyAnalyzer
import com.example.personalsport.ai.VisualAnalysisResult
import com.example.personalsport.data.local.entity.BodyMetric
import com.example.personalsport.data.local.entity.ProgressPhoto
import com.example.personalsport.data.repository.BodyMetricRepository
import com.example.personalsport.data.repository.ProgressPhotoRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@Immutable
data class CameraCaptureState(
    val isCameraMode: Boolean = false,
    val temporaryPhotoUri: Uri? = null,
    val photoNotesInput: String = ""
)

@Immutable
data class ProgressPhotoUiState(
    val photos: List<ProgressPhoto> = emptyList(),
    val selectedForComparison: List<ProgressPhoto> = emptyList(),
    val selectedPhotoForDetail: ProgressPhoto? = null,
    val cameraState: CameraCaptureState = CameraCaptureState(),
    val isAnalyzingProgress: Boolean = false,
    val visualAnalysisResult: VisualAnalysisResult? = null
)

class ProgressPhotoViewModel(
    private val repository: ProgressPhotoRepository,
    private val bodyMetricRepository: BodyMetricRepository? = null,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _selectedComparisonIds = MutableStateFlow<Set<Int>>(emptySet())
    private val _selectedPhotoForDetail = MutableStateFlow<ProgressPhoto?>(null)
    private val _isCameraMode = MutableStateFlow(false)
    private val _temporaryPhotoUri = MutableStateFlow<Uri?>(null)
    private val _photoNotesInput = MutableStateFlow("")
    private val _isAnalyzingProgress = MutableStateFlow(false)
    private val _visualAnalysisResult = MutableStateFlow<VisualAnalysisResult?>(null)

    private val _cameraState = combine(
        _isCameraMode,
        _temporaryPhotoUri,
        _photoNotesInput
    ) { isCamera, tempUri, notes ->
        CameraCaptureState(isCamera, tempUri, notes)
    }

    private val _analysisState = combine(
        _isAnalyzingProgress,
        _visualAnalysisResult
    ) { isAnalyzing, result ->
        Pair(isAnalyzing, result)
    }

    val uiState: StateFlow<ProgressPhotoUiState> = combine(
        repository.allProgressPhotos,
        _selectedComparisonIds,
        _selectedPhotoForDetail,
        _cameraState,
        _analysisState
    ) { photos, comparisonIds, detailPhoto, camera, analysis ->
        val selectedPhotos = photos.filter { it.id in comparisonIds }
            .sortedBy { it.date }

        ProgressPhotoUiState(
            photos = photos,
            selectedForComparison = selectedPhotos,
            selectedPhotoForDetail = detailPhoto,
            cameraState = camera,
            isAnalyzingProgress = analysis.first,
            visualAnalysisResult = analysis.second
        )
    }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProgressPhotoUiState()
        )

    fun setCameraMode(active: Boolean) {
        _isCameraMode.value = active
        if (!active) {
            _temporaryPhotoUri.value = null
            _photoNotesInput.value = ""
        }
    }

    fun onPhotoCaptured(uri: Uri) {
        _temporaryPhotoUri.value = uri
    }

    fun setPhotoNotesInput(notes: String) {
        _photoNotesInput.value = notes
    }

    fun saveCapturedPhoto(notes: String) {
        val uri = _temporaryPhotoUri.value ?: return
        viewModelScope.launch(ioDispatcher) {
            repository.insert(
                ProgressPhoto(
                    date = System.currentTimeMillis(),
                    photoUri = uri.toString(),
                    notes = notes.trim()
                )
            )
            _temporaryPhotoUri.value = null
            _photoNotesInput.value = ""
            _isCameraMode.value = false
        }
    }

    fun cancelCapturedPhoto() {
        _temporaryPhotoUri.value = null
        _photoNotesInput.value = ""
    }

    fun togglePhotoForComparison(photo: ProgressPhoto) {
        val current = _selectedComparisonIds.value.toMutableSet()
        if (current.contains(photo.id)) {
            current.remove(photo.id)
        } else {
            if (current.size >= 2) {
                val list = current.toList()
                current.clear()
                current.add(list.last())
                current.add(photo.id)
            } else {
                current.add(photo.id)
            }
        }
        _selectedComparisonIds.value = current
        _visualAnalysisResult.value = null
    }

    fun clearComparisonSelection() {
        _selectedComparisonIds.value = emptySet()
        _visualAnalysisResult.value = null
    }

    fun setSelectedPhotoForDetail(photo: ProgressPhoto?) {
        _selectedPhotoForDetail.value = photo
    }

    fun updatePhotoNotes(photo: ProgressPhoto, newNotes: String) {
        viewModelScope.launch(ioDispatcher) {
            val updated = photo.copy(notes = newNotes.trim())
            repository.update(updated)
            if (_selectedPhotoForDetail.value?.id == photo.id) {
                _selectedPhotoForDetail.value = updated
            }
        }
    }

    fun deletePhoto(photo: ProgressPhoto) {
        viewModelScope.launch(ioDispatcher) {
            repository.delete(photo)
            if (_selectedPhotoForDetail.value?.id == photo.id) {
                _selectedPhotoForDetail.value = null
            }
            val comparisonSet = _selectedComparisonIds.value.toMutableSet()
            if (comparisonSet.contains(photo.id)) {
                comparisonSet.remove(photo.id)
                _selectedComparisonIds.value = comparisonSet
            }
        }
    }

    fun runVisualAiAnalysis(context: Context? = null) {
        val comparison = uiState.value.selectedForComparison
        if (comparison.size < 2) return

        val beforePhoto = comparison[0]
        val afterPhoto = comparison[1]

        viewModelScope.launch {
            _isAnalyzingProgress.value = true

            val allMetrics = withContext(ioDispatcher) {
                bodyMetricRepository?.allBodyMetrics?.firstOrNull() ?: emptyList()
            }
            val beforeMetric = findClosestMetric(allMetrics, beforePhoto.date)
            val afterMetric = findClosestMetric(allMetrics, afterPhoto.date)

            val result = withContext(defaultDispatcher) {
                VisualAIBodyAnalyzer.analyzeProgressPhotos(
                    beforePhoto = beforePhoto,
                    afterPhoto = afterPhoto,
                    beforeMetric = beforeMetric,
                    afterMetric = afterMetric,
                    context = context
                )
            }

            _visualAnalysisResult.value = result
            _isAnalyzingProgress.value = false
        }
    }

    fun clearVisualAiAnalysis() {
        _visualAnalysisResult.value = null
    }

    private fun findClosestMetric(metrics: List<BodyMetric>, photoDate: Long): BodyMetric? {
        if (metrics.isEmpty()) return null
        val maxDiffMillis = 14 * 86400000L
        val closest = metrics.minByOrNull { abs(it.date - photoDate) } ?: return null
        return if (abs(closest.date - photoDate) <= maxDiffMillis) closest else null
    }
}

class ProgressPhotoViewModelFactory(
    private val repository: ProgressPhotoRepository,
    private val bodyMetricRepository: BodyMetricRepository? = null,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgressPhotoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProgressPhotoViewModel(repository, bodyMetricRepository, defaultDispatcher, ioDispatcher) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
