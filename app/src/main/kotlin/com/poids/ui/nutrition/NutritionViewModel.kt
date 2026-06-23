package com.poids.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poids.domain.model.LoggedMeal
import com.poids.domain.repository.FoodLookupResult
import com.poids.domain.repository.FoodRepository
import com.poids.domain.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class NutritionUiState(
    val meals: List<LoggedMeal> = emptyList(),
    val totalKcal: Double = 0.0,
)

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val foodRepository: FoodRepository,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    val uiState: StateFlow<NutritionUiState> = mealRepository.observeMealsForDate(today)
        .map { meals -> NutritionUiState(meals = meals, totalKcal = meals.sumOf { it.kcal }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionUiState())

    private val _scanEvents = MutableSharedFlow<FoodLookupResult>(extraBufferCapacity = 1)
    val scanEvents: SharedFlow<FoodLookupResult> = _scanEvents.asSharedFlow()

    /** Résout un code-barres scanné (cache puis API) et émet le résultat à l'UI. */
    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            _scanEvents.emit(foodRepository.lookupByBarcode(barcode))
        }
    }

    fun deleteMeal(id: Long) {
        viewModelScope.launch { mealRepository.deleteMeal(id) }
    }
}
