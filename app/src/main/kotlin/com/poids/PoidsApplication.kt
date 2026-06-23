package com.poids

import android.app.Application
import com.poids.reminder.ReminderEntryPoint
import com.poids.reminder.WeightReminderScheduler
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class PoidsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleReminderFromPrefs()
    }

    /** Replanifie le rappel selon la config persistée (heure choisie / désactivé). */
    private fun scheduleReminderFromPrefs() {
        val prefs = EntryPointAccessors
            .fromApplication(this, ReminderEntryPoint::class.java)
            .reminderPreferences()
        CoroutineScope(Dispatchers.Default).launch {
            val config = prefs.config.first()
            if (config.enabled) {
                WeightReminderScheduler.schedule(this@PoidsApplication, config.hour, config.minute)
            } else {
                WeightReminderScheduler.cancel(this@PoidsApplication)
            }
        }
    }
}
