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
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class SubstanceHistoryUiState(
    /** Noms de substances présents, du plus fréquent au moins fréquent. */
    val names: List<String> = emptyList(),
    val monthLabels: List<String> = emptyList(),
    /** Par nom : dose cumulée sur 12 mois (ancien → récent). */
    val monthlyByName: Map<String, List<Float>> = emptyMap(),
    val totalMonthByName: Map<String, Double> = emptyMap(),
    val totalYearByName: Map<String, Double> = emptyMap(),
    val entries: List<SubstanceLog> = emptyList(),
    /** (nom, unité) déjà saisis, pour l'autocomplete. */
    val recent: List<Pair<String, String>> = emptyList(),
)

@HiltViewModel
class SubstanceHistoryViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
) : ViewModel() {

    private val currentMonth: YearMonth = YearMonth.now()

    val uiState: StateFlow<SubstanceHistoryUiState> = habitRepository.observeSubstances()
        .map { all -> aggregate(all) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubstanceHistoryUiState())

    private fun aggregate(all: List<SubstanceLog>): SubstanceHistoryUiState {
        val months = (0..11).map { currentMonth.minusMonths((11 - it).toLong()) }
        val named = all.map { it to displayName(it) }

        val names = named.groupingBy { it.second }.eachCount()
            .entries.sortedByDescending { it.value }.map { it.key }

        val monthlyByName = names.associateWith { name ->
            months.map { ym ->
                named.filter { it.second == name && YearMonth.from(it.first.dateTime) == ym }
                    .sumOf { it.first.amount }.toFloat()
            }
        }
        val thisYear = currentMonth.year
        return SubstanceHistoryUiState(
            names = names,
            monthLabels = months.map { it.month.getDisplayName(TextStyle.SHORT, Locale.FRENCH).take(4) },
            monthlyByName = monthlyByName,
            totalMonthByName = names.associateWith { name ->
                named.filter { it.second == name && YearMonth.from(it.first.dateTime) == currentMonth }
                    .sumOf { it.first.amount }
            },
            totalYearByName = names.associateWith { name ->
                named.filter { it.second == name && it.first.dateTime.year == thisYear }
                    .sumOf { it.first.amount }
            },
            entries = all,
            recent = named.map { it.second to it.first.unit }.distinctBy { it.first.lowercase() }.take(9),
        )
    }

    fun addSubstance(input: SubstanceEntryInput) {
        viewModelScope.launch {
            habitRepository.addSubstance(
                input.dateTime, input.kind, input.amount, input.unit, input.name, input.note,
            )
        }
    }

    fun updateSubstance(id: Long, input: SubstanceEntryInput) {
        viewModelScope.launch {
            habitRepository.updateSubstance(
                id, input.dateTime, input.kind, input.amount, input.unit, input.name, input.note,
            )
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { habitRepository.deleteSubstance(id) }
    }
}

/** Nom affiché d'une conso : nom libre si présent, sinon libellé de catégorie. */
fun displayName(log: SubstanceLog): String =
    log.label?.takeIf { it.isNotBlank() } ?: when (log.kind) {
        SubstanceKind.ALCOHOL -> "Alcool"
        SubstanceKind.TOBACCO -> "Tabac"
        SubstanceKind.CAFFEINE -> "Café"
        SubstanceKind.OTHER -> "Autre"
    }
