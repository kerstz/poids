package org.tally.nutrition.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.tally.domain.model.BaseUnit
import org.tally.domain.nutrition.JournalItem
import org.tally.domain.nutrition.MealSlot
import org.tally.nutrition.R
import org.tally.nutrition.common.MacroSummary
import org.tally.nutrition.common.formatKcal
import org.tally.nutrition.common.formatNumber
import org.tally.nutrition.common.parseDecimal

@Composable
fun JournalScreen(
    onAddEntry: (mealSlotId: Long, epochDay: Long) -> Unit,
    viewModel: JournalViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editingTarget by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item { DayHeader(state, viewModel) }
        item { TotalsCard(state, onEditTarget = { editingTarget = true }) }
        state.sections.forEach { section ->
            item(key = "slot-${section.slot.id}") {
                SlotHeader(
                    slot = section.slot,
                    kcal = formatKcal(section.total),
                    onAdd = { onAddEntry(section.slot.id, state.epochDay) },
                )
            }
            items(section.items, key = { "entry-${it.id}" }) { item ->
                EntryRow(item, onDelete = { viewModel.delete(item.id) })
            }
        }
    }

    if (editingTarget) {
        TargetDialog(
            initial = state.targets.energyKcal,
            onDismiss = { editingTarget = false },
            onSave = {
                viewModel.setEnergyTarget(it)
                editingTarget = false
            },
        )
    }
}

@Composable
private fun DayHeader(state: JournalUiState, viewModel: JournalViewModel) {
    val date = LocalDate.ofEpochDay(state.epochDay)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = viewModel::previousDay) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.journal_previous_day))
        }
        Text(
            text = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = viewModel::today),
        )
        IconButton(onClick = viewModel::nextDay) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.journal_next_day))
        }
    }
}

@Composable
private fun TotalsCard(state: JournalUiState, onEditTarget: () -> Unit) {
    val target = state.targets.energyKcal
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (target != null) {
                    stringResource(R.string.journal_kcal_of_target, formatKcal(state.total), formatNumber(target))
                } else {
                    stringResource(R.string.journal_kcal_only, formatKcal(state.total))
                },
                style = MaterialTheme.typography.headlineSmall,
            )
            if (target != null && target > 0) {
                LinearProgressIndicator(
                    progress = { (state.total.energyKcal / target).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            MacroSummary(state.total)
            TextButton(onClick = onEditTarget) {
                Text(stringResource(R.string.journal_set_target))
            }
        }
    }
}

@Composable
private fun SlotHeader(slot: MealSlot, kcal: String, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = slotLabel(slot),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(text = stringResource(R.string.kcal_value, kcal), style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.journal_add_to_slot, slotLabel(slot)))
        }
    }
}

@Composable
private fun slotLabel(slot: MealSlot): String = slot.name ?: when (slot.builtinKey) {
    "breakfast" -> stringResource(R.string.slot_breakfast)
    "lunch" -> stringResource(R.string.slot_lunch)
    "dinner" -> stringResource(R.string.slot_dinner)
    "snack" -> stringResource(R.string.slot_snack)
    else -> slot.builtinKey.orEmpty()
}

@Composable
private fun EntryRow(item: JournalItem, onDelete: () -> Unit) {
    val quantity = when {
        item.isRecipe -> stringResource(R.string.entry_servings, formatNumber(item.quantityBase, 1))
        item.baseUnit == BaseUnit.ML -> stringResource(R.string.entry_ml, formatNumber(item.quantityBase))
        else -> stringResource(R.string.entry_g, formatNumber(item.quantityBase))
    }
    ListItem(
        headlineContent = { Text(item.name) },
        supportingContent = {
            Column {
                Text("$quantity · ${stringResource(R.string.kcal_value, formatKcal(item.macros))}")
                MacroSummary(item.macros)
            }
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
            }
        },
    )
}

@Composable
private fun TargetDialog(initial: Double?, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var text by remember { mutableStateOf(initial?.let { formatNumber(it) } ?: "") }
    val value = parseDecimal(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.journal_set_target)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.journal_target_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onSave) }, enabled = value != null && value > 0) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
