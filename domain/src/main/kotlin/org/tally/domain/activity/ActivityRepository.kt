// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.domain.activity

import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    /** Sessions, plus récentes d'abord (active incluse). */
    fun observeSessions(): Flow<List<ActivitySession>>
    fun observeSession(id: Long): Flow<ActivitySession?>

    /** Session RECORDING ou PAUSED, s'il y en a une. */
    suspend fun activeSession(): ActivitySession?
    suspend fun get(id: Long): ActivitySession?
    suspend fun points(sessionId: Long): List<TrackPoint>
    suspend fun lastPoint(sessionId: Long): TrackPoint?

    suspend fun create(session: ActivitySession): Long
    suspend fun update(session: ActivitySession)

    /** Points + agrégats en une transaction. */
    suspend fun appendBatch(sessionId: Long, points: List<TrackPoint>, aggregates: SessionAggregates, now: Long)

    suspend fun delete(id: Long)

    /** Annulation d'une suppression : réinsère session (même id) et points. */
    suspend fun restore(session: ActivitySession, points: List<TrackPoint>)

    suspend fun finishedWithoutEnergy(): List<ActivitySession>
}
