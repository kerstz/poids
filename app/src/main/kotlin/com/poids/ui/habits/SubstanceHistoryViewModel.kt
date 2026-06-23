package com.poids.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poids.domain.model.SubstanceKind
import com.poids.domain.model.SubstanceLog
import com.poids.domain.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class SubstanceHistoryUiState(
    /** Par type : 12 valeurs mensuelles (du plus ancien au plus récent). */
    val monthly: Map<SubstanceKind, List<Float>> = emptyMap(),
    val monthLabels: List<String> = emptyList(),
    /** Totaux par type pour le mois et l'année en cours. */
    val totalThisMonth: Map<SubstanceKind, Double> = emptyMap(),
    val totalThisYear: Map<SubstanceKind, Double> = emptyMap(),
    val entries: List<SubstanceLog> = emptyList(),
)

@HiltViewModel
class SubstanceHistoryViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
) : ViewModel() {

    private val kinds = listOf(SubstanceKind.ALCOHOL, SubstanceKind.TOBACCO, SubstanceKind.CAFFEINE)
    private val currentMonth: YearMonth = YearMonth.now()

    val uiState: StateFlow<SubstanceHistoryUiState> = habitRepository.observeSubstances()
        .map { all -> aggregate(all) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubstanceHistoryUiState())

    private fun aggregate(all: List<SubstanceLog>): SubstanceHistoryUiState {
        val months = (0..11).map { currentMonth.minusMonths((11 - it).toLong()) }
        val byMonthKind = all.groupBy { YearMonth.from(it.dateTime) to it.kind }

        val monthly = kinds.associateWith { kind ->
            months.map { ym ->
                byMonthKind[ym to kind].orEmpty().sumOf { it.amount }.toFloat()
            }
        }
        val thisYear = currentMonth.year
        return SubstanceHistoryUiState(
            monthly = monthly,
            monthLabels = months.map {
                it.month.getDisplayName(TextStyle.SHORT, Locale.FRENCH).take(4)
            },
            totalThisMonth = kinds.associateWith { kind ->
                all.filter { it.kind == kind && YearMonth.from(it.dateTime) == currentMonth }
                    .sumOf { it.amount }
            },
            totalThisYear = kinds.associateWith { kind ->
                all.filter { it.kind == kind && it.dateTime.year == thisYear }.sumOf { it.amount }
            },
            entries = all,
        )
    }

    fun addSubstance(kind: SubstanceKind, amount: Double, unit: String, dateTime: LocalDateTime = LocalDateTime.now()) {
        viewModelScope.launch {
            habitRepository.addSubstance(dateTime, kind, amount, unit, null)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { habitRepository.deleteSubstance(id) }
    }
}
