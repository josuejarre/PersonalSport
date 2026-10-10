package com.example.personalsport.ui.supplement

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalsport.diet.DietGoal
import com.example.personalsport.diet.DietSupplementSyncManager
import com.example.personalsport.diet.DietSupplementSyncState
import com.example.personalsport.supplement.SupplementGoal
import com.example.personalsport.supplement.SupplementProduct
import com.example.personalsport.supplement.SupplementScraperEngine
import com.example.personalsport.supplement.SupplementVendor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class SupplementUiState(
    val selectedGoal: SupplementGoal = SupplementGoal.MUSCLE_GAIN,
    val searchQuery: String = "",
    val selectedVendor: SupplementVendor? = null,
    val products: List<SupplementProduct> = emptyList(),
    val aiRecommendedProducts: List<SupplementProduct> = emptyList(),
    val isScraping: Boolean = false,
    val statusMessage: String? = null,
    val selectedProductForComparison: SupplementProduct? = null,
    val dietSyncState: DietSupplementSyncState = DietSupplementSyncState()
)

class SupplementViewModel(
    private val scraperEngine: SupplementScraperEngine = SupplementScraperEngine,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplementUiState())
    val uiState: StateFlow<SupplementUiState> = _uiState.asStateFlow()

    init {
        loadProducts()

        viewModelScope.launch(ioDispatcher) {
            DietSupplementSyncManager.syncState.collect { sync ->
                val mappedGoal = when (sync.activeDietGoal) {
                    DietGoal.FAT_LOSS -> SupplementGoal.FAT_LOSS
                    DietGoal.MUSCLE_GAIN -> SupplementGoal.MUSCLE_GAIN
                    DietGoal.BODY_RECOMP -> SupplementGoal.MUSCLE_GAIN
                    DietGoal.ATHLETIC_PERFORMANCE -> SupplementGoal.ENDURANCE
                }
                _uiState.update { currentState ->
                    currentState.copy(
                        dietSyncState = sync,
                        selectedGoal = mappedGoal
                    )
                }
                loadProducts()
            }
        }
    }

    fun selectGoal(goal: SupplementGoal) {
        _uiState.update { currentState ->
            currentState.copy(selectedGoal = goal)
        }
        loadProducts()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { currentState ->
            currentState.copy(searchQuery = query)
        }
        loadProducts()
    }

    fun selectVendor(vendor: SupplementVendor?) {
        _uiState.update { currentState ->
            currentState.copy(selectedVendor = vendor)
        }
        loadProducts()
    }

    fun selectProductForComparison(product: SupplementProduct?) {
        _uiState.update { currentState ->
            currentState.copy(selectedProductForComparison = product)
        }
    }

    fun refreshScrapedPrices() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isScraping = true, statusMessage = "Buscando y comparando precios en Prozis, MyProtein, HSN, Decathlon y Bulk Powders...") }
            delay(1200)
            val query = _uiState.value.searchQuery
            val scrapedProducts = scraperEngine.simulateLiveWebScrape(query)

            val goal = _uiState.value.selectedGoal
            val vendor = _uiState.value.selectedVendor
            val filtered = scrapedProducts.filter { product ->
                val matchesGoal = product.primaryGoal == goal
                val matchesVendor = vendor == null || product.vendorOffers.any { it.vendor == vendor }
                matchesGoal && matchesVendor
            }

            val aiRecs = scraperEngine.getAiRecommendedProductsForGoal(goal)

            _uiState.update {
                it.copy(
                    isScraping = false,
                    products = filtered,
                    aiRecommendedProducts = aiRecs,
                    statusMessage = "¡Precios actualizados en vivo de las 5 principales tiendas!"
                )
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun loadProducts() {
        viewModelScope.launch(ioDispatcher) {
            val state = _uiState.value
            val filtered = scraperEngine.searchProducts(
                query = state.searchQuery,
                goalFilter = state.selectedGoal,
                vendorFilter = state.selectedVendor
            )
            val aiRecs = scraperEngine.getAiRecommendedProductsForGoal(state.selectedGoal)

            _uiState.update {
                it.copy(
                    products = filtered,
                    aiRecommendedProducts = aiRecs
                )
            }
        }
    }
}
