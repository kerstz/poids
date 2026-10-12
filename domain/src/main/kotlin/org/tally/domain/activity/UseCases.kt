// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.domain.activity

import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import org.tally.core.time.AppClock
import org.tally.domain.body.GetWeightAt

class StartSession(
    private val repository: ActivityRepository,
    private val clock: AppClock,
) {
    /** Refuse s'il existe déjà une session active. Retourne l'id créé. */
    suspend operator fun invoke(type: ActivityType, zoneId: String, uuid: String = UUID.randomUUID().toString()): Long {
        check(repository.activeSession() == null) { "session déjà active" }
        val now = clock.nowMillis()
        val localDate = Instant.ofEpochMilli(now).atZone(ZoneId.of(zoneId)).toLocalDate().toEpochDay()
        return repository.create(
            ActivitySession(
                uuid = uuid,
                type = type,
                status = SessionStatus.RECORDING,
                startedAt = now,
                zoneId = zoneId,
                localDate = localDate,
                activeSince = now,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }
}

class PauseSession(
    private val repository: ActivityRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(id: Long) {
        val session = repository.get(id) ?: return
        if (session.status != SessionStatus.RECORDING) return
        val now = clock.nowMillis()
        repository.update(
            session.copy(
                status = SessionStatus.PAUSED,
                movingMs = session.movingMsAt(now),
                activeSince = null,
                updatedAt = now,
            ),
        )
    }
}

class ResumeSession(
    private val repository: ActivityRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(id: Long) {
        val session = repository.get(id) ?: return
        if (session.status != SessionStatus.PAUSED) return
        val now = clock.nowMillis()
        repository.update(session.copy(status = SessionStatus.RECORDING, activeSince = now, updatedAt = now))
    }
}

/** Clôture : agrégats recalculés depuis les points stockés, énergie si un poids est connu. */
class FinishSession(
    private val repository: ActivityRepository,
    private val getWeightAt: GetWeightAt,
    private val clock: AppClock,
) {
    suspend operator fun invoke(id: Long) {
        val session = repository.get(id) ?: return
        if (session.status == SessionStatus.FINISHED) return
        val now = clock.nowMillis()
        val aggregates = TrackAggregator.aggregate(
            repository.points(id),
            TrackFilterConfig.forType(session.type),
        )
        val energy = getWeightAt(session.startedAt)?.let {
            EstimateActivityEnergy.kcal(session.type, it, aggregates.distanceM, aggregates.elevationGainM)
        }
        repository.update(
            session.copy(
                status = SessionStatus.FINISHED,
                endedAt = now,
                movingMs = session.movingMsAt(now),
                activeSince = null,
                distanceM = aggregates.distanceM,
                elevationGainM = aggregates.elevationGainM,
                elevationLossM = aggregates.elevationLossM,
                energyKcal = energy,
                energyMethod = energy?.let { EstimateActivityEnergy.METHOD },
                updatedAt = now,
            ),
        )
    }
}

class DeleteSession(private val repository: ActivityRepository) {
    /** Retourne la session et ses points pour permettre l'annulation. */
    suspend operator fun invoke(id: Long): Pair<ActivitySession, List<TrackPoint>>? {
        val session = repository.get(id) ?: return null
        val points = repository.points(id)
        repository.delete(id)
        return session to points
    }
}

class RestoreSession(private val repository: ActivityRepository) {
    suspend operator fun invoke(session: ActivitySession, points: List<TrackPoint>) =
        repository.restore(session, points)
}

/** Remplit l'énergie des sessions terminées sans poids connu à l'époque. */
class RecomputeMissingEnergy(
    private val repository: ActivityRepository,
    private val getWeightAt: GetWeightAt,
    private val clock: AppClock,
) {
    suspend operator fun invoke() {
        for (session in repository.finishedWithoutEnergy()) {
            val weight = getWeightAt(session.startedAt) ?: continue
            val kcal = EstimateActivityEnergy.kcal(session.type, weight, session.distanceM, session.elevationGainM)
            repository.update(
                session.copy(
                    energyKcal = kcal,
                    energyMethod = EstimateActivityEnergy.METHOD,
                    updatedAt = clock.nowMillis(),
                ),
            )
        }
    }
}
