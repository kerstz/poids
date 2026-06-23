package com.poids.data.local.converter

import androidx.room.TypeConverter
import com.poids.domain.model.ActivityLevel
import com.poids.domain.model.ActivitySource
import com.poids.domain.model.ActivityType
import com.poids.domain.model.FoodSource
import com.poids.domain.model.GoalType
import com.poids.domain.model.MealType
import com.poids.domain.model.Sex
import com.poids.domain.model.SubstanceKind
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Convertisseurs Room. Dates/heures en primitifs sortables (epochDay / epochMilli),
 * énumérations par leur `name`.
 *
 * LocalDateTime est stocké en epoch-millis via un offset UTC fixe : sans fuseau, c'est
 * un round-trip stable ET numériquement triable (indispensable pour les range-queries).
 */
class Converters {

    // --- Temporel ---
    @TypeConverter fun dateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()
    @TypeConverter fun epochDayToDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun dateTimeToEpochMilli(value: LocalDateTime?): Long? =
        value?.toInstant(ZoneOffset.UTC)?.toEpochMilli()

    @TypeConverter
    fun epochMilliToDateTime(value: Long?): LocalDateTime? =
        value?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) }

    @TypeConverter fun instantToEpochMilli(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun epochMilliToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    // --- Énumérations ---
    @TypeConverter fun sexToString(v: Sex?): String? = v?.name
    @TypeConverter fun stringToSex(v: String?): Sex? = v?.let(Sex::valueOf)

    @TypeConverter fun activityLevelToString(v: ActivityLevel?): String? = v?.name
    @TypeConverter fun stringToActivityLevel(v: String?): ActivityLevel? = v?.let(ActivityLevel::valueOf)

    @TypeConverter fun goalTypeToString(v: GoalType?): String? = v?.name
    @TypeConverter fun stringToGoalType(v: String?): GoalType? = v?.let(GoalType::valueOf)

    @TypeConverter fun activityTypeToString(v: ActivityType?): String? = v?.name
    @TypeConverter fun stringToActivityType(v: String?): ActivityType? = v?.let(ActivityType::valueOf)

    @TypeConverter fun activitySourceToString(v: ActivitySource?): String? = v?.name
    @TypeConverter fun stringToActivitySource(v: String?): ActivitySource? = v?.let(ActivitySource::valueOf)

    @TypeConverter fun mealTypeToString(v: MealType?): String? = v?.name
    @TypeConverter fun stringToMealType(v: String?): MealType? = v?.let(MealType::valueOf)

    @TypeConverter fun foodSourceToString(v: FoodSource?): String? = v?.name
    @TypeConverter fun stringToFoodSource(v: String?): FoodSource? = v?.let(FoodSource::valueOf)

    @TypeConverter fun substanceKindToString(v: SubstanceKind?): String? = v?.name
    @TypeConverter fun stringToSubstanceKind(v: String?): SubstanceKind? = v?.let(SubstanceKind::valueOf)
}
