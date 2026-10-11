package org.tally.nutrition.add

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.tally.domain.nutrition.Recipe

@Composable
fun AddEntryScreen(
    onDone: () -> Unit,
    onNewFood: () -> Unit,
    onEditFood: (Long) -> Unit,
    onNewRecipe: () -> Unit,
    viewModel: AddEntryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.queryText.collectAsStateWithLifecycle()
    val selected by viewModel.selectedFood.collectAsStateWithLifecycle()
    val online by viewModel.online.collectAsStateWithLifecycle()
    val ciqual by viewModel.ciqualResults.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var recipeToLog by remember { mutableStateOf<Recipe?>(null) }

    AddEntryContent(
        foods = state.foods,
        recipes = state.recipes,
        query = query,
        tab = tab,
        online = online,
        selectedFood = selected,
        recipeToLog = recipeToLog,
        onQueryChange = viewModel::setQuery,
        onTabChange = { tab = it },
        onSubmitOnline = viewModel::submitOnline,
        onSelectFood = viewModel::selectFood,
        onChooseRemote = viewModel::chooseRemote,
        ciqual = ciqual,
        onChooseCiqual = viewModel::chooseCiqual,
        onSelectRecipe = { recipeToLog = it },
        onNewFood = onNewFood,
        onEditFood = onEditFood,
        onNewRecipe = onNewRecipe,
        onDismissFood = viewModel::clearSelection,
        onDismissRecipe = { recipeToLog = null },
        onAddFood = { food, qty, portionId, count -> viewModel.addFood(food, qty, portionId, count, onDone) },
        onAddRecipe = { recipe, servings -> viewModel.addRecipe(recipe, servings, onDone) },
    )
}
