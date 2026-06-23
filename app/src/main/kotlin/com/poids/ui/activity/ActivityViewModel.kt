package com.poids.ui.activity

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poids.domain.model.ActivitySession
import com.poids.domain.model.ActivityType
import com.poids.domain.model.LiveActivityStats
import com.poids.domain.model.StepDay
import com.poids.domain.repository.ActivityRepository
import com.poids.domain.repository.StepRepository
import com.poids.tracking.ActivityTrackingService
import com.poids.tracking.TrackingStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stepRepository: StepRepository,
    activityRepository: ActivityRepository,
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

    private var stepJob: Job? = null

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
