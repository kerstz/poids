package org.tally.body.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.tally.core.time.AppClock
import org.tally.core.unit.MassUnit
import org.tally.domain.body.WeightEntry
import org.tally.domain.body.SaveWeight
import org.tally.domain.body.WeightRepository
import org.tally.domain.body.WeightValidation
import org.tally.domain.settings.SettingsRepository

data class WeightUiState(
    /** Plus récente d'abord. */
    val entries: List<WeightEntry> = emptyList(),
    val massUnit: MassUnit = MassUnit.KG,
)

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val repository: WeightRepository,
    private val saveWeight: SaveWeight,
    settings: SettingsRepository,
    private val clock: AppClock,
) : ViewModel() {

    val uiState: StateFlow<WeightUiState> = combine(
        repository.observeEntries(),
        settings.unitPreferences,
    ) { entries, prefs -> WeightUiState(entries, prefs.mass) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightUiState())

    /**
     * Enregistre une pesée. [valueInUnit] est exprimée dans l'unité d'affichage.
     * Retourne false si la valeur est hors bornes (rien n'est écrit).
     */
    fun save(id: Long?, valueInUnit: Double, measuredAt: Long?): Boolean {
        val kg = uiState.value.massUnit.toSi(valueInUnit)
        if (!WeightValidation.isValid(kg)) return false
        viewModelScope.launch {
            if (id == null) {
                saveWeight.add(kg, measuredAt ?: clock.nowMillis(), ZoneId.systemDefault().id)
            } else {
                uiState.value.entries.firstOrNull { it.id == id }?.let { existing ->
                    saveWeight.update(
                        existing.copy(valueKg = kg, measuredAt = measuredAt ?: existing.measuredAt),
                    )
                }
            }
        }
        return true
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}
