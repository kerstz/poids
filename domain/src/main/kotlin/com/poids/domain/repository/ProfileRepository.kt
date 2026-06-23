package com.poids.domain.repository

import com.poids.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Accès au profil utilisateur (singleton applicatif).
 * L'implémentation vit dans :data ; le domaine ne connaît que ce contrat.
 */
interface ProfileRepository {
    /** Émet le profil courant, ou null tant qu'il n'est pas configuré. */
    fun observeProfile(): Flow<UserProfile?>

    suspend fun getProfile(): UserProfile?

    suspend fun saveProfile(profile: UserProfile)
}
