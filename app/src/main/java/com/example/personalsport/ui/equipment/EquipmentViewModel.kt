package com.example.personalsport.ui.equipment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.personalsport.data.local.entity.Equipment
import com.example.personalsport.data.repository.EquipmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EquipmentUiState(
    val savedEquipment: List<Equipment> = emptyList(),
    val selectedBrandFilter: String? = null,
    val searchQuery: String = "",
    val catalogItems: List<CatalogItem> = BrandCatalog.catalogItems
)

class EquipmentViewModel(
    private val repository: EquipmentRepository
) : ViewModel() {

    private val _selectedBrandFilter = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<EquipmentUiState> = combine(
        repository.allEquipment,
        _selectedBrandFilter,
        _searchQuery
    ) { saved, brandFilter, query ->
        val filteredCatalog = BrandCatalog.catalogItems.filter { item ->
            val matchesBrand = brandFilter == null || item.brand.equals(brandFilter, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    item.brand.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true)
            matchesBrand && matchesQuery
        }

        EquipmentUiState(
            savedEquipment = saved,
            selectedBrandFilter = brandFilter,
            searchQuery = query,
            catalogItems = filteredCatalog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EquipmentUiState()
    )

    fun selectBrandFilter(brand: String?) {
        _selectedBrandFilter.value = brand
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addCatalogItemToGym(item: CatalogItem) {
        viewModelScope.launch {
            repository.insert(
                Equipment(
                    name = item.name,
                    description = "[${item.brand}] ${item.category} · ${item.description}"
                )
            )
        }
    }

    fun addCustomEquipment(name: String, brand: String, category: String, notes: String) {
        if (name.isBlank()) return
        val brandTag = if (brand.isNotBlank()) "[$brand] " else ""
        val categoryTag = if (category.isNotBlank()) "$category · " else ""
        val fullDesc = "$brandTag$categoryTag$notes".trim()

        viewModelScope.launch {
            repository.insert(
                Equipment(
                    name = name.trim(),
                    description = if (fullDesc.isBlank()) null else fullDesc
                )
            )
        }
    }

    fun deleteEquipment(equipment: Equipment) {
        viewModelScope.launch {
            repository.delete(equipment)
        }
    }
}

class EquipmentViewModelFactory(
    private val repository: EquipmentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EquipmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EquipmentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
