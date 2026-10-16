// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.nutrition.journal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.tally.coreui.component.EmptyState
import org.tally.coreui.component.FoodRow
import org.tally.coreui.component.MacroBar
import org.tally.coreui.component.MacroLegend
import org.tally.coreui.component.ProgressGauge
import org.tally.coreui.component.QuantitySheet
import org.tally.coreui.component.SectionHeader
import org.tally.coreui.component.TallyCard
import org.tally.coreui.icon.TallyIcons
import org.tally.coreui.theme.TallyTheme
import org.tally.domain.model.QuantityUnit
import org.tally.domain.nutrition.JournalItem
import org.tally.domain.nutrition.Macros
import org.tally.domain.nutrition.MealSlot
import org.tally.nutrition.R
import org.tally.nutrition.common.formatKcal
import org.tally.nutrition.common.formatNumber
import org.tally.nutrition.common.parseDecimal

/** Écran Journal sans état : testable en capture avec un [JournalUiState] fixe. */
@Composable
internal fun JournalContent(
    state: JournalUiState,
    snackbarHostState: SnackbarHostState,
    editingTarget: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onAddEntry: (slotId: Long?) -> Unit,
    onDelete: (JournalItem) -> Unit,
    onEditTarget: () -> Unit,
    onDismissTarget: () -> Unit,
    onSaveTarget: (Double) -> Unit,
) {
    val isEmpty = state.sections.all { it.items.isEmpty() }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(bottom = TallyTheme.dimens.fabClearance)) {
            item { DayHeader(state.epochDay, onPreviousDay, onNextDay, onToday) }
            item { TotalsCard(state, onEditTarget) }
            if (isEmpty) {
                item {
                    EmptyState(
                        icon = painterResource(TallyIcons.restaurant),
                        title = stringResource(R.string.journal_empty_title),
                        body = stringResource(R.string.journal_empty_body),
                        actionLabel = stringResource(R.string.action_add),
                        onAction = { onAddEntry(null) },
                    )
                }
            } else {
                state.sections.forEach { section ->
                    item(key = "slot-${section.slot.id}") {
                        val label = slotLabel(section.slot)
                        SectionHeader(
                            title = label,
                            trailing = stringResource(R.string.kcal_value, formatKcal(section.total)),
                            action = {
                                IconButton(onClick = { onAddEntry(section.slot.id) }) {
                                    Icon(
                                        painterResource(TallyIcons.add),
                                        contentDescription = stringResource(R.string.journal_add_to_slot, label),
                                    )
                                }
                            },
                        )
                    }
                    items(section.items, key = { "entry-${it.id}" }) { item ->
                        EntryRow(item, onDelete = { onDelete(item) })
                    }
                }
            }
        }
        if (!isEmpty) {
            ExtendedFloatingActionButton(
                onClick = { onAddEntry(null) },
                containerColor = TallyTheme.colors.accent,
                contentColor = TallyTheme.colors.onAccent,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(TallyTheme.spacing.gutter),
            ) {
                Icon(painterResource(TallyIcons.add), contentDescription = null)
                Text(stringResource(R.string.action_add), modifier = Modifier.padding(start = TallyTheme.spacing.s))
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }

    if (editingTarget) {
        TargetSheet(state.targets.energyKcal, onDismissTarget, onSaveTarget)
    }
}

@Composable
private fun DayHeader(epochDay: Long, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    val date = LocalDate.ofEpochDay(epochDay)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TallyTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(painterResource(TallyIcons.chevronLeft), contentDescription = stringResource(R.string.journal_previous_day))
        }
        TextButton(onClick = onToday, modifier = Modifier.weight(1f)) {
            Text(
                text = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                style = MaterialTheme.typography.titleMedium,
                color = TallyTheme.colors.onSurface,
            )
        }
        IconButton(onClick = onNext) {
            Icon(painterResource(TallyIcons.chevronRight), contentDescription = stringResource(R.string.journal_next_day))
        }
    }
}

@Composable
private fun TotalsCard(state: JournalUiState, onEditTarget: () -> Unit) {
    val target = state.targets.energyKcal
    TallyCard(modifier = Modifier.padding(horizontal = TallyTheme.spacing.gutter, vertical = TallyTheme.spacing.s)) {
        Text(
            text = formatKcal(state.total),
            style = TallyTheme.type.numberHero,
            color = TallyTheme.colors.onSurface,
        )
        Text(
            text = if (target != null) {
                stringResource(R.string.journal_kcal_of_target_label, formatNumber(target))
            } else {
                stringResource(R.string.journal_kcal_unit)
            },
            style = MaterialTheme.typography.labelMedium,
            color = TallyTheme.colors.onSurfaceVariant,
        )
        if (target != null && target > 0) {
            ProgressGauge(
                value = state.total.energyKcal.toFloat(),
                target = target.toFloat(),
                label = stringResource(R.string.journal_kcal_of_target, formatKcal(state.total), formatNumber(target)),
            )
        }
        MacroBar(
            proteinG = state.total.proteinG,
            carbsG = state.total.carbsG,
            fatG = state.total.fatG,
            legend = state.total.legend(),
        )
        TextButton(onClick = onEditTarget) {
            Text(stringResource(R.string.journal_set_target))
        }
    }
}

@Composable
private fun Macros.legend() = MacroLegend(
    protein = stringResource(R.string.macro_legend_protein, formatNumber(proteinG, 1)),
    carbs = stringResource(R.string.macro_legend_carbs, formatNumber(carbsG, 1)),
    fat = stringResource(R.string.macro_legend_fat, formatNumber(fatG, 1)),
)

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
    val quantity = when (item.quantityUnit) {
        QuantityUnit.SERVING -> stringResource(R.string.entry_servings, formatNumber(item.quantityBase, 1))
        QuantityUnit.ML -> stringResource(R.string.entry_ml, formatNumber(item.quantityBase))
        QuantityUnit.G -> stringResource(R.string.entry_g, formatNumber(item.quantityBase))
    }
    FoodRow(
        name = item.name,
        detail = quantity,
        kcal = stringResource(R.string.kcal_value, formatKcal(item.macros)),
        secondary = stringResource(
            R.string.macro_summary,
            formatNumber(item.macros.proteinG, 1),
            formatNumber(item.macros.carbsG, 1),
            formatNumber(item.macros.fatG, 1),
        ),
        onDelete = onDelete,
        deleteLabel = stringResource(R.string.action_delete),
    )
}

@Composable
private fun TargetSheet(initial: Double?, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var text by remember { mutableStateOf(initial?.let { formatNumber(it) } ?: "") }
    val value = parseDecimal(text)
    val valid = value != null && value > 0
    QuantitySheet(
        title = stringResource(R.string.journal_set_target),
        value = text,
        onValueChange = { text = it },
        label = stringResource(R.string.journal_target_label),
        unit = stringResource(R.string.journal_kcal_unit),
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = { if (value != null && value > 0) onSave(value) },
        onDismiss = onDismiss,
        confirmEnabled = valid,
    )
}
