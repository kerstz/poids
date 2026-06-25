package com.poids.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poids.domain.calc.WaterGoalCalculator
import com.poids.domain.model.SleepLog
import com.poids.domain.model.SubstanceKind
import com.poids.domain.repository.HabitRepository
import com.poids.domain.repository.WaterRepository
import com.poids.domain.repository.WeightRepository
import com.poids.settings.SettingsPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class HabitsUiState(
    val latestSleep: SleepLog? = null,
    /** 7 derniers jours (du plus ancien au plus récent) : durée de sommeil en minutes (0 si aucune). */
    val last7Sleep: List<Int> = List(7) { 0 },
    val alcoholToday: Double = 0.0,
    val tobaccoToday: Double = 0.0,
    val caffeineToday: Double = 0.0,
    /** Poids chronologiques pour la mini-tendance. */
    val weightSeries: List<Double> = emptyList(),
    /** Suivi de substances activé dans les réglages. */
    val substanceTrackingEnabled: Boolean = false,
    /** Substances déjà saisies (nom, unité) pour l'autocomplete. */
    val recentSubstances: List<Pair<String, String>> = emptyList(),
    /** Hydratation du jour (ml) et objectif (ml). */
    val waterTodayMl: Int = 0,
    val waterGoalMl: Int = WaterGoalCalculator.DEFAULT_GOAL_ML,
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val waterRepository: WaterRepository,
    weightRepository: WeightRepository,
    private val settingsPreferences: SettingsPreferences,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private val weekStart: LocalDate = today.minusDays(6)

    val uiState: StateFlow<HabitsUiState> = combine(
        habitRepository.observeSleep(weekStart, today),
        habitRepository.observeSubstances(),
        weightRepository.observeAll(),
        settingsPreferences.settings,
        waterRepository.observeForDate(today),
    ) { sleep, substances, weights, settings, water ->
        val byDate = sleep.associateBy { it.date }
        val last7 = (0..6).map { offset ->
            byDate[weekStart.plusDays(offset.toLong())]?.durationMin ?: 0
        }
        val todaySubs = substances.filter { it.dateTime.toLocalDate() == today }
        val latestWeight = weights.firstOrNull()?.weightKg
        HabitsUiState(
            latestSleep = sleep.firstOrNull(),
            last7Sleep = last7,
            alcoholToday = todaySubs.filter { it.kind == SubstanceKind.ALCOHOL }.sumOf { it.amount },
            tobaccoToday = todaySubs.filter { it.kind == SubstanceKind.TOBACCO }.sumOf { it.amount },
            caffeineToday = todaySubs.filter { it.kind == SubstanceKind.CAFFEINE }.sumOf { it.amount },
            weightSeries = weights.reversed().map { it.weightKg },
            substanceTrackingEnabled = settings.substanceTrackingEnabled,
            recentSubstances = substances
                .map { displayName(it) to it.unit }
                .distinctBy { it.first.lowercase() }
                .take(9),
            waterTodayMl = water.sumOf { it.amountMl },
            waterGoalMl = settings.waterGoalMl ?: WaterGoalCalculator.dailyGoalMl(latestWeight),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsUiState())

    fun addWater(amountMl: Int) {
        viewModelScope.launch { waterRepository.addWater(amountMl) }
    }

    fun undoLastWater() {
        viewModelScope.launch { waterRepository.deleteLastForDate(today) }
    }

    fun setWaterGoal(ml: Int?) {
        viewModelScope.launch { settingsPreferences.setWaterGoal(ml) }
    }

    fun addSleep(bedTime: LocalTime, wakeTime: LocalTime) {
        viewModelScope.launch {
            // Si on se couche après-midi (>12h), le coucher est la veille du réveil.
            val wakeDate = today
            val bedDate = if (bedTime.isAfter(LocalTime.NOON)) today.minusDays(1) else today
            habitRepository.addSleep(
                date = wakeDate,
                bedTime = LocalDateTime.of(bedDate, bedTime),
                wakeTime = LocalDateTime.of(wakeDate, wakeTime),
                quality = null,
                note = null,
            )
        }
    }

    fun addSubstance(input: SubstanceEntryInput) {
        viewModelScope.launch {
            habitRepository.addSubstance(
                dateTime = input.dateTime,
                kind = input.kind,
                amount = input.amount,
                unit = input.unit,
                label = input.name,
                note = input.note,
            )
        }
    }
}
