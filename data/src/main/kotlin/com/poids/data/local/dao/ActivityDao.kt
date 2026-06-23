package com.poids.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.poids.data.local.entity.ActivitySessionEntity
import com.poids.data.local.entity.RoutePointEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ActivityDao {

    @Insert
    suspend fun insertSession(session: ActivitySessionEntity): Long

    @Insert
    suspend fun insertRoutePoints(points: List<RoutePointEntity>)

    @Query("SELECT * FROM activity_session ORDER BY startTime DESC")
    fun observeSessions(): Flow<List<ActivitySessionEntity>>

    @Query("SELECT * FROM route_point WHERE sessionId = :sessionId ORDER BY timestamp")
    suspend fun getRoute(sessionId: Long): List<RoutePointEntity>

    /** Total des calories brûlées par les sessions démarrées sur l'intervalle. */
    @Query("SELECT COALESCE(SUM(caloriesKcal), 0.0) FROM activity_session WHERE startTime >= :start AND startTime < :end")
    fun observeBurnedKcal(start: Instant, end: Instant): Flow<Double>
}
