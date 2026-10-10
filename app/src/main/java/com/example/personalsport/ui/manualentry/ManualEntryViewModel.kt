package com.example.personalsport.ui.manualentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.personalsport.data.local.entity.BodyMetric
import com.example.personalsport.data.local.entity.WorkoutLog
import com.example.personalsport.data.model.GpsRouteUtils
import com.example.personalsport.data.repository.BodyMetricRepository
import com.example.personalsport.data.repository.WorkoutLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ManualEntryViewModel(
    private val workoutLogRepository: WorkoutLogRepository,
    private val bodyMetricRepository: BodyMetricRepository
) : ViewModel() {

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    fun addWorkoutLog(
        title: String,
        category: String,
        durationMinutes: Int,
        distanceKm: Float?,
        calories: Int?,
        notes: String,
        updateMetrics: Boolean,
        weightKg: Float?,
        bodyFatPct: Float?
    ) {
        if (title.isBlank() || durationMinutes <= 0) return

        viewModelScope.launch {
            val formattedNotes = buildString {
                append("Category: $category")
                if (distanceKm != null && distanceKm > 0) append(" · Distance: %.2f km".format(distanceKm))
                if (calories != null && calories > 0) append(" · Calories: $calories kcal")
                if (notes.isNotBlank()) append("\n$notes")
            }

            val routeGpsPoints = when {
                category.contains("aguas abiertas", ignoreCase = true) || category.contains("open water", ignoreCase = true) -> {
                    GpsRouteUtils.generateSampleOpenWaterSwimRouteJson()
                }
                category.contains("gps", ignoreCase = true) || category.contains("carrera", ignoreCase = true) || category.contains("running", ignoreCase = true) -> {
                    GpsRouteUtils.generateSampleRunRouteJson()
                }
                else -> null
            }

            workoutLogRepository.insert(
                WorkoutLog(
                    date = System.currentTimeMillis(),
                    workoutName = title.trim(),
                    durationMinutes = durationMinutes,
                    notes = formattedNotes,
                    source = "Manual Entry",
                    routeGpsPoints = routeGpsPoints
                )
            )

            if (updateMetrics && weightKg != null && weightKg > 0) {
                bodyMetricRepository.insert(
                    BodyMetric(
                        date = System.currentTimeMillis(),
                        weightKg = weightKg,
                        bodyFatPercentage = bodyFatPct
                    )
                )
            }

            _saveSuccess.value = true
        }
    }
}

class ManualEntryViewModelFactory(
    private val workoutLogRepository: WorkoutLogRepository,
    private val bodyMetricRepository: BodyMetricRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManualEntryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ManualEntryViewModel(workoutLogRepository, bodyMetricRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
