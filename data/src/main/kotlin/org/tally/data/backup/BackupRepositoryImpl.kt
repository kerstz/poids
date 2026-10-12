// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.backup

import android.content.Context
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.tally.core.barcode.Gtin
import org.tally.core.time.AppClock
import org.tally.data.backup.v1.AppInfoV1
import org.tally.data.backup.v1.AppSettingV1
import org.tally.data.backup.v1.AttributionV1
import org.tally.data.backup.v1.FoodPortionV1
import org.tally.data.backup.v1.FoodV1
import org.tally.data.backup.v1.GoalV1
import org.tally.data.backup.v1.JournalEntryV1
import org.tally.data.backup.v1.ManifestV1
import org.tally.data.backup.v1.MealSlotV1
import org.tally.data.backup.v1.MeasurementTypeV1
import org.tally.data.backup.v1.MeasurementV1
import org.tally.data.backup.v1.ProfileV1
import org.tally.data.backup.v1.RecipeIngredientV1
import org.tally.data.backup.v1.RecipeV1
import org.tally.data.backup.v1.TableInfoV1
import org.tally.data.db.TallyDatabase
import org.tally.data.db.dao.BackupData
import org.tally.data.db.dao.BackupDao
import org.tally.domain.body.WeightValidation
import org.tally.domain.data.BackupRepository
import org.tally.domain.data.BackupSummary
import org.tally.domain.data.RestoreError
import org.tally.domain.data.RestoreResult
import org.tally.domain.model.FoodSource
import org.tally.domain.nutrition.Food
import org.tally.domain.nutrition.FoodValidation
import org.tally.domain.nutrition.Macros

/** Version de l'application écrite dans le manifeste (fournie par `:app`). */
data class AppVersion(val name: String, val code: Long)

/** Rejet interne : converti en [RestoreResult.Rejected] à la frontière. */
private class Reject(val error: RestoreError, val table: String? = null, val line: Int? = null) : Exception(error.name)

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val db: TallyDatabase,
    private val version: AppVersion,
    private val clock: AppClock,
) : BackupRepository {
    private val dao: BackupDao get() = db.backupDao()

    override suspend fun export(out: OutputStream): BackupSummary = withContext(Dispatchers.IO) {
        BackupSummary(writeArchive(out))
    }

    override suspend fun restore(input: InputStream): RestoreResult = withContext(Dispatchers.IO) {
        val temp = File(context.cacheDir, "restore.zip")
        try {
            copyCapped(input, temp)
            restoreFile(temp)
        } catch (r: Reject) {
            RestoreResult.Rejected(r.error, r.table, r.line)
        } catch (_: IOException) {
            RestoreResult.Rejected(RestoreError.IO)
        } finally {
            temp.delete()
        }
    }

    override suspend fun undoLastRestore(): RestoreResult = withContext(Dispatchers.IO) {
        val saved = safetyFile()
        if (!saved.exists()) return@withContext RestoreResult.Rejected(RestoreError.NOTHING_TO_UNDO)
        try {
            // Le fichier est relu pendant que `restoreFile` réécrit la sauvegarde : on passe par une copie.
            val copy = File(context.cacheDir, "undo.zip")
            saved.copyTo(copy, overwrite = true)
            try {
                restoreFile(copy)
            } finally {
                copy.delete()
            }
        } catch (r: Reject) {
            RestoreResult.Rejected(r.error, r.table, r.line)
        } catch (_: IOException) {
            RestoreResult.Rejected(RestoreError.IO)
        }
    }

    // ---- export ----

    private suspend fun writeArchive(out: OutputStream): Map<String, Int> {
        val zip = ZipOutputStream(out)
        val infos = linkedMapOf<String, TableInfoV1>()
        val excluded = EXCLUDED_SETTING_KEYS.toList()

        fun <D> table(name: String, serializer: KSerializer<D>, rows: List<D>) {
            val md = MessageDigest.getInstance("SHA-256")
            zip.putNextEntry(ZipEntry("data/$name.jsonl").apply { time = FIXED_TIME })
            for (row in rows) {
                val bytes = (JSON.encodeToString(serializer, row) + "\n").toByteArray(Charsets.UTF_8)
                md.update(bytes)
                zip.write(bytes)
            }
            zip.closeEntry()
            infos[name] = TableInfoV1(rows.size, md.digest().toHex())
        }

        table("profile", ProfileV1.serializer(), dao.profile().map { it.toDto() })
        table("app_setting", AppSettingV1.serializer(), dao.appSettings(excluded).map { it.toDto() })
        table("meal_slot", MealSlotV1.serializer(), dao.mealSlots().map { it.toDto() })
        table("measurement_type", MeasurementTypeV1.serializer(), dao.measurementTypes().map { it.toDto() })
        val foods = dao.foods()
        table("food", FoodV1.serializer(), foods.map { it.toDto() })
        table("food_portion", FoodPortionV1.serializer(), dao.foodPortions().map { it.toDto() })
        table("recipe", RecipeV1.serializer(), dao.recipes().map { it.toDto() })
        table("recipe_ingredient", RecipeIngredientV1.serializer(), dao.recipeIngredients().map { it.toDto() })
        table("goal", GoalV1.serializer(), dao.goals().map { it.toDto() })
        table("measurement", MeasurementV1.serializer(), dao.measurements().map { it.toDto() })
        table("journal_entry", JournalEntryV1.serializer(), dao.journalEntries().map { it.toDto() })

        val attributions = buildList {
            if (foods.any { it.source == FoodSource.OFF }) {
                add(AttributionV1("Open Food Facts", "ODbL-1.0", "https://opendatacommons.org/licenses/odbl/1-0/"))
            }
            if (foods.any { it.source == FoodSource.CIQUAL }) {
                add(AttributionV1("Anses Ciqual 2020", "Licence Ouverte 1.0", "https://ciqual.anses.fr"))
            }
        }
        val manifest = ManifestV1(
            formatVersion = FORMAT_VERSION,
            app = AppInfoV1(version.name, version.code),
            dbSchemaVersion = DB_SCHEMA_VERSION,
            createdAt = Instant.ofEpochMilli(clock.nowMillis()).toString(),
            tables = infos,
            attributions = attributions,
        )
        zip.putNextEntry(ZipEntry(MANIFEST).apply { time = FIXED_TIME })
        zip.write(JSON.encodeToString(ManifestV1.serializer(), manifest).toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        zip.finish()
        out.flush()
        return infos.mapValues { it.value.rows }
    }

    // ---- restauration ----

    private suspend fun restoreFile(file: File): RestoreResult {
        val data = readAndValidate(file) // passe 1 : lecture seule, rien n'est modifié
        // Sauvegarde de sécurité de l'état courant, pour « Annuler ».
        val safety = safetyFile().also { it.parentFile?.mkdirs() }
        val tmp = File(safety.parentFile, "pre-restore.tmp")
        tmp.outputStream().use { writeArchive(it) }
        if (!tmp.renameTo(safety)) {
            tmp.copyTo(safety, overwrite = true)
            tmp.delete()
        }
        try {
            dao.replaceAll(data, EXCLUDED_SETTING_KEYS.toList()) // passe 2 : une seule transaction
        } catch (e: IllegalStateException) {
            throw Reject(RestoreError.COUNT_MISMATCH)
        } catch (e: android.database.SQLException) {
            throw Reject(RestoreError.INVALID_ROW)
        }
        return RestoreResult.Success(data.counts())
    }

    private fun safetyFile() = File(File(context.filesDir, "backup"), "pre-restore.zip")

    private fun copyCapped(input: InputStream, to: File) {
        var total = 0L
        to.outputStream().use { out ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                total += n
                if (total > MAX_BYTES) throw Reject(RestoreError.TOO_LARGE)
                out.write(buf, 0, n)
            }
        }
    }

    private fun readAndValidate(file: File): BackupData {
        val zip = try {
            ZipFile(file)
        } catch (_: java.util.zip.ZipException) {
            throw Reject(RestoreError.NOT_A_BACKUP)
        }
        zip.use {
            val expected = TABLES.map { "data/$it.jsonl" }.toSet() + MANIFEST
            val names = zip.entries().asSequence().map { it.name }.toList()
            if (names.toSet() != expected || names.size != expected.size) throw Reject(RestoreError.NOT_A_BACKUP)
            if (zip.entries().asSequence().sumOf { it.size } > MAX_BYTES) throw Reject(RestoreError.TOO_LARGE)

            val manifest = try {
                JSON.decodeFromString(
                    ManifestV1.serializer(),
                    zip.getInputStream(zip.getEntry(MANIFEST)).readBytes().toString(Charsets.UTF_8),
                )
            } catch (_: SerializationException) {
                throw Reject(RestoreError.NOT_A_BACKUP)
            } catch (_: IllegalArgumentException) {
                throw Reject(RestoreError.NOT_A_BACKUP)
            }
            if (manifest.formatVersion > FORMAT_VERSION) throw Reject(RestoreError.FUTURE_FORMAT)
            if (manifest.formatVersion != FORMAT_VERSION || manifest.tables.keys != TABLES.toSet()) {
                throw Reject(RestoreError.NOT_A_BACKUP)
            }

            fun <D> rows(name: String, serializer: KSerializer<D>): List<D> {
                val info = manifest.tables.getValue(name)
                val md = MessageDigest.getInstance("SHA-256")
                val out = ArrayList<D>(info.rows)
                DigestInputStream(zip.getInputStream(zip.getEntry("data/$name.jsonl")), md).bufferedReader(Charsets.UTF_8)
                    .use { reader ->
                        var n = 0
                        while (true) {
                            val line = reader.readLine() ?: break
                            n++
                            if (line.length > MAX_LINE) throw Reject(RestoreError.TOO_LARGE, name, n)
                            try {
                                out += JSON.decodeFromString(serializer, line)
                            } catch (_: SerializationException) {
                                throw Reject(RestoreError.INVALID_ROW, name, n)
                            } catch (_: IllegalArgumentException) {
                                throw Reject(RestoreError.INVALID_ROW, name, n)
                            }
                        }
                    }
                if (md.digest().toHex() != info.sha256) throw Reject(RestoreError.CHECKSUM, name)
                if (out.size != info.rows) throw Reject(RestoreError.COUNT_MISMATCH, name)
                return out
            }

            val profile = rows("profile", ProfileV1.serializer())
            val settings = rows("app_setting", AppSettingV1.serializer())
            val slots = rows("meal_slot", MealSlotV1.serializer())
            val types = rows("measurement_type", MeasurementTypeV1.serializer())
            val foods = rows("food", FoodV1.serializer())
            val portions = rows("food_portion", FoodPortionV1.serializer())
            val recipes = rows("recipe", RecipeV1.serializer())
            val ingredients = rows("recipe_ingredient", RecipeIngredientV1.serializer())
            val goals = rows("goal", GoalV1.serializer())
            val measurements = rows("measurement", MeasurementV1.serializer())
            val journal = rows("journal_entry", JournalEntryV1.serializer())

            return validate(profile, settings, slots, types, foods, portions, recipes, ingredients, goals, measurements, journal)
        }
    }

    @Suppress("LongParameterList")
    private fun validate(
        profile: List<ProfileV1>, settings: List<AppSettingV1>, slots: List<MealSlotV1>,
        types: List<MeasurementTypeV1>, foods: List<FoodV1>, portions: List<FoodPortionV1>,
        recipes: List<RecipeV1>, ingredients: List<RecipeIngredientV1>, goals: List<GoalV1>,
        measurements: List<MeasurementV1>, journal: List<JournalEntryV1>,
    ): BackupData {
        fun bad(table: String, i: Int): Nothing = throw Reject(RestoreError.INVALID_ROW, table, i + 1)
        fun ok(v: Double?) = v == null || (v.isFinite() && v >= 0.0)

        if (profile.size > 1 || profile.any { it.id != 1L }) bad("profile", 0)
        if (EXCLUDED_SETTING_KEYS.any { k -> settings.any { it.key == k } }) bad("app_setting", 0)
        slots.forEachIndexed { i, s -> if (s.position < 0) bad("meal_slot", i) }
        val slotKeys = slots.mapNotNull { it.builtinKey }.toSet()
        if (!slotKeys.containsAll(BUILTIN_MEALS)) throw Reject(RestoreError.MISSING_BUILTIN, "meal_slot")
        val weightType = types.firstOrNull { it.key == WEIGHT_KEY } ?: throw Reject(RestoreError.MISSING_BUILTIN, "measurement_type")

        foods.forEachIndexed { i, f ->
            val domain = Food(
                name = f.name, per100 = Macros(f.energyKcal, f.proteinG, f.carbsG, f.fatG),
            )
            val optionals = listOf(f.sugarsG, f.satFatG, f.fiberG, f.saltG, f.densityGPerMl)
            if (!FoodValidation.isValid(domain) || !optionals.all(::ok)) bad("food", i)
            if (f.barcode != null && f.barcode != Gtin.normalize(f.barcode)) bad("food", i)
            if (f.barcode != null && f.barcode.isEmpty()) bad("food", i)
        }
        val foodIds = foods.map { it.id }.toSet()
        if (foodIds.size != foods.size) bad("food", 0)
        val portionIds = HashSet<Long>()
        portions.forEachIndexed { i, p ->
            if (!p.quantityBase.isFinite() || p.quantityBase <= 0 || p.foodId !in foodIds || !portionIds.add(p.id)) bad("food_portion", i)
        }
        recipes.forEachIndexed { i, r -> if (r.name.isBlank() || !r.servings.isFinite() || r.servings <= 0 || !ok(r.cookedWeightG)) bad("recipe", i) }
        val recipeIds = recipes.map { it.id }.toSet()
        ingredients.forEachIndexed { i, g ->
            if (g.recipeId !in recipeIds || g.foodId !in foodIds || !g.quantityBase.isFinite() || g.quantityBase <= 0) bad("recipe_ingredient", i)
        }
        goals.forEachIndexed { i, g -> if (!ok(g.value)) bad("goal", i) }
        val typeIds = types.map { it.id }.toSet()
        measurements.forEachIndexed { i, m ->
            if (m.typeId !in typeIds || !m.valueSi.isFinite()) bad("measurement", i)
            if (m.typeId == weightType.id && !WeightValidation.isValid(m.valueSi)) bad("measurement", i)
        }
        val slotIds = slots.map { it.id }.toSet()
        journal.forEachIndexed { i, j ->
            val xor = (j.foodId != null) != (j.recipeId != null)
            val macros = listOf(j.energyKcal, j.proteinG, j.carbsG, j.fatG, j.sugarsG, j.satFatG, j.fiberG, j.saltG)
            val fk = j.mealSlotId in slotIds && (j.foodId == null || j.foodId in foodIds) &&
                (j.recipeId == null || j.recipeId in recipeIds) && (j.portionId == null || j.portionId in portionIds)
            if (!xor || !fk || !macros.all(::ok) || !j.quantityBase.isFinite() || j.quantityBase <= 0) bad("journal_entry", i)
        }

        return try {
            BackupData(
                profile = profile.map { it.toEntity() },
                appSettings = settings.map { it.toEntity() },
                mealSlots = slots.map { it.toEntity() },
                measurementTypes = types.map { it.toEntity() },
                foods = foods.map { it.toEntity() },
                foodPortions = portions.map { it.toEntity() },
                recipes = recipes.map { it.toEntity() },
                recipeIngredients = ingredients.map { it.toEntity() },
                goals = goals.map { it.toEntity() },
                measurements = measurements.map { it.toEntity() },
                journalEntries = journal.map { it.toEntity() },
            )
        } catch (_: IllegalArgumentException) {
            throw Reject(RestoreError.INVALID_ROW) // valeur d'enum inconnue
        }
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    private companion object {
        const val FORMAT_VERSION = 1
        const val DB_SCHEMA_VERSION = 3
        const val MANIFEST = "manifest.json"
        const val WEIGHT_KEY = "builtin.weight"
        const val MAX_BYTES = 256L * 1024 * 1024
        const val MAX_LINE = 1024 * 1024
        const val FIXED_TIME = 315_532_800_000L // 1980-01-01 : archives reproductibles
        val BUILTIN_MEALS = setOf("breakfast", "lunch", "dinner", "snack")
        /** Réglages techniques propres à l'appareil : ni sauvegardés, ni effacés à la restauration. */
        val EXCLUDED_SETTING_KEYS = setOf("ciqual.version")
        val TABLES = listOf(
            "profile", "app_setting", "meal_slot", "measurement_type", "food", "food_portion",
            "recipe", "recipe_ingredient", "goal", "measurement", "journal_entry",
        )
        val JSON = Json {
            encodeDefaults = true
            explicitNulls = true
            ignoreUnknownKeys = false
            allowSpecialFloatingPointValues = false
        }
    }
}
