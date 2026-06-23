package com.poids.ui.nutrition.addmeal

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poids.domain.model.FoodItem
import com.poids.domain.model.MealType
import com.poids.domain.repository.FoodRepository
import com.poids.domain.repository.MealRepository
import com.poids.util.MealPhotoStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class AddMealForm(
    val quantityG: String = "100",
    val mealType: MealType = defaultMealTypeNow(),
    val photoPath: String? = null,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class AddMealViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    foodRepository: FoodRepository,
    private val mealRepository: MealRepository,
    private val photoStorage: MealPhotoStorage,
) : ViewModel() {

    private val foodId: Long = checkNotNull(savedStateHandle.get<Long>("foodId"))

    val food: StateFlow<FoodItem?> = foodRepository.observeById(foodId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _form = MutableStateFlow(AddMealForm())
    val form: StateFlow<AddMealForm> = _form.asStateFlow()

    fun onQuantityChange(v: String) = _form.update { it.copy(quantityG = v, error = null) }
    fun onMealTypeChange(v: MealType) = _form.update { it.copy(mealType = v) }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            val path = photoStorage.savePhoto(uri)
            _form.update { it.copy(photoPath = path) }
        }
    }

    fun save() {
        val qty = _form.value.quantityG.trim().replace(',', '.').toDoubleOrNull()
        if (qty == null || qty <= 0) {
            _form.update { it.copy(error = "Quantité invalide.") }
            return
        }
        viewModelScope.launch {
            mealRepository.addMeal(
                foodId = foodId,
                mealType = _form.value.mealType,
                quantityG = qty,
                dateTime = LocalDateTime.now(),
                photoPath = _form.value.photoPath,
            )
            _form.update { it.copy(saved = true) }
        }
    }
}

/** Type de repas par défaut selon l'heure. */
private fun defaultMealTypeNow(): MealType {
    val hour = LocalTime.now().hour
    return when {
        hour < 11 -> MealType.BREAKFAST
        hour < 15 -> MealType.LUNCH
        hour < 21 -> MealType.DINNER
        else -> MealType.SNACK
    }
}
