package com.poids.ui.profile

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

sealed interface ProfileSummaryState {
    data object Loading : ProfileSummaryState
    data object Empty : ProfileSummaryState
    data class Ready(
        val profile: UserProfile,
        val targets: CalorieTargets,
        val latestWeight: WeightEntry?,
        val ageYears: Int,
    ) : ProfileSummaryState
}

@HiltViewModel
class ProfileSummaryViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    weightRepository: WeightRepository,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    val state: StateFlow<ProfileSummaryState> = combine(
        profileRepository.observeProfile(),
        weightRepository.observeLatest(),
    ) { profile, latestWeight ->
        if (profile == null) {
            ProfileSummaryState.Empty
        } else {
            ProfileSummaryState.Ready(
                profile = profile,
                targets = CalorieTargetCalculator.compute(profile, today),
                latestWeight = latestWeight,
                ageYears = profile.ageOn(today),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileSummaryState.Loading)
}
