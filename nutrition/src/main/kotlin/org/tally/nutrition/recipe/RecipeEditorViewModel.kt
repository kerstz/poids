package org.tally.nutrition.recipe

import androidx.compose.runtime.derivedStateOf
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.tally.domain.nutrition.Food
import org.tally.domain.nutrition.FoodRepository
import org.tally.domain.nutrition.Macros
import org.tally.domain.nutrition.NutritionCalc
import org.tally.domain.nutrition.Recipe
import org.tally.domain.nutrition.RecipeIngredient
import org.tally.domain.nutrition.RecipeRepository
import org.tally.domain.nutrition.SaveRecipe
import org.tally.nutrition.RecipeEditorRoute
import org.tally.nutrition.common.formatNumber
import org.tally.nutrition.common.parseDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecipeEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipes: RecipeRepository,
    private val saveRecipe: SaveRecipe,
    private val foods: FoodRepository,
) : ViewModel() {

    private val recipeId = savedStateHandle.toRoute<RecipeEditorRoute>().recipeId
    val isEditing: Boolean get() = recipeId != 0L

    var name by mutableStateOf("")
    var servings by mutableStateOf("1")
    var showError by mutableStateOf(false)
    val ingredients = mutableStateListOf<RecipeIngredient>()

    val perServing: Macros by derivedStateOf {
        val total = ingredients.fold(Macros.ZERO) { acc, i -> acc + i.macros }
        val s = parseDecimal(servings)
        if (s != null && s > 0) total * (1.0 / s) else Macros.ZERO
    }

    private val foodQuery = MutableStateFlow("")
    val foodResults: StateFlow<List<Food>> = foodQuery
        .flatMapLatest { foods.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val foodQueryText: StateFlow<String> = foodQuery

    init {
        if (isEditing) {
            viewModelScope.launch {
                recipes.get(recipeId)?.let { r ->
                    name = r.name
                    servings = formatNumber(r.servings, 1)
                    ingredients.clear()
                    ingredients.addAll(r.ingredients)
                }
            }
        }
    }

    fun setFoodQuery(value: String) {
        foodQuery.value = value
    }

    fun addIngredient(food: Food, quantityBase: Double) {
        ingredients.add(RecipeIngredient(food.id, food.name, quantityBase, food.per100, food.per100Detail))
    }

    fun removeIngredient(index: Int) {
        if (index in ingredients.indices) ingredients.removeAt(index)
    }

    fun save(onDone: () -> Unit) {
        val recipe = Recipe(
            id = recipeId,
            name = name,
            servings = parseDecimal(servings) ?: 0.0,
            ingredients = ingredients.toList(),
        )
        viewModelScope.launch {
            if (saveRecipe(recipe) == null) showError = true else onDone()
        }
    }

    fun archive(onDone: () -> Unit) {
        if (!isEditing) return
        viewModelScope.launch {
            recipes.archive(recipeId)
            onDone()
        }
    }

    /** Valeur utilisée par l'écran pour le calcul d'aperçu d'un ingrédient. */
    fun previewKcal(food: Food, quantityBase: Double): Double =
        NutritionCalc.macrosFor(food.per100, quantityBase).energyKcal
}
