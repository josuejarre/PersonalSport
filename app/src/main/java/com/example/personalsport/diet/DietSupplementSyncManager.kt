package com.example.personalsport.diet

import com.example.personalsport.ai.SupplementTimingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DietSupplementSyncState(
    val activeDietGoal: DietGoal = DietGoal.BODY_RECOMP,
    val isSynced: Boolean = true,
    val lastSyncTimestamp: String = "Hoy",
    val recommendedSupplements: List<SupplementTimingItem> = emptyList()
)

object DietSupplementSyncManager {
    private val _syncState = MutableStateFlow(DietSupplementSyncState())
    val syncState: StateFlow<DietSupplementSyncState> = _syncState.asStateFlow()

    fun updateSyncState(goal: DietGoal, supplements: List<SupplementTimingItem>) {
        _syncState.value = DietSupplementSyncState(
            activeDietGoal = goal,
            isSynced = true,
            lastSyncTimestamp = "Hoy",
            recommendedSupplements = supplements
        )
    }
}
