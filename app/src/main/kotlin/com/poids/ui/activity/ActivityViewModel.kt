package com.poids.ui.activity

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poids.domain.calc.MetCalculator
import com.poids.domain.model.ActivitySession
import com.poids.domain.model.ActivityType
import com.poids.domain.model.LiveActivityStats
import com.poids.domain.model.StepDay
import com.poids.domain.repository.ActivityRepository
import com.poids.domain.repository.ProfileRepository
import com.poids.domain.repository.StepRepository
import com.poids.data.tracking.TrackingStateHolder
import com.poids.tracking.ActivityTrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stepRepository: StepRepository,
    private val activityRepository: ActivityRepository,
    profileRepository: ProfileRepository,
    trackingStateHolder: TrackingStateHolder,
) : ViewModel() {

    val stepSensorAvailable: Boolean = stepRepository.sensorAvailable

    val stepDay: StateFlow<StepDay> = stepRepository.observeToday()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            StepDay(LocalDate.now(), 0, 0.0, 0.0),
        )

    val sessions: StateFlow<List<ActivitySession>> = activityRepository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val liveStats: StateFlow<LiveActivityStats?> = trackingStateHolder.stats

    /** Poids courant (kg) pour estimer les kcal d'une saisie manuelle ; 70 par défaut. */
    val weightKg: StateFlow<Double> = profileRepository.observeProfile()
        .map { it?.weightKg ?: 70.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 70.0)

    private var stepJob: Job? = null

    /** Estime les kcal d'un effort (MET net, comme les sessions GPS). */
    fun estimateKcal(type: ActivityType, durationSec: Long): Double =
        MetCalculator.caloriesBurned(type, weightKg.value, durationSec, net = true)

    /** Enregistre un effort saisi à la main (effort non mesuré). */
    fun addManualActivity(type: ActivityType, durationSec: Long, caloriesKcal: Double) {
        viewModelScope.launch {
            activityRepository.addManualSession(type, durationSec, caloriesKcal)
        }
    }

    /** À appeler après l'octroi de la permission ACTIVITY_RECOGNITION. */
    fun startStepTracking() {
        if (stepJob?.isActive == true) return
        stepJob = viewModelScope.launch { stepRepository.trackUntilCancelled() }
    }

    fun startGps(type: ActivityType) {
        ContextCompat.startForegroundService(
            context,
            ActivityTrackingService.startIntent(context, type),
        )
    }

    fun stopGps() {
        context.startService(ActivityTrackingService.stopIntent(context))
    }
}
