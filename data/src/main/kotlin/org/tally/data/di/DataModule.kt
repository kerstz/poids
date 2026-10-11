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
import org.tally.data.db.dao.OffCacheDao
import org.tally.domain.nutrition.FoodRemoteSource
import org.tally.domain.nutrition.ImportRemoteFood
import org.tally.domain.nutrition.ResolveBarcode
import org.tally.core.time.AppClock
import org.tally.core.time.SystemAppClock
import org.tally.data.db.TallyDatabase
import org.tally.data.body.WeightRepositoryImpl
import org.tally.data.db.dao.AppSettingDao
import org.tally.data.db.dao.FoodDao
import org.tally.data.db.dao.GoalDao
import org.tally.data.db.dao.JournalDao
import org.tally.data.db.dao.MealSlotDao
import org.tally.data.db.dao.MeasurementDao
import org.tally.data.db.dao.RecipeDao
import org.tally.data.nutrition.FoodRepositoryImpl
import org.tally.data.nutrition.GoalRepositoryImpl
import org.tally.data.nutrition.JournalRepositoryImpl
import org.tally.data.nutrition.MealSlotRepositoryImpl
import org.tally.data.nutrition.RecipeRepositoryImpl
import org.tally.domain.nutrition.FoodRepository
import org.tally.domain.nutrition.GoalRepository
import org.tally.domain.nutrition.JournalRepository
import org.tally.domain.nutrition.LogFood
import org.tally.domain.nutrition.LogRecipe
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
    abstract fun bindRecipeRepository(impl: RecipeRepositoryImpl): RecipeRepository

    @Binds
    abstract fun bindJournalRepository(impl: JournalRepositoryImpl): JournalRepository

    @Binds
    abstract fun bindMealSlotRepository(impl: MealSlotRepositoryImpl): MealSlotRepository

    @Binds
    abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataProvidersModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TallyDatabase =
        Room.databaseBuilder(context, TallyDatabase::class.java, TallyDatabase.NAME)
            .addCallback(TallyDatabase.seedCallback)
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
    @Singleton
    fun provideClock(): AppClock = SystemAppClock
}
