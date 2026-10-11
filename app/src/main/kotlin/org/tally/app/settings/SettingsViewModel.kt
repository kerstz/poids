package org.tally.app.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.tally.core.unit.DistanceUnit
import org.tally.core.unit.EnergyUnit
import org.tally.core.unit.LengthUnit
import org.tally.core.unit.MassUnit
import org.tally.domain.settings.SettingsRepository
import org.tally.domain.settings.UnitPreferences

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val unitPreferences: StateFlow<UnitPreferences> = repository.unitPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitPreferences())

    val offEnabled: StateFlow<Boolean> = repository.offEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setOffEnabled(enabled: Boolean) = viewModelScope.launch { repository.setOffEnabled(enabled) }

    fun setMass(unit: MassUnit) = viewModelScope.launch { repository.setMassUnit(unit) }
    fun setLength(unit: LengthUnit) = viewModelScope.launch { repository.setLengthUnit(unit) }
    fun setDistance(unit: DistanceUnit) = viewModelScope.launch { repository.setDistanceUnit(unit) }
    fun setEnergy(unit: EnergyUnit) = viewModelScope.launch { repository.setEnergyUnit(unit) }
}
