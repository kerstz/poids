// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.body.weight

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import org.tally.coreui.component.DataRow
import org.tally.coreui.component.EmptyState
import org.tally.coreui.component.QuantitySheet
import org.tally.coreui.icon.TallyIcons
import org.tally.coreui.theme.TallyTheme
import org.tally.domain.body.WeightEntry

internal sealed interface EditorTarget {
    data object New : EditorTarget
    data class Edit(val entry: WeightEntry) : EditorTarget
}

/** Écran Corps sans état : testable en capture avec un [WeightUiState] fixe. */
@Composable
internal fun WeightContent(
    state: WeightUiState,
    snackbarHostState: SnackbarHostState,
    editor: EditorTarget?,
    onAdd: () -> Unit,
    onEdit: (WeightEntry) -> Unit,
    onDelete: (WeightEntry) -> Unit,
    onDismissEditor: () -> Unit,
    onSave: (value: Double, measuredAt: Long?) -> Boolean,
) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = TallyTheme.dimens.fabClearance),
        ) {
            val latest = state.entries.firstOrNull()
            if (latest == null) {
                item {
                    EmptyState(
                        icon = painterResource(TallyIcons.showChart),
                        title = stringResource(R.string.weight_empty_title),
                        body = stringResource(R.string.weight_empty_body),
                        actionLabel = stringResource(R.string.weight_add),
                        onAction = onAdd,
                    )
                }
            } else {
                item { Hero(latest, state.massUnit) }
                item { WeightChart(state) }
                items(state.entries, key = { it.id }) { entry ->
                    DataRow(
                        title = formatMass(state.massUnit.fromSi(entry.valueKg), state.massUnit),
                        detail = formatDate(entry, FormatStyle.MEDIUM, FormatStyle.SHORT),
                        value = "",
                        onClick = { onEdit(entry) },
                        onDelete = { onDelete(entry) },
                        deleteLabel = stringResource(R.string.weight_delete),
                    )
                }
            }
        }
        if (state.entries.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                containerColor = TallyTheme.colors.accent,
                contentColor = TallyTheme.colors.onAccent,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(TallyTheme.spacing.gutter),
            ) {
                Icon(painterResource(TallyIcons.add), contentDescription = null)
                Text(stringResource(R.string.weight_add), modifier = Modifier.padding(start = TallyTheme.spacing.s))
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }

    editor?.let { target ->
        WeightEditorSheet(target, state.massUnit, onDismissEditor, onSave)
    }
}

@Composable
private fun Hero(entry: WeightEntry, unit: MassUnit) {
    Box(Modifier.padding(horizontal = TallyTheme.spacing.gutter, vertical = TallyTheme.spacing.l)) {
        Column {
            Text(
                text = formatMass(unit.fromSi(entry.valueKg), unit),
                style = TallyTheme.type.numberHero,
                color = TallyTheme.colors.onSurface,
            )
            Text(
                text = formatDate(entry, FormatStyle.MEDIUM, null),
                style = MaterialTheme.typography.labelMedium,
                color = TallyTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeightChart(state: WeightUiState) {
    val points = remember(state.entries, state.massUnit) {
        state.entries.asReversed().map { entry ->
            ChartPoint(
                epochDay = entry.measuredAt.toLocalDate(entry.zoneId).toEpochDay(),
                value = state.massUnit.fromSi(entry.valueKg),
            )
        }
    }
    if (points.size >= 2) {
        val first = points.first().value
        val last = points.last().value
        TimeSeriesLineChart(
            points = points,
            summary = stringResource(
                R.string.weight_chart_summary,
                formatNumber(first),
                formatNumber(last),
                state.massUnit.name.lowercase(),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(TallyTheme.dimens.chartHeight)
                .padding(horizontal = TallyTheme.spacing.gutter),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightEditorSheet(
    target: EditorTarget,
    unit: MassUnit,
    onDismiss: () -> Unit,
    onSave: (value: Double, measuredAt: Long?) -> Boolean,
) {
    val existing = (target as? EditorTarget.Edit)?.entry
    var text by remember { mutableStateOf(existing?.let { formatNumber(unit.fromSi(it.valueKg)) } ?: "") }
    var invalid by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(existing?.measuredAt?.toLocalDate(existing.zoneId) ?: LocalDate.now()) }
    var pickingDate by remember { mutableStateOf(false) }

    QuantitySheet(
        title = stringResource(if (existing == null) R.string.weight_add else R.string.weight_edit),
        value = text,
        onValueChange = { text = it; invalid = false },
        label = stringResource(R.string.weight_label),
        unit = unit.name.lowercase(),
        confirmLabel = stringResource(R.string.weight_save),
        onConfirm = {
            val value = text.trim().replace(',', '.').toDoubleOrNull()
            if (value == null || !onSave(value, measuredAtFor(date, existing))) invalid = true
        },
        onDismiss = onDismiss,
        error = if (invalid) stringResource(R.string.weight_invalid) else null,
        confirmEnabled = text.isNotBlank(),
        preview = {
            TextButton(onClick = { pickingDate = true }) {
                Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
            }
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

private fun formatDate(entry: WeightEntry, date: FormatStyle, time: FormatStyle?): String {
    val formatter = if (time != null) {
        DateTimeFormatter.ofLocalizedDateTime(date, time)
    } else {
        DateTimeFormatter.ofLocalizedDate(date)
    }
    return formatter.withLocale(Locale.getDefault()).format(Instant.ofEpochMilli(entry.measuredAt).atZone(safeZone(entry.zoneId)))
}

internal fun formatNumber(value: Double): String = String.format(Locale.getDefault(), "%.1f", value)

internal fun formatMass(value: Double, unit: MassUnit): String = "${formatNumber(value)} ${unit.name.lowercase()}"
