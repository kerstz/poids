// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.nutrition.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import org.tally.coreui.component.DataRow
import org.tally.coreui.component.EmptyState
import org.tally.coreui.component.ErrorBanner
import org.tally.coreui.component.QuantitySheet
import org.tally.coreui.icon.TallyIcons
import org.tally.coreui.theme.TallyTheme
import org.tally.domain.model.BaseUnit
import org.tally.domain.nutrition.CiqualFood
import org.tally.domain.nutrition.Food
import org.tally.domain.nutrition.FoodWithPortions
import org.tally.domain.nutrition.NutritionCalc
import org.tally.domain.nutrition.Recipe
import org.tally.domain.nutrition.RemoteFood
import org.tally.nutrition.R
import org.tally.nutrition.common.formatKcal
import org.tally.nutrition.common.formatNumber
import org.tally.nutrition.common.parseDecimal

private const val TAB_FOODS = 0
private const val TAB_RECIPES = 1
private const val TAB_ONLINE = 2
private const val TAB_CIQUAL = 3
private const val MIN_CIQUAL_QUERY = 2
private const val MIN_ONLINE_QUERY = 3

/** Écran d'ajout sans état : testable en capture. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddEntryContent(
    foods: List<Food>,
    recipes: List<Recipe>,
    query: String,
    tab: Int,
    online: OnlineState,
    selectedFood: FoodWithPortions?,
    recipeToLog: Recipe?,
    onQueryChange: (String) -> Unit,
    onTabChange: (Int) -> Unit,
    onSubmitOnline: () -> Unit,
    onSelectFood: (Food) -> Unit,
    onChooseRemote: (RemoteFood) -> Unit,
    onSelectRecipe: (Recipe) -> Unit,
    onNewFood: () -> Unit,
    onEditFood: (Long) -> Unit,
    onNewRecipe: () -> Unit,
    onDismissFood: () -> Unit,
    onDismissRecipe: () -> Unit,
    onAddFood: (Food, quantityBase: Double, portionId: Long?, portionCount: Double?) -> Unit,
    onAddRecipe: (Recipe, servings: Double) -> Unit,
    ciqual: List<CiqualFood> = emptyList(),
    onChooseCiqual: (CiqualFood) -> Unit = {},
    showScan: Boolean = false,
    onScan: () -> Unit = {},
    scanOutcome: ScanOutcome? = null,
    onCreateWithBarcode: (String) -> Unit = {},
) {
    val spacing = TallyTheme.spacing
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.add_search)) },
            singleLine = true,
            trailingIcon = if (showScan) {
                {
                    IconButton(onClick = onScan) {
                        Icon(
                            painter = painterResource(TallyIcons.scan),
                            contentDescription = stringResource(R.string.add_scan),
                        )
                    }
                }
            } else {
                null
            },
            shape = MaterialTheme.shapes.small,
            keyboardOptions = KeyboardOptions(imeAction = if (tab == TAB_ONLINE) ImeAction.Search else ImeAction.Default),
            keyboardActions = KeyboardActions(onSearch = { onSubmitOnline() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.gutter, vertical = spacing.s),
        )
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.gutter, vertical = spacing.s),
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
        ) {
            listOf(R.string.add_tab_foods, R.string.add_tab_recipes, R.string.add_tab_online, R.string.add_tab_ciqual)
                .forEachIndexed { index, label ->
                    FilterChip(
                        selected = tab == index,
                        onClick = { onTabChange(index) },
                        label = { Text(stringResource(label)) },
                    )
                }
        }
        scanOutcome?.let { outcome ->
            val message = when (outcome) {
                is ScanOutcome.NotFound -> stringResource(R.string.scan_not_found, outcome.code)
                is ScanOutcome.Offline -> stringResource(R.string.scan_offline, outcome.code)
                is ScanOutcome.RateLimited -> stringResource(R.string.scan_rate_limited, outcome.seconds)
            }
            ErrorBanner(
                message = message,
                retryLabel = stringResource(R.string.scan_create_manual),
                onRetry = { onCreateWithBarcode(outcome.code) },
                modifier = Modifier.padding(horizontal = spacing.gutter, vertical = spacing.s),
            )
        }
        if (tab == TAB_ONLINE && online == OnlineState.Loading) {
            val loading = stringResource(R.string.online_loading)
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.gutter)
                    .semantics { contentDescription = loading },
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            when (tab) {
                TAB_FOODS -> foodItems(foods, query, onSelectFood, onNewFood)
                TAB_RECIPES -> recipeItems(recipes, query, onSelectRecipe, onNewRecipe)
                TAB_CIQUAL -> ciqualItems(ciqual, query, onChooseCiqual)
                else -> onlineItems(online, query, onSubmitOnline, onChooseRemote)
            }
        }
    }

    selectedFood?.let { data ->
        FoodSheet(data, onDismissFood, onEdit = { onEditFood(data.food.id) }, onConfirm = { qty, portionId, count ->
            onAddFood(data.food, qty, portionId, count)
        })
    }
    recipeToLog?.let { recipe ->
        RecipeSheet(recipe, onDismissRecipe) { servings -> onAddRecipe(recipe, servings) }
    }
}

private fun LazyListScope.foodItems(
    foods: List<Food>,
    query: String,
    onSelect: (Food) -> Unit,
    onNew: () -> Unit,
) {
    if (foods.isEmpty()) {
        item(key = "empty-foods") {
            EmptyState(
                icon = painterResource(TallyIcons.restaurant),
                title = stringResource(if (query.isBlank()) R.string.add_empty_foods_title else R.string.add_no_match_title),
                body = stringResource(if (query.isBlank()) R.string.add_empty_foods_body else R.string.add_no_match_foods_body),
                actionLabel = stringResource(R.string.add_new_food),
                onAction = onNew,
            )
        }
        return
    }
    item(key = "new-food") { CreateAction(R.string.add_new_food, onNew) }
    items(foods, key = { "f-${it.id}" }) { food ->
        DataRow(
            title = food.name,
            detail = food.brand.orEmpty(),
            value = stringResource(
                if (food.baseUnit == BaseUnit.ML) R.string.per_100_ml else R.string.per_100_g,
                formatKcal(food.per100),
            ),
            onClick = { onSelect(food) },
        )
    }
}

private fun LazyListScope.recipeItems(
    recipes: List<Recipe>,
    query: String,
    onSelect: (Recipe) -> Unit,
    onNew: () -> Unit,
) {
    if (recipes.isEmpty()) {
        item(key = "empty-recipes") {
            EmptyState(
                icon = painterResource(TallyIcons.restaurant),
                title = stringResource(if (query.isBlank()) R.string.add_empty_recipes_title else R.string.add_no_match_title),
                body = stringResource(if (query.isBlank()) R.string.add_empty_recipes_body else R.string.add_no_match_recipes_body),
                actionLabel = stringResource(R.string.add_new_recipe),
                onAction = onNew,
            )
        }
        return
    }
    item(key = "new-recipe") { CreateAction(R.string.add_new_recipe, onNew) }
    items(recipes, key = { "r-${it.id}" }) { recipe ->
        DataRow(
            title = recipe.name,
            detail = "",
            value = stringResource(R.string.recipe_per_serving, formatKcal(recipe.perServing)),
            onClick = { onSelect(recipe) },
        )
    }
}

private fun LazyListScope.onlineItems(
    state: OnlineState,
    query: String,
    onSearch: () -> Unit,
    onChoose: (RemoteFood) -> Unit,
) {
    item(key = "online-search") {
        TextButton(
            onClick = onSearch,
            enabled = state != OnlineState.Loading && query.trim().length >= MIN_ONLINE_QUERY,
            modifier = Modifier.padding(horizontal = TallyTheme.spacing.xs),
        ) { Text(stringResource(R.string.add_search_online)) }
    }
    item(key = "online-status") {
        when (state) {
            OnlineState.Idle -> Hint(R.string.online_hint)
            OnlineState.Empty -> Hint(R.string.online_empty)
            OnlineState.Disabled -> Hint(R.string.online_disabled)
            OnlineState.Loading -> Unit
            OnlineState.Unavailable -> ErrorBanner(
                message = stringResource(R.string.online_unavailable),
                retryLabel = stringResource(R.string.action_retry),
                onRetry = onSearch,
                modifier = Modifier.padding(horizontal = TallyTheme.spacing.gutter),
            )
            is OnlineState.RateLimited -> Hint(R.string.online_rate_limited, state.seconds)
            is OnlineState.Results -> Unit
        }
    }
    if (state is OnlineState.Results) {
        items(state.items, key = { "o-${it.barcode}" }) { item ->
            DataRow(
                title = item.name,
                detail = item.brand.orEmpty(),
                value = stringResource(R.string.per_100_g, formatKcal(item.per100)),
                onClick = { onChoose(item) },
            )
        }
        item(key = "online-attribution") {
            Text(
                stringResource(R.string.online_attribution),
                style = MaterialTheme.typography.bodySmall,
                color = TallyTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = TallyTheme.spacing.gutter, vertical = TallyTheme.spacing.m),
            )
        }
    }
}

private fun LazyListScope.ciqualItems(
    results: List<CiqualFood>,
    query: String,
    onChoose: (CiqualFood) -> Unit,
) {
    if (results.isEmpty()) {
        item(key = "ciqual-status") {
            Hint(if (query.trim().length < MIN_CIQUAL_QUERY) R.string.ciqual_hint else R.string.ciqual_empty)
        }
    }
    items(results, key = { "c-${it.code}" }) { item ->
        DataRow(
            title = item.name,
            detail = "",
            value = stringResource(R.string.per_100_g, formatKcal(item.per100)),
            onClick = { onChoose(item) },
        )
    }
    item(key = "ciqual-attribution") {
        Text(
            stringResource(R.string.ciqual_attribution),
            style = MaterialTheme.typography.bodySmall,
            color = TallyTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = TallyTheme.spacing.gutter, vertical = TallyTheme.spacing.m),
        )
    }
}

@Composable
private fun CreateAction(label: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.padding(horizontal = TallyTheme.spacing.xs)) {
        Text(stringResource(label))
    }
}

@Composable
private fun Hint(message: Int, vararg args: Any) {
    Text(
        text = stringResource(message, *args),
        style = MaterialTheme.typography.bodyMedium,
        color = TallyTheme.colors.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = TallyTheme.spacing.gutter, vertical = TallyTheme.spacing.s),
    )
}

@Composable
private fun FoodSheet(
    data: FoodWithPortions,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onConfirm: (quantityBase: Double, portionId: Long?, portionCount: Double?) -> Unit,
) {
    val food = data.food
    val baseLabel = if (food.baseUnit == BaseUnit.ML) "ml" else "g"
    var portionId by remember { mutableStateOf(data.portions.firstOrNull { it.isDefault }?.id) }
    var text by remember { mutableStateOf(if (portionId != null) "1" else "100") }

    val portion = data.portions.firstOrNull { it.id == portionId }
    val amount = parseDecimal(text)
    val quantity = if (portion != null) amount?.let { it * portion.quantityBase } else amount
    val valid = quantity != null && quantity > 0

    QuantitySheet(
        title = food.name,
        value = text,
        onValueChange = { text = it },
        label = stringResource(if (portion != null) R.string.add_portion_count else R.string.add_quantity),
        unit = portion?.label ?: baseLabel,
        units = if (data.portions.isEmpty()) emptyList() else listOf(baseLabel) + data.portions.map { it.label },
        onUnitChange = { label ->
            val next = data.portions.firstOrNull { it.label == label }
            portionId = next?.id
            text = if (next != null) "1" else "100"
        },
        confirmLabel = stringResource(R.string.action_add),
        confirmEnabled = valid,
        onConfirm = { onConfirm(quantity!!, portion?.id, if (portion != null) amount else null) },
        onDismiss = onDismiss,
        preview = {
            Column {
                if (valid) {
                    Text(
                        text = stringResource(
                            R.string.add_preview,
                            formatNumber(quantity!!),
                            baseLabel,
                            formatKcal(NutritionCalc.macrosFor(food.per100, quantity)),
                        ),
                        style = TallyTheme.type.numberBody,
                        color = TallyTheme.colors.onSurface,
                    )
                }
                TextButton(onClick = onEdit) { Text(stringResource(R.string.food_edit)) }
            }
        },
    )
}

@Composable
private fun RecipeSheet(recipe: Recipe, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var text by remember { mutableStateOf("1") }
    val servings = parseDecimal(text)
    val valid = servings != null && servings > 0
    QuantitySheet(
        title = recipe.name,
        value = text,
        onValueChange = { text = it },
        label = stringResource(R.string.add_servings),
        unit = stringResource(R.string.add_servings_unit),
        confirmLabel = stringResource(R.string.action_add),
        confirmEnabled = valid,
        onConfirm = { onConfirm(servings!!) },
        onDismiss = onDismiss,
    )
}
