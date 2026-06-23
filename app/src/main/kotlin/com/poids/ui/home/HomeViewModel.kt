package com.poids.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poids.domain.calc.CalorieTargetCalculator
import com.poids.domain.model.CalorieTargets
import com.poids.domain.model.StepDay
import com.poids.domain.model.UserProfile
import com.poids.domain.model.WeightEntry
import com.poids.domain.repository.DailyEnergyRepository
import com.poids.domain.repository.MealRepository
import com.poids.domain.repository.ProfileRepository
import com.poids.domain.repository.StepRepository
import com.poids.domain.repository.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object NoProfile : HomeUiState
    data class Ready(
        val profile: UserProfile,
        val latestWeight: WeightEntry?,
        val targets: CalorieTargets,
        val intakeKcal: Double,
        val burnedKcal: Double,
        val carbsG: Double,
        val proteinG: Double,
        val fatG: Double,
        val stepDay: StepDay,
    ) : HomeUiState {
        val netKcal: Double get() = intakeKcal - burnedKcal
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    weightRepository: WeightRepository,
    mealRepository: MealRepository,
    dailyEnergyRepository: DailyEnergyRepository,
    stepRepository: StepRepository,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    val uiState: StateFlow<HomeUiState> = combine(
        profileRepository.observeProfile(),
        weightRepository.observeLatest(),
        mealRepository.observeMealsForDate(today),
        dailyEnergyRepository.observeBurnedKcal(today),
        stepRepository.observeToday(),
    ) { profile, latestWeight, meals, burnedKcal, stepDay ->
        if (profile == null) {
            HomeUiState.NoProfile
        } else {
            HomeUiState.Ready(
                profile = profile,
                latestWeight = latestWeight,
                targets = CalorieTargetCalculator.compute(profile, today),
                intakeKcal = meals.sumOf { it.kcal },
                burnedKcal = burnedKcal,
                carbsG = meals.sumOf { it.carbsG ?: 0.0 },
                proteinG = meals.sumOf { it.proteinG ?: 0.0 },
                fatG = meals.sumOf { it.fatG ?: 0.0 },
                stepDay = stepDay,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading,
    )
}
