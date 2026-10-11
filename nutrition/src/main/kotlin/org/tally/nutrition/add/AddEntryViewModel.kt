package org.tally.nutrition.add

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.tally.domain.nutrition.Food
import org.tally.domain.nutrition.FoodRepository
import org.tally.domain.nutrition.FoodWithPortions
import org.tally.domain.nutrition.LogFood
import org.tally.domain.nutrition.LogRecipe
import org.tally.domain.nutrition.Recipe
import org.tally.domain.nutrition.RecipeRepository
import org.tally.nutrition.AddEntryRoute

data class AddEntryUiState(
    val foods: List<Food> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foodRepository: FoodRepository,
    recipeRepository: RecipeRepository,
    private val logFood: LogFood,
    private val logRecipe: LogRecipe,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<AddEntryRoute>()
    private val query = MutableStateFlow("")

    val uiState: StateFlow<AddEntryUiState> = combine(
        query.flatMapLatest { foodRepository.search(it) },
        recipeRepository.observeRecipes(),
        query,
    ) { foods, recipes, q ->
        AddEntryUiState(
            foods = foods,
            recipes = recipes.filter { it.name.contains(q.trim(), ignoreCase = true) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddEntryUiState())

    val queryText: StateFlow<String> = query

    private val _selectedFood = MutableStateFlow<FoodWithPortions?>(null)
    val selectedFood: StateFlow<FoodWithPortions?> = _selectedFood

    fun setQuery(value: String) {
        query.value = value
    }

    fun selectFood(food: Food) {
        viewModelScope.launch { _selectedFood.value = foodRepository.get(food.id) }
    }

    fun clearSelection() {
        _selectedFood.value = null
    }

    fun addFood(food: Food, quantityBase: Double, portionId: Long?, portionCount: Double?, onDone: () -> Unit) {
        viewModelScope.launch {
            logFood(route.epochDay, route.mealSlotId, food, quantityBase, portionId, portionCount)
            onDone()
        }
    }

    fun addRecipe(recipe: Recipe, servings: Double, onDone: () -> Unit) {
        viewModelScope.launch {
            logRecipe(route.epochDay, route.mealSlotId, recipe, servings)
            onDone()
        }
    }
}
