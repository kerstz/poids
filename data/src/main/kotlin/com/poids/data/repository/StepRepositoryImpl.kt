package com.poids.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.poids.data.local.dao.StepDao
import com.poids.data.local.entity.StepDayEntity
import com.poids.data.sensor.StepCounterDataSource
import com.poids.data.tracking.TrackingStateHolder
import com.poids.domain.calc.StepDistanceCalculator
import com.poids.domain.model.StepDay
import com.poids.domain.repository.ProfileRepository
import com.poids.domain.repository.StepRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class StepRepositoryImpl @Inject constructor(
    private val counter: StepCounterDataSource,
    private val stepDao: StepDao,
    private val dataStore: DataStore<Preferences>,
    private val profileRepository: ProfileRepository,
    private val trackingStateHolder: TrackingStateHolder,
) : StepRepository {

    override val sensorAvailable: Boolean get() = counter.available

    override fun observeToday(): Flow<StepDay> {
        val today = LocalDate.now()
        return stepDao.observeDay(today).map { it?.toDomain() ?: StepDay(today, 0, 0.0, 0.0) }
    }

    override fun observeRange(start: LocalDate, end: LocalDate): Flow<List<StepDay>> =
        stepDao.observeRange(start, end).map { list -> list.map { it.toDomain() } }

    override suspend fun trackUntilCancelled() {
        counter.counterFlow().collect { raw ->
            val today = LocalDate.now()
            val prefs = dataStore.data.first()
            val storedDate = prefs[KEY_DATE]
            val storedLast = prefs[KEY_LAST] ?: raw
            val storedSteps = prefs[KEY_STEPS] ?: 0
            val storedSessionSteps = prefs[KEY_SESSION_STEPS] ?: 0

            // delta = pas depuis la dernière lecture (gère le reboot : compteur remis à 0).
            val delta = if (storedDate != today.toEpochDay()) {
                0
            } else if (raw >= storedLast) {
                (raw - storedLast).toInt()
            } else {
                raw.toInt()
            }

            // Le total du jour accumule TOUS les pas, sport inclus (repères « pas/jour »).
            val newSteps = if (storedDate != today.toEpochDay()) 0 else storedSteps + delta
            // Les pas faits pendant une session GPS sont comptés à part : leur part
            // calorique sera retranchée du bilan (déjà comptée par la session MET).
            val newSessionSteps = when {
                storedDate != today.toEpochDay() -> 0
                trackingStateHolder.sessionActive -> storedSessionSteps + delta
                else -> storedSessionSteps
            }

            dataStore.edit {
                it[KEY_DATE] = today.toEpochDay()
                it[KEY_STEPS] = newSteps
                it[KEY_SESSION_STEPS] = newSessionSteps
                it[KEY_LAST] = raw
            }

            val profile = profileRepository.getProfile()
            val distance = profile?.let {
                StepDistanceCalculator.distanceMeters(newSteps, it.heightCm, it.sex)
            } ?: 0.0
            val kcal = profile?.let {
                StepDistanceCalculator.caloriesWalked(distance, it.weightKg)
            } ?: 0.0

            stepDao.upsert(StepDayEntity(today, newSteps, distance, kcal, newSessionSteps))
        }
    }

    private companion object {
        val KEY_DATE = longPreferencesKey("date")
        val KEY_STEPS = intPreferencesKey("steps")
        val KEY_SESSION_STEPS = intPreferencesKey("session_steps")
        val KEY_LAST = longPreferencesKey("last_counter")
    }
}
