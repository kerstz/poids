package com.poids.data.repository

import com.poids.data.local.dao.UserProfileDao
import com.poids.domain.model.UserProfile
import com.poids.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val dao: UserProfileDao,
) : ProfileRepository {

    override fun observeProfile(): Flow<UserProfile?> =
        dao.observe().map { it?.toDomain() }

    override suspend fun getProfile(): UserProfile? = dao.get()?.toDomain()

    override suspend fun saveProfile(profile: UserProfile) =
        dao.upsert(profile.toEntity(updatedAt = Instant.now()))
}
