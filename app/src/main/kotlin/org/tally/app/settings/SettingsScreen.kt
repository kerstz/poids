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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.tally.app.R
import org.tally.core.unit.DistanceUnit
import org.tally.core.unit.EnergyUnit
import org.tally.core.unit.LengthUnit
import org.tally.core.unit.MassUnit

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.unitPreferences.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.settings_units), style = MaterialTheme.typography.titleLarge)
        UnitChoice(R.string.settings_unit_mass, MassUnit.entries, prefs.mass, viewModel::setMass)
        UnitChoice(R.string.settings_unit_length, LengthUnit.entries, prefs.length, viewModel::setLength)
        UnitChoice(R.string.settings_unit_distance, DistanceUnit.entries, prefs.distance, viewModel::setDistance)
        UnitChoice(R.string.settings_unit_energy, EnergyUnit.entries, prefs.energy, viewModel::setEnergy)
    }
}

@Composable
private fun <E : Enum<E>> UnitChoice(
    labelRes: Int,
    options: List<E>,
    selected: E,
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
                Text(option.name.lowercase())
            }
        }
    }
}
