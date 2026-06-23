package com.poids.ui.nutrition.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poids.domain.model.FoodItem
import com.poids.domain.model.FoodSource
import com.poids.domain.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManualFoodForm(
    val name: String = "",
    val kcal: String = "",
    val carbs: String = "",
    val fat: String = "",
    val protein: String = "",
    val error: String? = null,
    val savedFoodId: Long? = null,
)

@HiltViewModel
class ManualFoodViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
) : ViewModel() {

    private val _form = MutableStateFlow(ManualFoodForm())
    val form: StateFlow<ManualFoodForm> = _form.asStateFlow()

    fun onNameChange(v: String) = _form.update { it.copy(name = v, error = null) }
    fun onKcalChange(v: String) = _form.update { it.copy(kcal = v, error = null) }
    fun onCarbsChange(v: String) = _form.update { it.copy(carbs = v, error = null) }
    fun onFatChange(v: String) = _form.update { it.copy(fat = v, error = null) }
    fun onProteinChange(v: String) = _form.update { it.copy(protein = v, error = null) }

    fun save() {
        val s = _form.value
        val name = s.name.trim()
        val kcal = s.kcal.toFrenchDouble()
        when {
            name.isBlank() -> return fail("Nom requis.")
            kcal == null || kcal < 0 -> return fail("Calories (pour 100 g) invalides.")
        }
        val food = FoodItem(
            name = name,
            source = FoodSource.MANUAL,
            energyKcal100 = kcal!!,
            carbs100 = s.carbs.toFrenchDouble(),
            fat100 = s.fat.toFrenchDouble(),
            protein100 = s.protein.toFrenchDouble(),
        )
        viewModelScope.launch {
            val id = foodRepository.saveManual(food)
            _form.update { it.copy(savedFoodId = id) }
        }
    }

    private fun fail(message: String) = _form.update { it.copy(error = message) }
}

private fun String.toFrenchDouble(): Double? =
    trim().replace(',', '.').toDoubleOrNull()
