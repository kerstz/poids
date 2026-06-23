package com.poids

import android.app.Application
import com.poids.reminder.WeightReminderScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PoidsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Rappel de pesée quotidien (idempotent : remplace la planification existante).
        WeightReminderScheduler.schedule(this)
    }
}
