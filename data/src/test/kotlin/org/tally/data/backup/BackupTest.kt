// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tally.core.time.AppClock
import org.tally.data.db.TallyDatabase
import org.tally.data.db.entity.AppSettingEntity
import org.tally.data.db.entity.FoodEntity
import org.tally.data.db.entity.FoodPortionEntity
import org.tally.data.db.entity.GoalEntity
import org.tally.data.db.entity.JournalEntryEntity
import org.tally.data.db.entity.MeasurementEntity
import org.tally.data.db.entity.ProfileEntity
import org.tally.data.db.entity.RecipeEntity
import org.tally.data.db.entity.RecipeIngredientEntity
import org.tally.data.export.CsvExporterImpl
import org.tally.domain.data.RestoreError
import org.tally.domain.data.RestoreResult
import org.tally.domain.model.ActivityLevel
import org.tally.domain.model.BaseUnit
import org.tally.domain.model.FoodSource
import org.tally.domain.model.GoalMode
import org.tally.domain.model.GoalType
import org.tally.domain.model.MeasurementSource
import org.tally.domain.model.QuantityUnit
import org.tally.domain.model.Sex

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupTest {
    private lateinit var context: Context
    private lateinit var source: TallyDatabase
    private lateinit var target: TallyDatabase
    private val clock = object : AppClock { override fun nowMillis() = 1_760_000_000_000L }

    private fun newDb() = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java)
        .allowMainThreadQueries()
        .addCallback(TallyDatabase.seedCallback)
        .build()

    private fun repo(db: TallyDatabase) = BackupRepositoryImpl(context, db, AppVersion("0.1.0", 1), clock)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        source = newDb()
        target = newDb()
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    /** Base riche : archivés, OFF + CIQUAL, nulls, unicode, guillemets, recette, flottants limites. */
    private suspend fun seed(db: TallyDatabase) {
        val dao = db.backupDao()
        val weightType = dao.measurementTypes().first { it.key == "builtin.weight" }.id
        val slot = dao.mealSlots().first().id
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO profile (id, birth_date, sex, height_m, activity_level, created_at, updated_at) " +
                "VALUES (1, 9000, 'FEMALE', 1.68, 'LIGHT', 1, 2)",
        )
        dao.insertSettings(listOf(AppSettingEntity("units.mass", "KG", 5), AppSettingEntity("ciqual.version", "2020-07-07", 6)))
        fun food(name: String, source: FoodSource, barcode: String?, ciqual: Int?, archived: Long?, k: Double) = FoodEntity(
            name = name, brand = "Marque \"Q\", é\nligne2", barcode = barcode, source = source, ciqualCode = ciqual,
            baseUnit = BaseUnit.G, energyKcal = k, proteinG = 0.1 + 0.2, carbsG = 1e-7, fatG = 3.0,
            sugarsG = null, satFatG = 0.0, archivedAt = archived, createdAt = 1, updatedAt = 2,
        )
        dao.insertFoods(
            listOf(
                food("Yaourt 🥛", FoodSource.OFF, "3017620422003", null, null, 60.0).copy(id = 1, offLastModified = 99),
                food("Pomme", FoodSource.CIQUAL, null, 20047, 7L, 52.0).copy(id = 2),
                food("Maison", FoodSource.USER, null, null, null, 100.0).copy(id = 3),
            ),
        )
        dao.insertPortions(listOf(FoodPortionEntity(1, 1, "1 pot", 125.0, true)))
        dao.insertRecipes(listOf(RecipeEntity(1, "Dessert", 2.0, null, "note", null, 1, 2)))
        dao.insertIngredients(listOf(RecipeIngredientEntity(1, 1, 1, 250.0, 0)))
        dao.insertGoals(listOf(GoalEntity(1, GoalType.ENERGY_KCAL, 2000.0, GoalMode.FIXED, 19000, 1)))
        dao.insertMeasurements(
            listOf(
                MeasurementEntity(1, weightType, 72.5, 1_759_000_000_000, "Europe/Paris", null, MeasurementSource.MANUAL, "matin", 1, 1),
                MeasurementEntity(2, weightType, 72.1, 1_759_100_000_000, "UTC", null, MeasurementSource.MANUAL, "=cmd", 1, 1),
            ),
        )
        fun entry(id: Long, foodId: Long?, recipeId: Long?, name: String, portionId: Long?) = JournalEntryEntity(
            id = id, name = name, brand = null, localEpochDay = 20000, mealSlotId = slot, foodId = foodId, recipeId = recipeId,
            portionId = portionId, portionCount = portionId?.let { 1.0 }, quantityBase = 125.0,
            quantityUnit = if (recipeId != null) QuantityUnit.SERVING else QuantityUnit.G,
            energyKcal = 75.0, proteinG = 5.0, carbsG = 6.0, fatG = 2.0, sugarsG = null, satFatG = 0.5,
            loggedAt = 1_759_000_000_000, zoneId = "Europe/Paris", createdAt = 1, updatedAt = 1,
        )
        dao.insertJournal(
            listOf(entry(1, 1, null, "Yaourt 🥛", 1), entry(2, null, 1, "Dessert", null), entry(3, 2, null, "Pomme", null)),
        )
    }

    private suspend fun fingerprint(db: TallyDatabase): List<Any> {
        val d = db.backupDao()
        return listOf(
            d.profile(), d.appSettings(emptyList()), d.mealSlots(), d.measurementTypes(), d.foods(), d.foodPortions(),
            d.recipes(), d.recipeIngredients(), d.goals(), d.measurements(), d.journalEntries(),
        )
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { z ->
            entries.forEach { (n, c) -> z.putNextEntry(ZipEntry(n)); z.write(c.toByteArray()); z.closeEntry() }
        }
        return bos.toByteArray()
    }

    private suspend fun export(db: TallyDatabase): ByteArray {
        val out = ByteArrayOutputStream()
        repo(db).export(out)
        return out.toByteArray()
    }

    private fun entryBytes(zip: ByteArray, name: String): String {
        val f = java.io.File.createTempFile("backup", ".zip").apply { writeBytes(zip) }
        return ZipFile(f).use { z -> z.getInputStream(z.getEntry(name)).readBytes().toString(Charsets.UTF_8) }.also { f.delete() }
    }

    @Test
    fun roundTripRestoresEveryRowAndSecondExportIsIdentical() = runTest {
        seed(source)
        val archive = export(source)
        // La cible n'est pas vide : elle est entièrement remplacée.
        target.backupDao().insertSettings(listOf(AppSettingEntity("ciqual.version", "device", 1), AppSettingEntity("x", "y", 1)))

        val result = repo(target).restore(ByteArrayInputStream(archive))
        assertTrue(result.toString(), result is RestoreResult.Success)

        val expected = fingerprint(source).toMutableList()
        // ciqual.version (réglage technique) n'est jamais sauvegardé.
        expected[1] = source.backupDao().appSettings(listOf("ciqual.version"))
        val actual = fingerprint(target).toMutableList()
        actual[1] = target.backupDao().appSettings(listOf("ciqual.version"))
        assertEquals(expected, actual)
        assertEquals("device", target.appSettingDaoValue("ciqual.version"))
        assertEquals(3, target.backupDao().foods().size)

        val second = export(target)
        listOf("food", "journal_entry", "measurement", "recipe", "app_setting").forEach { t ->
            assertEquals(entryBytes(archive, "data/$t.jsonl"), entryBytes(second, "data/$t.jsonl"))
        }
        val manifest = entryBytes(archive, "manifest.json")
        assertTrue(manifest.contains("Open Food Facts") && manifest.contains("Ciqual"))
    }

    private suspend fun TallyDatabase.appSettingDaoValue(key: String) = appSettingDao().get(key)

    private var targetSeeded = false

    private suspend fun assertRejected(archive: ByteArray, error: RestoreError) {
        if (!targetSeeded) { seed(target); targetSeeded = true }
        val before = fingerprint(target)
        val result = repo(target).restore(ByteArrayInputStream(archive))
        assertTrue(result.toString(), result is RestoreResult.Rejected && result.error == error)
        assertEquals("base modifiée malgré le rejet", before, fingerprint(target))
    }

    @Test
    fun rejectsGarbageFutureFormatBadChecksumAndBadCount() = runTest {
        seed(source)
        val good = export(source)
        assertRejected("pas un zip".toByteArray(), RestoreError.NOT_A_BACKUP)
        assertRejected(zipOf("manifest.json" to "{}"), RestoreError.NOT_A_BACKUP)

        val manifest = entryBytes(good, "manifest.json")
        fun rebuild(m: String, tweak: (String, String) -> String = { _, c -> c }): ByteArray {
            val tables = listOf("profile", "app_setting", "meal_slot", "measurement_type", "food", "food_portion",
                "recipe", "recipe_ingredient", "goal", "measurement", "journal_entry")
            return zipOf(
                *tables.map { "data/$it.jsonl" to tweak(it, entryBytes(good, "data/$it.jsonl")) }.toTypedArray(),
                "manifest.json" to m,
            )
        }
        assertRejected(rebuild(manifest.replace("\"formatVersion\":1", "\"formatVersion\":2")), RestoreError.FUTURE_FORMAT)
        assertRejected(
            rebuild(manifest) { t, c -> if (t == "food") c.replace("Pomme", "Poire") else c },
            RestoreError.CHECKSUM,
        )
        // Compteur faux dans le manifeste (sha inchangé).
        val badCount = Regex("\"food\":\\{\"rows\":3").replace(manifest, "\"food\":{\"rows\":4")
        assertRejected(rebuild(badCount), RestoreError.COUNT_MISMATCH)
    }

    @Test
    fun rejectsInvalidRowsWithoutTouchingTheDatabase() = runTest {
        seed(source)
        val good = export(source)
        val manifest = entryBytes(good, "manifest.json")
        val tables = listOf("profile", "app_setting", "meal_slot", "measurement_type", "food", "food_portion",
            "recipe", "recipe_ingredient", "goal", "measurement", "journal_entry")

        fun withTable(table: String, transform: (String) -> String): ByteArray {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val changed = transform(entryBytes(good, "data/$table.jsonl"))
            val sha = md.digest(changed.toByteArray()).joinToString("") { "%02x".format(it) }
            val m = Regex("(\"$table\":\\{\"rows\":\\d+,\"sha256\":\")[0-9a-f]+").replace(manifest) { it.groupValues[1] + sha }
            return zipOf(*tables.map { "data/$it.jsonl" to if (it == table) changed else entryBytes(good, "data/$it.jsonl") }.toTypedArray(), "manifest.json" to m)
        }
        // Énergie négative.
        assertRejected(withTable("food") { it.replaceFirst("\"energy_kcal\":60.0", "\"energy_kcal\":-5.0") }, RestoreError.INVALID_ROW)
        // Enum inconnu.
        assertRejected(withTable("food") { it.replaceFirst("\"source\":\"OFF\"", "\"source\":\"ALIEN\"") }, RestoreError.INVALID_ROW)
        // FK orpheline (portion d'un aliment absent).
        assertRejected(withTable("food_portion") { it.replace("\"food_id\":1", "\"food_id\":99") }, RestoreError.INVALID_ROW)
        // Poids hors bornes.
        assertRejected(withTable("measurement") { it.replaceFirst("\"value_si\":72.5", "\"value_si\":9999.0") }, RestoreError.INVALID_ROW)
        // Champ manquant / JSON invalide.
        assertRejected(withTable("goal") { "{\"id\":1}\n" }, RestoreError.INVALID_ROW)
        // Doublon de code-barres : refusé par la validation avant toute écriture.
        assertRejected(
            withTable("food") { it.replaceFirst("\"barcode\":null", "\"barcode\":\"3017620422003\"") },
            RestoreError.INVALID_ROW,
        )
        // Code-barres non normalisé (UPC-A 12 chiffres).
        assertRejected(
            withTable("food") { it.replaceFirst("\"barcode\":\"3017620422003\"", "\"barcode\":\"036000291452\"") },
            RestoreError.INVALID_ROW,
        )
    }

    @Test
    fun undoReturnsToThePreRestoreState() = runTest {
        seed(source)
        val archive = export(source)
        seedTargetDifferently()
        val before = fingerprint(target)
        val r = repo(target)
        assertTrue(r.restore(ByteArrayInputStream(archive)) is RestoreResult.Success)
        assertFalse(before == fingerprint(target))
        assertTrue(r.undoLastRestore() is RestoreResult.Success)
        assertEquals(before, fingerprint(target))
    }

    private suspend fun seedTargetDifferently() {
        target.backupDao().insertFoods(
            listOf(
                FoodEntity(
                    id = 50, name = "Local", source = FoodSource.USER, baseUnit = BaseUnit.G,
                    energyKcal = 10.0, proteinG = 1.0, carbsG = 1.0, fatG = 1.0, createdAt = 1, updatedAt = 1,
                ),
            ),
        )
    }

    @Test
    fun undoWithoutRestoreIsRejected() = runTest {
        java.io.File(context.filesDir, "backup/pre-restore.zip").delete()
        val r = repo(target).undoLastRestore()
        assertEquals(RestoreResult.Rejected(RestoreError.NOTHING_TO_UNDO), r)
    }

    @Test
    fun csvExportsWeightsAndJournalWithLocalTimeAndSource() = runTest {
        seed(source)
        val csv = CsvExporterImpl(source)
        val w = ByteArrayOutputStream()
        assertEquals(2, csv.exportWeights(w))
        val weightLines = w.toString("UTF-8").split("\r\n").filter { it.isNotEmpty() }
        assertEquals("date,time,zone_id,weight_kg,note,measured_at_utc", weightLines[0])
        assertTrue(weightLines[1].startsWith("2025-09-27,")) // 1_759_000_000 s = 2025-09-27T19:06:40Z → 21:06 à Paris
        assertTrue(weightLines[1].contains("Europe/Paris,72.5,matin,"))
        assertTrue("injection neutralisée", weightLines[2].contains(",'=cmd,"))

        val j = ByteArrayOutputStream()
        assertEquals(3, csv.exportJournal(j))
        val lines = j.toString("UTF-8").split("\r\n").filter { it.isNotEmpty() }
        assertTrue(lines.any { it.endsWith(",RECIPE") })
        assertTrue(lines.any { it.endsWith(",OFF") })
        assertTrue(lines.any { it.endsWith(",CIQUAL") })
        assertNotNull(lines.firstOrNull { it.contains("breakfast") })
    }

    /** Fixture figée (jamais régénérée) : garde-fou contre une dérive silencieuse du format v1. */
    @Test
    fun frozenV1FixtureStillRestores() = runTest {
        val stream = requireNotNull(javaClass.getResourceAsStream("/backup/v1-minimal.zip")) { "fixture absente" }
        val result = repo(target).restore(stream)
        assertTrue(result.toString(), result is RestoreResult.Success)
        val d = target.backupDao()
        assertEquals(listOf("Pâtes"), d.foods().map { it.name })
        assertEquals(72.5, d.measurements().single().valueSi, 0.0)
        assertEquals(280.0, d.journalEntries().single().energyKcal, 0.0)
        assertEquals(null, d.journalEntries().single().satFatG)
        assertEquals(4, d.mealSlots().size)
    }
}
