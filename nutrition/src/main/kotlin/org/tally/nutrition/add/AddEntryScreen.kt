package org.tally.nutrition.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import org.tally.domain.nutrition.RemoteFood
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.tally.domain.model.BaseUnit
import org.tally.domain.nutrition.Food
import org.tally.domain.nutrition.FoodWithPortions
import org.tally.domain.nutrition.NutritionCalc
import org.tally.domain.nutrition.Recipe
import org.tally.nutrition.R
import org.tally.nutrition.common.formatKcal
import org.tally.nutrition.common.formatNumber
import org.tally.nutrition.common.parseDecimal

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
    var tab by remember { mutableIntStateOf(0) }
    var recipeToLog by remember { mutableStateOf<Recipe?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            label = { Text(stringResource(R.string.add_search)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = if (tab == 2) ImeAction.Search else ImeAction.Default),
            keyboardActions = KeyboardActions(onSearch = { viewModel.submitOnline() }),
            modifier = Modifier.fillMaxWidth(),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            listOf(R.string.add_tab_foods, R.string.add_tab_recipes, R.string.add_tab_online)
                .forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = tab == index,
                        onClick = { tab = index },
                        shape = SegmentedButtonDefaults.itemShape(index, 3),
                    ) { Text(stringResource(label)) }
                }
        }
        when (tab) {
            0 -> TextButton(onClick = onNewFood) { Text(stringResource(R.string.add_new_food)) }
            1 -> TextButton(onClick = onNewRecipe) { Text(stringResource(R.string.add_new_recipe)) }
            else -> TextButton(
                onClick = viewModel::submitOnline,
                enabled = online != OnlineState.Loading && query.trim().length >= 3,
            ) { Text(stringResource(R.string.add_search_online)) }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            if (tab == 2) {
                onlineItems(online, onChoose = viewModel::chooseRemote)
            } else if (tab == 0) {
                items(state.foods, key = { "f-${it.id}" }) { food ->
                    ListItem(
                        headlineContent = { Text(food.name) },
                        supportingContent = {
                            Text(
                                listOfNotNull(
                                    food.brand,
                                    stringResource(
                                        if (food.baseUnit == BaseUnit.ML) R.string.per_100_ml else R.string.per_100_g,
                                        formatKcal(food.per100),
                                    ),
                                ).joinToString(" · "),
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { onEditFood(food.id) }) {
                                Icon(Icons.Filled.Create, contentDescription = stringResource(R.string.action_edit))
                            }
                        },
                        modifier = Modifier.clickable { viewModel.selectFood(food) },
                    )
                }
            } else {
                items(state.recipes, key = { "r-${it.id}" }) { recipe ->
                    ListItem(
                        headlineContent = { Text(recipe.name) },
                        supportingContent = {
                            Text(stringResource(R.string.recipe_per_serving, formatKcal(recipe.perServing)))
                        },
                        modifier = Modifier.clickable { recipeToLog = recipe },
                    )
                }
            }
        }
    }

    selected?.let { fw ->
        FoodQuantityDialog(
            data = fw,
            onDismiss = viewModel::clearSelection,
            onConfirm = { qty, portionId, count ->
                viewModel.addFood(fw.food, qty, portionId, count, onDone)
            },
        )
    }
    recipeToLog?.let { recipe ->
        RecipeServingsDialog(
            recipe = recipe,
            onDismiss = { recipeToLog = null },
            onConfirm = { servings -> viewModel.addRecipe(recipe, servings, onDone) },
        )
    }
}

@Composable
private fun FoodQuantityDialog(
    data: FoodWithPortions,
    onDismiss: () -> Unit,
    onConfirm: (quantityBase: Double, portionId: Long?, portionCount: Double?) -> Unit,
) {
    val food = data.food
    val unitLabel = if (food.baseUnit == BaseUnit.ML) "ml" else "g"
    val defaultPortion = data.portions.firstOrNull { it.isDefault }
    var portionId by remember { mutableStateOf(defaultPortion?.id) }
    var countText by remember { mutableStateOf("1") }
    var amountText by remember { mutableStateOf("100") }

    val portion = data.portions.firstOrNull { it.id == portionId }
    val count = parseDecimal(countText)
    val quantity = if (portion != null) count?.let { it * portion.quantityBase } else parseDecimal(amountText)
    val valid = quantity != null && quantity > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(food.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (data.portions.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = portion == null,
                            onClick = { portionId = null },
                            label = { Text(unitLabel) },
                        )
                        data.portions.forEach { p ->
                            FilterChip(
                                selected = portionId == p.id,
                                onClick = { portionId = p.id },
                                label = { Text(p.label) },
                            )
                        }
                    }
                }
                if (portion != null) {
                    OutlinedTextField(
                        value = countText,
                        onValueChange = { countText = it },
                        label = { Text(stringResource(R.string.add_portion_count)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                } else {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text(unitLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
                if (valid) {
                    val macros = NutritionCalc.macrosFor(food.per100, quantity)
                    Text(
                        stringResource(
                            R.string.add_preview,
                            formatNumber(quantity),
                            unitLabel,
                            formatKcal(macros),
                        ),
                        modifier = Modifier.align(Alignment.Start),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onConfirm(quantity!!, portion?.id, if (portion != null) count else null) },
            ) { Text(stringResource(R.string.action_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun RecipeServingsDialog(recipe: Recipe, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var text by remember { mutableStateOf("1") }
    val servings = parseDecimal(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(recipe.name) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.add_servings)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = {
            TextButton(enabled = servings != null && servings > 0, onClick = { onConfirm(servings!!) }) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.onlineItems(
    state: OnlineState,
    onChoose: (RemoteFood) -> Unit,
) {
    item(key = "online-status") {
        val message = when (state) {
            OnlineState.Idle -> R.string.online_hint
            OnlineState.Loading -> R.string.online_loading
            OnlineState.Empty -> R.string.online_empty
            OnlineState.Unavailable -> R.string.online_unavailable
            OnlineState.Disabled -> R.string.online_disabled
            is OnlineState.RateLimited, is OnlineState.Results -> null
        }
        if (message != null) {
            Text(stringResource(message), modifier = Modifier.padding(vertical = 8.dp))
        }
        if (state is OnlineState.RateLimited) {
            Text(
                stringResource(R.string.online_rate_limited, state.seconds),
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
    if (state is OnlineState.Results) {
        items(state.items, key = { "o-${it.barcode}" }) { item ->
            ListItem(
                headlineContent = { Text(item.name) },
                supportingContent = {
                    Text(
                        listOfNotNull(
                            item.brand,
                            stringResource(R.string.per_100_g, formatKcal(item.per100)),
                        ).joinToString(" · "),
                    )
                },
                modifier = Modifier.clickable { onChoose(item) },
            )
        }
        item(key = "online-attribution") {
            Text(
                stringResource(R.string.online_attribution),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
    }
}
