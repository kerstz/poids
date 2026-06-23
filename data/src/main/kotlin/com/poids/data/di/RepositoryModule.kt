package com.poids.data.di

import com.poids.data.repository.DailyEnergyRepositoryImpl
import com.poids.data.repository.ProfileRepositoryImpl
import com.poids.data.repository.WeightRepositoryImpl
import com.poids.domain.repository.DailyEnergyRepository
import com.poids.domain.repository.ProfileRepository
import com.poids.domain.repository.WeightRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Lie les interfaces de repository (domaine) à leurs implémentations (data).
 * Les autres repositories (food, meal, activity, step, habit) seront ajoutés ici
 * au fil des features.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindWeightRepository(impl: WeightRepositoryImpl): WeightRepository

    @Binds
    @Singleton
    abstract fun bindDailyEnergyRepository(impl: DailyEnergyRepositoryImpl): DailyEnergyRepository
}
