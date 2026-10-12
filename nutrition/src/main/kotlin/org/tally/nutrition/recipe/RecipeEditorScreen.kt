// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.nutrition.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import org.tally.coreui.icon.TallyIcons
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.tally.domain.nutrition.Food
import org.tally.nutrition.R
import org.tally.nutrition.common.MacroSummary
import org.tally.nutrition.common.formatKcal
import org.tally.nutrition.common.formatNumber
import org.tally.nutrition.common.parseDecimal

@Composable
fun RecipeEditorScreen(onDone: () -> Unit, viewModel: RecipeEditorViewModel = hiltViewModel()) {
    var picking by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(if (viewModel.isEditing) R.string.recipe_edit else R.string.recipe_new),
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedTextField(
            value = viewModel.name,
            onValueChange = { viewModel.name = it },
            label = { Text(stringResource(R.string.food_name)) },
            isError = viewModel.showError && viewModel.name.isBlank(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = viewModel.servings,
            onValueChange = { viewModel.servings = it },
            label = { Text(stringResource(R.string.recipe_servings)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(stringResource(R.string.recipe_ingredients), style = MaterialTheme.typography.titleMedium)
        viewModel.ingredients.forEachIndexed { index, ingredient ->
            ListItem(
                headlineContent = { Text(ingredient.foodName) },
                supportingContent = {
                    Text(
                        "${formatNumber(ingredient.quantityBase)} · " +
                            stringResource(R.string.kcal_value, formatKcal(ingredient.macros)),
                    )
                },
                trailingContent = {
                    IconButton(onClick = { viewModel.removeIngredient(index) }) {
                        Icon(painterResource(TallyIcons.delete), contentDescription = stringResource(R.string.action_delete))
                    }
                },
            )
        }
        OutlinedButton(onClick = { picking = true }) { Text(stringResource(R.string.recipe_add_ingredient)) }

        Text(
            stringResource(R.string.recipe_per_serving, formatKcal(viewModel.perServing)),
            style = MaterialTheme.typography.titleMedium,
        )
        MacroSummary(viewModel.perServing)

        Button(onClick = { viewModel.save(onDone) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_save))
        }
        if (viewModel.isEditing) {
            TextButton(onClick = { viewModel.archive(onDone) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.recipe_archive))
            }
        }
    }

    if (picking) {
        IngredientPickerDialog(
            viewModel = viewModel,
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun IngredientPickerDialog(viewModel: RecipeEditorViewModel, onDismiss: () -> Unit) {
    val query by viewModel.foodQueryText.collectAsStateWithLifecycle()
    val results by viewModel.foodResults.collectAsStateWithLifecycle()
    var chosen by remember { mutableStateOf<Food?>(null) }
    var quantity by remember { mutableStateOf("100") }
    val qty = parseDecimal(quantity)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(chosen?.name ?: stringResource(R.string.recipe_add_ingredient)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (chosen == null) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::setFoodQuery,
                        label = { Text(stringResource(R.string.add_search)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LazyColumn(Modifier.heightIn(max = 280.dp)) {
                        items(results, key = { it.id }) { food ->
                            ListItem(
                                headlineContent = { Text(food.name) },
                                modifier = Modifier.clickable { chosen = food },
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text(stringResource(R.string.recipe_quantity)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
            }
        },
        confirmButton = {
            if (chosen != null) {
                TextButton(
                    enabled = qty != null && qty > 0,
                    onClick = {
                        viewModel.addIngredient(chosen!!, qty!!)
                        onDismiss()
                    },
                ) { Text(stringResource(R.string.action_add)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
