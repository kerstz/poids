// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.nutrition.food

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.tally.domain.model.BaseUnit
import org.tally.domain.nutrition.Food
import org.tally.domain.nutrition.edited
import org.tally.domain.nutrition.FoodPortion
import org.tally.domain.nutrition.FoodRepository
import org.tally.domain.nutrition.Macros
import org.tally.domain.nutrition.SaveFood
import org.tally.nutrition.FoodEditorRoute
import org.tally.nutrition.common.formatNumber
import org.tally.nutrition.common.parseDecimal

class PortionDraft(label: String = "", quantity: String = "", private val onChange: () -> Unit = {}) {
    var label by mutableStateOf(label)
        private set
    var quantity by mutableStateOf(quantity)
        private set

    fun updateLabel(value: String) {
        label = value
        onChange()
    }

    fun updateQuantity(value: String) {
        quantity = value
        onChange()
    }
}

/**
 * Brouillon conservé dans [SavedStateHandle] : survit à la rotation et à la mort du processus.
 * Les valeurs chargées depuis la base ne sont lues qu'une fois (clé `loaded`).
 */
@OptIn(SavedStateHandleSaveableApi::class)
@HiltViewModel
class FoodEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: FoodRepository,
    private val saveFood: SaveFood,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<FoodEditorRoute>()
    private val foodId = route.foodId

    val isEditing: Boolean get() = foodId != 0L

    var name by savedStateHandle.saveable { mutableStateOf("") }
    var brand by savedStateHandle.saveable { mutableStateOf("") }
    var baseUnit by savedStateHandle.saveable { mutableStateOf(BaseUnit.G) }
    var energy by savedStateHandle.saveable { mutableStateOf("") }
    var protein by savedStateHandle.saveable { mutableStateOf("") }
    var carbs by savedStateHandle.saveable { mutableStateOf("") }
    var fat by savedStateHandle.saveable { mutableStateOf("") }
    var showError by mutableStateOf(false)
    val portions = mutableStateListOf<PortionDraft>()

    init {
        val saved = savedStateHandle.get<ArrayList<String>>(KEY_PORTIONS)
        if (saved != null) {
            portions.addAll(saved.map(::decodePortion))
        } else if (isEditing) {
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
                    portions.addAll(
                        data.portions.map { PortionDraft(it.label, formatNumber(it.quantityBase, 1), ::persistPortions) },
                    )
                    persistPortions()
                }
            }
        }
    }

    fun addPortion() {
        portions.add(PortionDraft(onChange = ::persistPortions))
        persistPortions()
    }

    private fun persistPortions() {
        savedStateHandle[KEY_PORTIONS] = ArrayList(portions.map { "${it.label}$SEP${it.quantity}" })
    }

    private fun decodePortion(raw: String): PortionDraft {
        val label = raw.substringBefore(SEP)
        val quantity = raw.substringAfter(SEP, "")
        return PortionDraft(label, quantity, ::persistPortions)
    }

    fun save(onDone: () -> Unit) {
        val per100 = Macros(
            energyKcal = parseDecimal(energy) ?: -1.0,
            proteinG = parseDecimal(protein) ?: 0.0,
            carbsG = parseDecimal(carbs) ?: 0.0,
            fatG = parseDecimal(fat) ?: 0.0,
        )
        val validPortions = portions.mapNotNull { draft ->
            val q = parseDecimal(draft.quantity)
            if (draft.label.isBlank() || q == null || q <= 0) {
                null
            } else {
                FoodPortion(label = draft.label.trim(), quantityBase = q)
            }
        }.mapIndexed { index, p -> p.copy(isDefault = index == 0) }
        viewModelScope.launch {
            // Repartir de l'aliment stocké : code-barres, source, détail et densité ne sont pas éditables ici.
            val base = if (isEditing) repository.get(foodId)?.food else null
            val food = Food.edited(base, foodId, name, brand.ifBlank { null }, baseUnit, per100, barcode = route.barcode)
            if (saveFood(food, validPortions) == null) showError = true else onDone()
        }
    }

    fun archive(onDone: () -> Unit) {
        if (!isEditing) return
        viewModelScope.launch {
            repository.archive(foodId)
            onDone()
        }
    }

    private companion object {
        const val KEY_PORTIONS = "portions"
        const val SEP = '\u0001'
    }
}
