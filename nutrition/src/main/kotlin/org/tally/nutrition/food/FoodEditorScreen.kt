package org.tally.nutrition.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.tally.domain.model.BaseUnit
import org.tally.nutrition.R

@Composable
fun FoodEditorScreen(onDone: () -> Unit, viewModel: FoodEditorViewModel = hiltViewModel()) {
    val numeric = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(if (viewModel.isEditing) R.string.food_edit else R.string.food_new),
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
            value = viewModel.brand,
            onValueChange = { viewModel.brand = it },
            label = { Text(stringResource(R.string.food_brand)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            BaseUnit.entries.forEachIndexed { index, unit ->
                SegmentedButton(
                    selected = viewModel.baseUnit == unit,
                    onClick = { viewModel.baseUnit = unit },
                    shape = SegmentedButtonDefaults.itemShape(index, BaseUnit.entries.size),
                ) { Text(stringResource(if (unit == BaseUnit.G) R.string.food_per_100_g else R.string.food_per_100_ml)) }
            }
        }
        OutlinedTextField(
            value = viewModel.energy,
            onValueChange = { viewModel.energy = it },
            label = { Text(stringResource(R.string.food_energy)) },
            isError = viewModel.showError && viewModel.energy.isBlank(),
            singleLine = true,
            keyboardOptions = numeric,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = viewModel.protein,
            onValueChange = { viewModel.protein = it },
            label = { Text(stringResource(R.string.food_protein)) },
            singleLine = true,
            keyboardOptions = numeric,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = viewModel.carbs,
            onValueChange = { viewModel.carbs = it },
            label = { Text(stringResource(R.string.food_carbs)) },
            singleLine = true,
            keyboardOptions = numeric,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = viewModel.fat,
            onValueChange = { viewModel.fat = it },
            label = { Text(stringResource(R.string.food_fat)) },
            singleLine = true,
            keyboardOptions = numeric,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(stringResource(R.string.food_portions), style = MaterialTheme.typography.titleMedium)
        viewModel.portions.forEach { draft ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.label,
                    onValueChange = { draft.label = it },
                    label = { Text(stringResource(R.string.food_portion_label)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = draft.quantity,
                    onValueChange = { draft.quantity = it },
                    label = { Text(stringResource(R.string.food_portion_quantity)) },
                    singleLine = true,
                    keyboardOptions = numeric,
                    modifier = Modifier.weight(0.6f),
                )
            }
        }
        OutlinedButton(onClick = { viewModel.portions.add(PortionDraft()) }) {
            Text(stringResource(R.string.food_add_portion))
        }

        Button(onClick = { viewModel.save(onDone) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_save))
        }
        if (viewModel.isEditing) {
            TextButton(onClick = { viewModel.archive(onDone) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.food_archive))
            }
        }
    }
}
