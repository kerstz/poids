package com.poids.data.di

import android.content.Context
import androidx.room.Room
import com.poids.data.local.PoidsDatabase
import com.poids.data.local.dao.ActivityDao
import com.poids.data.local.dao.FoodItemDao
import com.poids.data.local.dao.MealLogDao
import com.poids.data.local.dao.SleepDao
import com.poids.data.local.dao.StepDao
import com.poids.data.local.dao.SubstanceDao
import com.poids.data.local.dao.UserProfileDao
import com.poids.data.local.dao.WaterDao
import com.poids.data.local.dao.WeightDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PoidsDatabase =
        Room.databaseBuilder(context, PoidsDatabase::class.java, PoidsDatabase.NAME)
            .addMigrations(PoidsDatabase.MIGRATION_1_2, PoidsDatabase.MIGRATION_2_3)
            .build()

    @Provides fun provideUserProfileDao(db: PoidsDatabase): UserProfileDao = db.userProfileDao()
    @Provides fun provideWeightDao(db: PoidsDatabase): WeightDao = db.weightDao()
    @Provides fun provideFoodItemDao(db: PoidsDatabase): FoodItemDao = db.foodItemDao()
    @Provides fun provideMealLogDao(db: PoidsDatabase): MealLogDao = db.mealLogDao()
    @Provides fun provideActivityDao(db: PoidsDatabase): ActivityDao = db.activityDao()
    @Provides fun provideStepDao(db: PoidsDatabase): StepDao = db.stepDao()
    @Provides fun provideSleepDao(db: PoidsDatabase): SleepDao = db.sleepDao()
    @Provides fun provideSubstanceDao(db: PoidsDatabase): SubstanceDao = db.substanceDao()
    @Provides fun provideWaterDao(db: PoidsDatabase): WaterDao = db.waterDao()
}
