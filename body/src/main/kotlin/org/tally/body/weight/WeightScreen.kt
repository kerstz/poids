package org.tally.body.weight

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.tally.body.R
import org.tally.core.unit.MassUnit
import org.tally.coreui.chart.ChartPoint
import org.tally.coreui.chart.TimeSeriesLineChart
import org.tally.domain.body.WeightEntry

@Composable
fun WeightScreen(viewModel: WeightViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<EditorTarget?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp),
        ) {
            item {
                WeightChart(state)
            }
            if (state.entries.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.weight_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(state.entries, key = { it.id }) { entry ->
                WeightRow(
                    entry = entry,
                    unit = state.massUnit,
                    onClick = { editor = EditorTarget.Edit(entry) },
                    onDelete = { viewModel.delete(entry.id) },
                )
            }
        }
        FloatingActionButton(
            onClick = { editor = EditorTarget.New },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.weight_add))
        }
    }

    editor?.let { target ->
        WeightEditorDialog(
            target = target,
            unit = state.massUnit,
            onDismiss = { editor = null },
            onSave = { value, measuredAt ->
                val saved = viewModel.save((target as? EditorTarget.Edit)?.entry?.id, value, measuredAt)
                if (saved) editor = null
                saved
            },
        )
    }
}

@Composable
private fun WeightChart(state: WeightUiState) {
    val points = remember(state.entries, state.massUnit) {
        state.entries.asReversed().map { entry ->
            ChartPoint(
                epochDay = Instant.ofEpochMilli(entry.measuredAt)
                    .atZone(safeZone(entry.zoneId)).toLocalDate().toEpochDay(),
                value = state.massUnit.fromSi(entry.valueKg),
            )
        }
    }
    if (points.size >= 2) {
        TimeSeriesLineChart(
            points = points,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun WeightRow(
    entry: WeightEntry,
    unit: MassUnit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val date = remember(entry) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .format(Instant.ofEpochMilli(entry.measuredAt).atZone(safeZone(entry.zoneId)))
    }
    ListItem(
        headlineContent = { Text(formatMass(unit.fromSi(entry.valueKg), unit)) },
        supportingContent = { Text(date) },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.weight_delete))
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private sealed interface EditorTarget {
    data object New : EditorTarget
    data class Edit(val entry: WeightEntry) : EditorTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightEditorDialog(
    target: EditorTarget,
    unit: MassUnit,
    onDismiss: () -> Unit,
    onSave: (value: Double, measuredAt: Long?) -> Boolean,
) {
    val existing = (target as? EditorTarget.Edit)?.entry
    var text by remember {
        mutableStateOf(existing?.let { formatNumber(unit.fromSi(it.valueKg)) } ?: "")
    }
    var invalid by remember { mutableStateOf(false) }
    var date by remember {
        mutableStateOf(existing?.let {
            Instant.ofEpochMilli(it.measuredAt).atZone(safeZone(it.zoneId)).toLocalDate()
        } ?: LocalDate.now())
    }
    var pickingDate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.weight_add else R.string.weight_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; invalid = false },
                    label = { Text(unit.name.lowercase()) },
                    isError = invalid,
                    supportingText = if (invalid) {
                        { Text(stringResource(R.string.weight_invalid)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { pickingDate = true }) {
                        Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = text.trim().replace(',', '.').toDoubleOrNull()
                val measuredAt = measuredAtFor(date, existing)
                if (value == null || !onSave(value, measuredAt)) invalid = true
            }) { Text(stringResource(R.string.weight_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.weight_cancel)) }
        },
    )

    if (pickingDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    pickingDate = false
                }) { Text(stringResource(R.string.weight_save)) }
            },
        ) { DatePicker(state = pickerState) }
    }
}

/** Date choisie + heure d'origine (édition) ou heure courante (nouvelle pesée), en zone locale. */
private fun measuredAtFor(date: LocalDate, existing: WeightEntry?): Long {
    val zone = existing?.let { safeZone(it.zoneId) } ?: ZoneId.systemDefault()
    val time = existing?.let { Instant.ofEpochMilli(it.measuredAt).atZone(zone).toLocalTime() }
        ?: LocalDateTime.now().toLocalTime()
    return LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()
}

private fun safeZone(id: String): ZoneId = runCatching { ZoneId.of(id) }.getOrDefault(ZoneId.systemDefault())

private fun formatNumber(value: Double): String = String.format(Locale.getDefault(), "%.1f", value)

private fun formatMass(value: Double, unit: MassUnit): String = "${formatNumber(value)} ${unit.name.lowercase()}"
