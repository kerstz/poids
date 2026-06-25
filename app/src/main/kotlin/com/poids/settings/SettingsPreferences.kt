package com.poids.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
    /** Objectif d'hydratation choisi par l'utilisateur (ml) ; null = calculé du poids. */
    val waterGoalMl: Int? = null,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

@Singleton
class SettingsPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            substanceTrackingEnabled = prefs[KEY_SUBSTANCE_TRACKING] ?: false,
            waterGoalMl = prefs[KEY_WATER_GOAL]?.takeIf { it > 0 },
        )
    }

    suspend fun setSubstanceTracking(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_SUBSTANCE_TRACKING] = enabled }
    }

    /** Fixe l'objectif d'eau (ml), ou null pour revenir au calcul automatique. */
    suspend fun setWaterGoal(ml: Int?) {
        context.settingsDataStore.edit { prefs ->
            if (ml == null || ml <= 0) prefs.remove(KEY_WATER_GOAL) else prefs[KEY_WATER_GOAL] = ml
        }
    }

    private companion object {
        val KEY_SUBSTANCE_TRACKING = booleanPreferencesKey("substance_tracking")
        val KEY_WATER_GOAL = intPreferencesKey("water_goal_ml")
    }
}
