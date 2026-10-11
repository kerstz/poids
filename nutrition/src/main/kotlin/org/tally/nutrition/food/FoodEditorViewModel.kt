package org.tally.nutrition.food

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.tally.domain.model.BaseUnit
import org.tally.domain.nutrition.Food
import org.tally.domain.nutrition.FoodPortion
import org.tally.domain.nutrition.FoodRepository
import org.tally.domain.nutrition.FoodValidation
import org.tally.domain.nutrition.Macros
import org.tally.nutrition.FoodEditorRoute
import org.tally.nutrition.common.formatNumber
import org.tally.nutrition.common.parseDecimal

class PortionDraft(label: String = "", quantity: String = "") {
    var label by mutableStateOf(label)
    var quantity by mutableStateOf(quantity)
}

@HiltViewModel
class FoodEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FoodRepository,
) : ViewModel() {

    private val foodId = savedStateHandle.toRoute<FoodEditorRoute>().foodId

    val isEditing: Boolean get() = foodId != 0L

    var name by mutableStateOf("")
    var brand by mutableStateOf("")
    var baseUnit by mutableStateOf(BaseUnit.G)
    var energy by mutableStateOf("")
    var protein by mutableStateOf("")
    var carbs by mutableStateOf("")
    var fat by mutableStateOf("")
    var showError by mutableStateOf(false)
    val portions = mutableStateListOf<PortionDraft>()

    init {
        if (isEditing) {
            viewModelScope.launch {
                repository.get(foodId)?.let { data ->
                    val f = data.food
                    name = f.name
                    brand = f.brand.orEmpty()
                    baseUnit = f.baseUnit
                    energy = formatNumber(f.per100.energyKcal, 1)
                    protein = formatNumber(f.per100.proteinG, 1)
                    carbs = formatNumber(f.per100.carbsG, 1)
                    fat = formatNumber(f.per100.fatG, 1)
                    portions.clear()
                    portions.addAll(data.portions.map { PortionDraft(it.label, formatNumber(it.quantityBase, 1)) })
                }
            }
        }
    }

    fun save(onDone: () -> Unit) {
        val macros = Macros(
            energyKcal = parseDecimal(energy) ?: -1.0,
            proteinG = parseDecimal(protein) ?: 0.0,
            carbsG = parseDecimal(carbs) ?: 0.0,
            fatG = parseDecimal(fat) ?: 0.0,
        )
        val food = Food(
            id = foodId,
            name = name,
            brand = brand.ifBlank { null },
            baseUnit = baseUnit,
            per100 = macros,
        )
        val validPortions = portions.mapNotNull { draft ->
            val q = parseDecimal(draft.quantity)
            if (draft.label.isBlank() || q == null || q <= 0) null else FoodPortion(label = draft.label.trim(), quantityBase = q)
        }.mapIndexed { index, p -> p.copy(isDefault = index == 0) }
        if (!FoodValidation.isValid(food)) {
            showError = true
            return
        }
        viewModelScope.launch {
            repository.save(food, validPortions)
            onDone()
        }
    }

    fun archive(onDone: () -> Unit) {
        if (!isEditing) return
        viewModelScope.launch {
            repository.archive(foodId)
            onDone()
        }
    }
}
