package com.poids.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.poids.data.local.entity.ActivitySessionEntity
import com.poids.data.local.entity.RoutePointEntity
import kotlinx.coroutines.flow.Flow

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
}
