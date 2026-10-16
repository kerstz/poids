// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import org.tally.data.remote.CachedOffRemoteSource
import org.tally.data.remote.OffApi
import org.tally.data.db.dao.CiqualDao
import org.tally.data.db.dao.OffCacheDao
import org.tally.domain.nutrition.FoodRemoteSource
import org.tally.domain.nutrition.ImportRemoteFood
import org.tally.domain.nutrition.ResolveBarcode
import org.tally.core.time.AppClock
import org.tally.core.time.SystemAppClock
import org.tally.data.db.TallyDatabase
import org.tally.data.body.WeightRepositoryImpl
import org.tally.data.activity.ActivityRepositoryImpl
import org.tally.data.db.dao.ActivityDao
import org.tally.data.db.dao.AppSettingDao
import org.tally.domain.activity.ActivityRepository
import org.tally.domain.activity.DeleteSession
import org.tally.domain.activity.FinishSession
import org.tally.domain.activity.PauseSession
import org.tally.domain.activity.RecomputeMissingEnergy
import org.tally.domain.activity.RestoreSession
import org.tally.domain.activity.ResumeSession
import org.tally.domain.activity.StartSession
import org.tally.data.db.dao.FoodDao
import org.tally.data.db.dao.GoalDao
import org.tally.data.db.dao.JournalDao
import org.tally.data.db.dao.MealSlotDao
import org.tally.data.db.dao.MeasurementDao
import org.tally.data.db.dao.RecipeDao
import org.tally.data.nutrition.CiqualRepositoryImpl
import org.tally.data.nutrition.FoodRepositoryImpl
import org.tally.data.nutrition.GoalRepositoryImpl
import org.tally.data.nutrition.JournalRepositoryImpl
import org.tally.data.nutrition.MealSlotRepositoryImpl
import org.tally.data.nutrition.RecipeRepositoryImpl
import org.tally.domain.nutrition.CiqualRepository
import org.tally.domain.data.BackupRepository
import org.tally.domain.data.CsvExporter
import org.tally.domain.data.DataWipeRepository
import org.tally.data.wipe.DataWipeRepositoryImpl
import org.tally.data.export.CsvExporterImpl
import org.tally.data.backup.BackupRepositoryImpl
import org.tally.domain.nutrition.FoodRepository
import org.tally.domain.nutrition.GoalRepository
import org.tally.domain.nutrition.JournalRepository
import org.tally.domain.nutrition.LogFood
import org.tally.domain.nutrition.ImportCiqualFood
import org.tally.domain.nutrition.LogRecipe
import org.tally.domain.nutrition.RestoreJournalEntry
import org.tally.domain.nutrition.MealSlotRepository
import org.tally.domain.nutrition.RecipeRepository
import org.tally.data.settings.SettingsRepositoryImpl
import org.tally.domain.body.GetWeightAt
import org.tally.domain.body.SaveWeight
import org.tally.domain.nutrition.SaveFood
import org.tally.domain.nutrition.SaveRecipe
import org.tally.domain.body.WeightRepository
import org.tally.domain.settings.SettingsRepository

/** User-Agent OFF (nom + version + contact) fourni par `:app`. Voir docs/AUDIT.md §3.4. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class OffUserAgent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RemoteBindingsModule {
    @Binds
    abstract fun bindFoodRemoteSource(impl: CachedOffRemoteSource): FoodRemoteSource
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {
    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    abstract fun bindWeightRepository(impl: WeightRepositoryImpl): WeightRepository

    @Binds
    abstract fun bindFoodRepository(impl: FoodRepositoryImpl): FoodRepository

    @Binds
    abstract fun bindCsvExporter(impl: CsvExporterImpl): CsvExporter

    @Binds
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    abstract fun bindDataWipeRepository(impl: DataWipeRepositoryImpl): DataWipeRepository

    @Binds
    abstract fun bindCiqualRepository(impl: CiqualRepositoryImpl): CiqualRepository

    @Binds
    abstract fun bindRecipeRepository(impl: RecipeRepositoryImpl): RecipeRepository

    @Binds
    abstract fun bindJournalRepository(impl: JournalRepositoryImpl): JournalRepository

    @Binds
    abstract fun bindMealSlotRepository(impl: MealSlotRepositoryImpl): MealSlotRepository

    @Binds
    abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository

    @Binds
    abstract fun bindActivityRepository(impl: ActivityRepositoryImpl): ActivityRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataProvidersModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TallyDatabase =
        Room.databaseBuilder(context, TallyDatabase::class.java, TallyDatabase.NAME)
            .addCallback(TallyDatabase.seedCallback)
            .addMigrations(*TallyDatabase.ALL_MIGRATIONS)
            .build()

    @Provides
    fun provideAppSettingDao(db: TallyDatabase): AppSettingDao = db.appSettingDao()

    @Provides
    fun provideMeasurementDao(db: TallyDatabase): MeasurementDao = db.measurementDao()

    @Provides
    fun provideFoodDao(db: TallyDatabase): FoodDao = db.foodDao()

    @Provides
    fun provideRecipeDao(db: TallyDatabase): RecipeDao = db.recipeDao()

    @Provides
    fun provideJournalDao(db: TallyDatabase): JournalDao = db.journalDao()

    @Provides
    fun provideMealSlotDao(db: TallyDatabase): MealSlotDao = db.mealSlotDao()

    @Provides
    fun provideGoalDao(db: TallyDatabase): GoalDao = db.goalDao()

    @Provides
    fun provideLogFood(journal: JournalRepository, clock: AppClock): LogFood = LogFood(journal, clock)

    @Provides
    fun provideLogRecipe(journal: JournalRepository, clock: AppClock): LogRecipe = LogRecipe(journal, clock)

    @Provides
    fun provideRestoreJournalEntry(journal: JournalRepository, foods: FoodRepository): RestoreJournalEntry =
        RestoreJournalEntry(journal, foods)

    @Provides
    fun provideCiqualDao(db: TallyDatabase): CiqualDao = db.ciqualDao()

    @Provides
    fun provideImportCiqualFood(foods: FoodRepository, saveFood: SaveFood): ImportCiqualFood =
        ImportCiqualFood(foods, saveFood)

    @Provides
    fun provideOffCacheDao(db: TallyDatabase): OffCacheDao = db.offCacheDao()

    @Provides
    @Singleton
    internal fun provideOffApi(@OffUserAgent userAgent: String): OffApi = OffApi(
        client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build(),
        userAgent = userAgent,
    )

    @Provides
    fun provideResolveBarcode(foods: FoodRepository, remote: FoodRemoteSource): ResolveBarcode =
        ResolveBarcode(foods, remote)

    @Provides
    fun provideImportRemoteFood(foods: FoodRepository, saveFood: SaveFood): ImportRemoteFood =
        ImportRemoteFood(foods, saveFood)

    @Provides
    fun provideSaveWeight(repository: WeightRepository): SaveWeight = SaveWeight(repository)

    @Provides
    fun provideSaveFood(repository: FoodRepository): SaveFood = SaveFood(repository)

    @Provides
    fun provideSaveRecipe(repository: RecipeRepository): SaveRecipe = SaveRecipe(repository)

    @Provides
    fun provideGetWeightAt(repository: WeightRepository): GetWeightAt = GetWeightAt(repository)

    @Provides
    fun provideActivityDao(db: TallyDatabase): ActivityDao = db.activityDao()

    @Provides
    fun provideStartSession(repository: ActivityRepository, clock: AppClock): StartSession =
        StartSession(repository, clock)

    @Provides
    fun providePauseSession(repository: ActivityRepository, clock: AppClock): PauseSession =
        PauseSession(repository, clock)

    @Provides
    fun provideResumeSession(repository: ActivityRepository, clock: AppClock): ResumeSession =
        ResumeSession(repository, clock)

    @Provides
    fun provideFinishSession(repository: ActivityRepository, weight: GetWeightAt, clock: AppClock): FinishSession =
        FinishSession(repository, weight, clock)

    @Provides
    fun provideDeleteSession(repository: ActivityRepository): DeleteSession = DeleteSession(repository)

    @Provides
    fun provideRestoreSession(repository: ActivityRepository): RestoreSession = RestoreSession(repository)

    @Provides
    fun provideRecomputeMissingEnergy(
        repository: ActivityRepository,
        weight: GetWeightAt,
        clock: AppClock,
    ): RecomputeMissingEnergy = RecomputeMissingEnergy(repository, weight, clock)

    @Provides
    @Singleton
    fun provideClock(): AppClock = SystemAppClock
}
