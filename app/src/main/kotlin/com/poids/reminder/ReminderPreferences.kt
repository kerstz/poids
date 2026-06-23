package com.poids.reminder

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Config du rappel de pesée : actif + heure. */
data class ReminderConfig(
    val enabled: Boolean = true,
    val hour: Int = 8,
    val minute: Int = 0,
)

private val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore("reminder")

@Singleton
class ReminderPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val config: Flow<ReminderConfig> = context.reminderDataStore.data.map { prefs ->
        ReminderConfig(
            enabled = prefs[KEY_ENABLED] ?: true,
            hour = prefs[KEY_HOUR] ?: 8,
            minute = prefs[KEY_MINUTE] ?: 0,
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.reminderDataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setTime(hour: Int, minute: Int) {
        context.reminderDataStore.edit {
            it[KEY_HOUR] = hour
            it[KEY_MINUTE] = minute
        }
    }

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("enabled")
        val KEY_HOUR = intPreferencesKey("hour")
        val KEY_MINUTE = intPreferencesKey("minute")
    }
}

/** Permet à l'Application (non injectable directement) d'accéder aux préférences. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun reminderPreferences(): ReminderPreferences
}
