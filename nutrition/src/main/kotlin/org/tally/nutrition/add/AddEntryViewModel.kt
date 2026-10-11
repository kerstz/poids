package org.tally.nutrition.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.tally.core.barcode.Gtin
import org.tally.domain.nutrition.BarcodeLookup
import org.tally.domain.nutrition.CiqualFood
import org.tally.domain.nutrition.CiqualRepository
import org.tally.domain.nutrition.ImportCiqualFood
import org.tally.domain.nutrition.FoodRemoteSource
import org.tally.domain.nutrition.ImportRemoteFood
import org.tally.domain.nutrition.RemoteFood
import org.tally.domain.nutrition.RemoteResult
import org.tally.domain.nutrition.ResolveBarcode
import org.tally.domain.settings.SettingsRepository
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

/** État de la recherche Open Food Facts (onglet « En ligne »). */
sealed interface OnlineState {
    data object Idle : OnlineState
    data object Loading : OnlineState
    data class Results(val items: List<RemoteFood>) : OnlineState
    data object Empty : OnlineState
    data class RateLimited(val seconds: Int) : OnlineState
    data object Unavailable : OnlineState
    data object Disabled : OnlineState
}

data class AddEntryUiState(
    val foods: List<Food> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class AddEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foodRepository: FoodRepository,
    recipeRepository: RecipeRepository,
    private val logFood: LogFood,
    private val logRecipe: LogRecipe,
    private val remote: FoodRemoteSource,
    private val resolveBarcode: ResolveBarcode,
    private val importRemoteFood: ImportRemoteFood,
    private val ciqual: CiqualRepository,
    private val importCiqualFood: ImportCiqualFood,
    private val settings: SettingsRepository,
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

    /** Table Ciqual embarquée : recherche locale, au fil de la frappe (aucun réseau). */
    val ciqualResults: StateFlow<List<CiqualFood>> = query
        .flatMapLatest { q -> flow { emit(if (q.trim().length >= MIN_CIQUAL_LENGTH) ciqual.search(q) else emptyList()) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedFood = MutableStateFlow<FoodWithPortions?>(null)
    val selectedFood: StateFlow<FoodWithPortions?> = _selectedFood

    private val _online = MutableStateFlow<OnlineState>(OnlineState.Idle)
    val online: StateFlow<OnlineState> = _online

    /**
     * Soumissions explicites uniquement : OFF interdit la recherche au fil de la frappe
     * (10 req/min). Le debounce absorbe les doubles appuis ; `collectLatest` annule la requête
     * précédente.
     */
    private val submissions = MutableSharedFlow<String>(extraBufferCapacity = 1)

    init {
        viewModelScope.launch {
            submissions.debounce(SUBMIT_DEBOUNCE_MS).collectLatest { runOnlineSearch(it) }
        }
    }

    fun submitOnline() {
        val q = query.value.trim()
        if (q.length < MIN_ONLINE_LENGTH) return
        _online.value = OnlineState.Loading
        submissions.tryEmit(q)
    }

    private suspend fun runOnlineSearch(q: String) {
        if (!settings.offEnabled.first()) {
            _online.value = OnlineState.Disabled
            return
        }
        _online.value = if (Gtin.looksLikeBarcode(q)) {
            when (val lookup = resolveBarcode(q)) {
                is BarcodeLookup.Local -> {
                    selectFood(lookup.food)
                    OnlineState.Idle
                }
                is BarcodeLookup.Remote -> OnlineState.Results(listOf(lookup.food))
                BarcodeLookup.NotFound -> OnlineState.Empty
                is BarcodeLookup.RateLimited -> OnlineState.RateLimited(secondsFor(lookup.retryAfterMs))
                BarcodeLookup.Unavailable -> OnlineState.Unavailable
            }
        } else {
            when (val result = remote.search(q)) {
                is RemoteResult.Success ->
                    if (result.value.isEmpty()) OnlineState.Empty else OnlineState.Results(result.value)
                RemoteResult.NotFound -> OnlineState.Empty
                is RemoteResult.RateLimited -> OnlineState.RateLimited(secondsFor(result.retryAfterMs))
                RemoteResult.Unavailable -> OnlineState.Unavailable
                RemoteResult.Disabled -> OnlineState.Disabled
            }
        }
    }

    /** Copie le produit distant dans `food` puis ouvre le choix de quantité. */
    fun chooseRemote(item: RemoteFood) {
        if (importing) return // double appui : un seul import à la fois
        importing = true
        viewModelScope.launch {
            try {
                importRemoteFood(item)?.let { selectFood(it) } ?: run { _online.value = OnlineState.Unavailable }
            } finally {
                importing = false
            }
        }
    }

    private var importing = false

    /** Copie l'entrée Ciqual dans `food` puis ouvre le choix de quantité. */
    fun chooseCiqual(item: CiqualFood) {
        if (importing) return
        importing = true
        viewModelScope.launch {
            try {
                importCiqualFood(item)?.let { selectFood(it) }
            } finally {
                importing = false
            }
        }
    }

    private fun secondsFor(ms: Long): Int = ((ms + 999) / 1_000).toInt().coerceAtLeast(1)

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

    private companion object {
        const val SUBMIT_DEBOUNCE_MS = 400L
        const val MIN_ONLINE_LENGTH = 3
        const val MIN_CIQUAL_LENGTH = 2
    }

    fun addRecipe(recipe: Recipe, servings: Double, onDone: () -> Unit) {
        viewModelScope.launch {
            logRecipe(route.epochDay, route.mealSlotId, recipe, servings)
            onDone()
        }
    }
}
