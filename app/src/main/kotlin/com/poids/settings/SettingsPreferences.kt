package com.poids.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Réglages applicatifs persistés. */
data class AppSettings(
    /** Suivi de consommation de substances — désactivé par défaut (sensible). */
    val substanceTrackingEnabled: Boolean = false,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

@Singleton
class SettingsPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(substanceTrackingEnabled = prefs[KEY_SUBSTANCE_TRACKING] ?: false)
    }

    suspend fun setSubstanceTracking(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_SUBSTANCE_TRACKING] = enabled }
    }

    private companion object {
        val KEY_SUBSTANCE_TRACKING = booleanPreferencesKey("substance_tracking")
    }
}
