package com.poids.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poids.domain.calc.CalorieTargetCalculator
import com.poids.domain.model.CalorieTargets
import com.poids.domain.model.UserProfile
import com.poids.domain.model.WeightEntry
import com.poids.domain.repository.ProfileRepository
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
    ) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    weightRepository: WeightRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        profileRepository.observeProfile(),
        weightRepository.observeLatest(),
    ) { profile, latestWeight ->
        if (profile == null) {
            HomeUiState.NoProfile
        } else {
            HomeUiState.Ready(
                profile = profile,
                latestWeight = latestWeight,
                targets = CalorieTargetCalculator.compute(profile, LocalDate.now()),
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading,
    )
}
