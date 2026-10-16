// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.app.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.tally.app.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import org.tally.coreui.component.DataRow
import org.tally.domain.data.RestoreError
import org.tally.core.unit.DistanceUnit
import org.tally.core.unit.EnergyUnit
import org.tally.core.unit.LengthUnit
import org.tally.core.unit.MassUnit

@Composable
fun SettingsScreen(onOpenPrivacy: () -> Unit = {}, viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.unitPreferences.collectAsStateWithLifecycle()
    val offEnabled by viewModel.offEnabled.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.settings_units), style = MaterialTheme.typography.titleLarge)
        UnitChoice(R.string.settings_unit_mass, MassUnit.entries, prefs.mass, ::massLabel, viewModel::setMass)
        UnitChoice(R.string.settings_unit_length, LengthUnit.entries, prefs.length, ::lengthLabel, viewModel::setLength)
        UnitChoice(R.string.settings_unit_distance, DistanceUnit.entries, prefs.distance, ::distanceLabel, viewModel::setDistance)
        UnitChoice(R.string.settings_unit_energy, EnergyUnit.entries, prefs.energy, ::energyLabel, viewModel::setEnergy)

        Text(
            stringResource(R.string.settings_online),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 24.dp),
        )
        // Toute la ligne est l'élément interactif (TalkBack : un seul interrupteur, cible ≥ 48 dp).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(top = 8.dp)
                .toggleable(value = offEnabled, role = Role.Switch, onValueChange = viewModel::setOffEnabled),
        ) {
            Text(
                stringResource(R.string.settings_off_enabled),
                modifier = Modifier.weight(1f),
            )
            Switch(checked = offEnabled, onCheckedChange = null)
        }
        Text(
            stringResource(R.string.settings_off_hint),
            style = MaterialTheme.typography.bodySmall,
        )

        DataSection(viewModel)

        TextButton(onClick = onOpenPrivacy, modifier = Modifier.padding(top = 24.dp)) {
            Text(stringResource(R.string.settings_privacy_open))
        }

        Text(
            stringResource(R.string.settings_sources),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            stringResource(R.string.settings_off_attribution),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )

        AboutSection()
    }
}

@Composable
private fun DataSection(viewModel: SettingsViewModel) {
    val status by viewModel.data.collectAsStateWithLifecycle()
    val busy = status == DataStatus.Busy
    val today = remember { java.time.LocalDate.now().toString() }

    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) {
        it?.let(viewModel::exportBackup)
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let(viewModel::restore)
    }
    val saveWeights = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
        it?.let(viewModel::exportWeights)
    }
    val saveJournal = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
        it?.let(viewModel::exportJournal)
    }

    Text(
        stringResource(R.string.settings_data),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 24.dp),
    )
    if (busy) {
        val label = stringResource(R.string.data_busy)
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .semantics { contentDescription = label },
        )
    }
    DataRow(
        title = stringResource(R.string.data_backup),
        detail = stringResource(R.string.data_backup_hint),
        value = "",
        onClick = if (busy) null else ({ saveBackup.launch("tally-backup-$today.zip") }),
    )
    DataRow(
        title = stringResource(R.string.data_restore),
        detail = stringResource(R.string.data_restore_hint),
        value = "",
        onClick = if (busy) null else ({ restore.launch(arrayOf("application/zip", "application/octet-stream")) }),
    )
    DataRow(
        title = stringResource(R.string.data_export_weight),
        detail = "CSV",
        value = "",
        onClick = if (busy) null else ({ saveWeights.launch("tally-weight-v1-$today.csv") }),
    )
    DataRow(
        title = stringResource(R.string.data_export_journal),
        detail = "CSV",
        value = "",
        onClick = if (busy) null else ({ saveJournal.launch("tally-journal-v1-$today.csv") }),
    )
    var confirmWipe by remember { mutableStateOf(false) }
    DataRow(
        title = stringResource(R.string.data_wipe),
        detail = stringResource(R.string.data_wipe_hint),
        value = "",
        onClick = if (busy) null else ({ confirmWipe = true }),
    )
    if (confirmWipe) {
        WipeConfirmSheet(
            onConfirm = {
                confirmWipe = false
                viewModel.wipe()
            },
            onDismiss = { confirmWipe = false },
        )
    }
    val message = when (val s = status) {
        DataStatus.Idle, DataStatus.Busy -> null
        is DataStatus.BackupSaved -> pluralStringResource(R.plurals.data_backup_saved, s.rows, s.rows)
        is DataStatus.Exported -> pluralStringResource(R.plurals.data_exported, s.rows, s.rows)
        is DataStatus.Restored -> pluralStringResource(R.plurals.data_restored, s.rows, s.rows)
        DataStatus.Undone -> stringResource(R.string.data_undone)
        DataStatus.Wiped -> stringResource(R.string.data_wiped)
        DataStatus.WipeFailed -> stringResource(R.string.data_err_wipe)
        is DataStatus.Failed -> stringResource(
            when (s.error) {
                RestoreError.FUTURE_FORMAT -> R.string.data_err_future
                RestoreError.CHECKSUM, RestoreError.COUNT_MISMATCH -> R.string.data_err_corrupt
                RestoreError.NOT_A_BACKUP -> R.string.data_err_not_backup
                RestoreError.INVALID_ROW, RestoreError.MISSING_BUILTIN -> R.string.data_err_invalid
                RestoreError.TOO_LARGE -> R.string.data_err_large
                RestoreError.NOTHING_TO_UNDO -> R.string.data_err_nothing
                RestoreError.IO, null -> R.string.data_err_io
            },
        )
    }
    if (message != null) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(top = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (status is DataStatus.Restored) {
            TextButton(onClick = viewModel::undoRestore) { Text(stringResource(R.string.data_undo)) }
        }
    }
}

@Composable
private fun <E : Enum<E>> UnitChoice(
    labelRes: Int,
    options: List<E>,
    selected: E,
    labelOf: (E) -> Int,
    onSelect: (E) -> Unit,
) {
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                Text(stringResource(labelOf(option)))
            }
        }
    }
}

private fun massLabel(u: MassUnit) = when (u) {
    MassUnit.KG -> R.string.unit_kg
    MassUnit.LB -> R.string.unit_lb
    MassUnit.ST -> R.string.unit_st
}

private fun lengthLabel(u: LengthUnit) = when (u) {
    LengthUnit.CM -> R.string.unit_cm
    LengthUnit.IN -> R.string.unit_in
}

private fun distanceLabel(u: DistanceUnit) = when (u) {
    DistanceUnit.KM -> R.string.unit_km
    DistanceUnit.MI -> R.string.unit_mi
}

private fun energyLabel(u: EnergyUnit) = when (u) {
    EnergyUnit.KCAL -> R.string.unit_kcal
    EnergyUnit.KJ -> R.string.unit_kj
}

/**
 * Confirmation d'effacement en bottom sheet (pas d'AlertDialog, cf. docs/UI.md).
 * Exception à la règle « jamais de confirmation » : l'effacement est irréversible (pas d'Annuler possible).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WipeConfirmSheet(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.data_wipe_confirm_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.data_wipe_confirm_body),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 16.dp),
            ) { Text(stringResource(R.string.data_wipe_confirm)) }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.data_wipe_cancel))
            }
        }
    }
}
