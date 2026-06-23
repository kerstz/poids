package com.poids.ui.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poids.domain.model.WeightEntry
import com.poids.domain.repository.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** entries triées de la plus récente à la plus ancienne (ordre du DAO). */
data class WeightUiState(
    val entries: List<WeightEntry> = emptyList(),
    val latest: WeightEntry? = null,
    val totalChangeKg: Double? = null,
)

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val repository: WeightRepository,
) : ViewModel() {

    val uiState: StateFlow<WeightUiState> = repository.observeAll()
        .map { entries ->
            WeightUiState(
                entries = entries,
                latest = entries.firstOrNull(),
                totalChangeKg = if (entries.size >= 2) {
                    entries.first().weightKg - entries.last().weightKg
                } else {
                    null
                },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WeightUiState(),
        )

    fun addWeight(weightKg: Double, date: LocalDate, note: String?) {
        viewModelScope.launch {
            repository.add(WeightEntry(date = date, weightKg = weightKg, note = note?.takeIf { it.isNotBlank() }))
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}
