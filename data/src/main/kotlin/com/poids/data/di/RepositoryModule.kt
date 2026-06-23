package com.poids.data.di

import com.poids.data.repository.ActivityRepositoryImpl
import com.poids.data.repository.DailyEnergyRepositoryImpl
import com.poids.data.repository.FoodRepositoryImpl
import com.poids.data.repository.MealRepositoryImpl
import com.poids.data.repository.ProfileRepositoryImpl
import com.poids.data.repository.StepRepositoryImpl
import com.poids.data.repository.WeightRepositoryImpl
import com.poids.domain.repository.ActivityRepository
import com.poids.domain.repository.DailyEnergyRepository
import com.poids.domain.repository.FoodRepository
import com.poids.domain.repository.MealRepository
import com.poids.domain.repository.ProfileRepository
import com.poids.domain.repository.StepRepository
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

    @Binds
    @Singleton
    abstract fun bindFoodRepository(impl: FoodRepositoryImpl): FoodRepository

    @Binds
    @Singleton
    abstract fun bindMealRepository(impl: MealRepositoryImpl): MealRepository

    @Binds
    @Singleton
    abstract fun bindStepRepository(impl: StepRepositoryImpl): StepRepository

    @Binds
    @Singleton
    abstract fun bindActivityRepository(impl: ActivityRepositoryImpl): ActivityRepository
}
