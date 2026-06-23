package com.poids.ui.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poids.domain.repository.HabitRepository
import com.poids.domain.repository.StepRepository
import com.poids.domain.repository.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class TrendsUiState(
    val weightSeries: List<Double> = emptyList(),
    val weightChange: Double? = null,
    /** 7 derniers jours (ancien → récent). */
    val steps7: List<Int> = List(7) { 0 },
    val sleep7: List<Int> = List(7) { 0 },
)

@HiltViewModel
class TrendsViewModel @Inject constructor(
    weightRepository: WeightRepository,
    stepRepository: StepRepository,
    habitRepository: HabitRepository,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private val weekStart: LocalDate = today.minusDays(6)

    val uiState: StateFlow<TrendsUiState> = combine(
        weightRepository.observeAll(),
        stepRepository.observeRange(weekStart, today),
        habitRepository.observeSleep(weekStart, today),
    ) { weights, steps, sleep ->
        val stepsByDate = steps.associateBy { it.date }
        val sleepByDate = sleep.associateBy { it.date }
        val series = weights.reversed().map { it.weightKg }
        TrendsUiState(
            weightSeries = series,
            weightChange = if (series.size >= 2) series.last() - series.first() else null,
            steps7 = (0..6).map { stepsByDate[weekStart.plusDays(it.toLong())]?.steps ?: 0 },
            sleep7 = (0..6).map { sleepByDate[weekStart.plusDays(it.toLong())]?.durationMin ?: 0 },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrendsUiState())
}
