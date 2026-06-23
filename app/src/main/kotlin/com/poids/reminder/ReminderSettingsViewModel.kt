package com.poids.reminder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: ReminderPreferences,
) : ViewModel() {

    val config: StateFlow<ReminderConfig> = prefs.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderConfig())

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setEnabled(enabled)
            reschedule()
        }
    }

    fun setTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefs.setTime(hour, minute)
            reschedule()
        }
    }

    private suspend fun reschedule() {
        val c = prefs.config.first()
        if (c.enabled) {
            WeightReminderScheduler.schedule(context, c.hour, c.minute)
        } else {
            WeightReminderScheduler.cancel(context)
        }
    }
}
