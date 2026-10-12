// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.db

import android.database.sqlite.SQLiteConstraintException
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TallyDatabase::class.java,
    )

    private fun SupportSQLiteDatabase.food(
        id: Long, name: String, barcode: String?, archivedAt: Long? = null, updatedAt: Long = 1,
        kcal: Double = 100.0, sugars: Double? = null, brand: String? = null,
    ) = execSQL(
        "INSERT INTO food (id, name, brand, barcode, source, base_unit, energy_kcal, protein_g, carbs_g, sugars_g, fat_g, " +
            "archived_at, created_at, updated_at) VALUES (?, ?, ?, ?, 'USER', 'G', ?, 0, 0, ?, 0, ?, 1, ?)",
        arrayOf<Any?>(id, name, brand, barcode, kcal, sugars, archivedAt, updatedAt),
    )

    private fun SupportSQLiteDatabase.entry(
        id: Long, foodId: Long?, recipeId: Long?, qty: Double, unit: String, kcal: Double,
    ) = execSQL(
        "INSERT INTO journal_entry (id, local_date, meal_slot_id, food_id, recipe_id, quantity_base, quantity_unit, " +
            "energy_kcal, protein_g, carbs_g, fat_g, logged_at, zone_id, created_at, updated_at) " +
            "VALUES (?, 20000, 1, ?, ?, ?, ?, ?, 0, 0, 0, 1, 'Europe/Paris', 1, 1)",
        arrayOf<Any?>(id, foodId, recipeId, qty, unit, kcal),
    )

    @Test
    fun migrate1To2KeepsAllRowsAndFreezesJournal() {
        helper.createDatabase("mig", 1).apply {
            // Seed v1 : créneau 1 (hors callback), doublons de code-barres, aliment modifié, recette.
            execSQL("INSERT INTO meal_slot (id, name, builtin_key, position) VALUES (1, NULL, 'breakfast', 0)")
            food(1, "Yaourt", "3017620422003", sugars = 4.0, brand = "Marque", kcal = 60.0)   // doublon, archivé
            execSQL("UPDATE food SET archived_at = 5 WHERE id = 1")
            food(2, "Yaourt bis", "3017620422003", updatedAt = 9, kcal = 60.0, sugars = 4.0) // gardé (non archivé)
            food(3, "Sans code", null)
            food(4, "Sans code 2", null)
            food(5, "Pâtes", "111", kcal = 350.0, sugars = 3.0)
            execSQL(
                "INSERT INTO recipe (id, name, servings, created_at, updated_at) VALUES (1, 'Soupe', 2, 1, 1)",
            )
            food(6, "UPC-A", "123456789012", updatedAt = 3)            // 12 chiffres
            food(7, "EAN-13", "0123456789012", updatedAt = 4)          // même produit en 13 chiffres
            execSQL("UPDATE food SET base_unit = 'ML' WHERE id = 5")
            entry(1, foodId = 1, recipeId = null, qty = 200.0, unit = "G", kcal = 120.0)  // énergie cohérente
            entry(2, foodId = 5, recipeId = null, qty = 100.0, unit = "G", kcal = 999.0)  // aliment modifié depuis
            entry(3, foodId = null, recipeId = 1, qty = 1.0, unit = "SERVING", kcal = 80.0)
            close()
        }

        val db = helper.runMigrationsAndValidate("mig", 2, true, Migrations.MIGRATION_1_2)

        fun scalar(sql: String): Long = db.query(sql).use { it.moveToFirst(); it.getLong(0) }
        assertEquals(7, scalar("SELECT COUNT(*) FROM food"))
        // GTIN-13 : le doublon 12/13 chiffres ne garde le code que sur la ligne la plus récente.
        assertEquals(7, scalar("SELECT id FROM food WHERE barcode = '0123456789012'"))
        assertEquals(1, scalar("SELECT COUNT(*) FROM food WHERE id = 6 AND barcode IS NULL"))
        assertEquals(0, scalar("SELECT COUNT(*) FROM pragma_foreign_key_check"))
        assertEquals(3, scalar("SELECT COUNT(*) FROM journal_entry"))

        // Doublon : seule la ligne non archivée garde le code ; l'autre est conservée sans code.
        assertEquals(2, scalar("SELECT id FROM food WHERE barcode = '3017620422003'"))
        assertEquals(1, scalar("SELECT COUNT(*) FROM food WHERE id = 1 AND barcode IS NULL"))

        // Snapshot nom + marque.
        db.query("SELECT name, brand, sugars_g FROM journal_entry WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("Yaourt", it.getString(0))
            assertEquals("Marque", it.getString(1))
            assertEquals(true, it.isNull(2)) // détail laissé inconnu, jamais déduit
        }
        // Aliment modifié depuis : nutriments optionnels laissés inconnus, nom figé quand même.
        db.query("SELECT name, sugars_g FROM journal_entry WHERE id = 2").use {
            it.moveToFirst()
            assertEquals("Pâtes", it.getString(0))
            assertEquals(true, it.isNull(1))
        }
        db.query("SELECT name, brand, sugars_g FROM journal_entry WHERE id = 3").use {
            it.moveToFirst()
            assertEquals("Soupe", it.getString(0))
            assertNull(it.getString(1))
            assertEquals(true, it.isNull(2))
        }
        // Macros v1 intactes.
        assertEquals(999, scalar("SELECT CAST(energy_kcal AS INTEGER) FROM journal_entry WHERE id = 2"))

        // Index unique effectif.
        try {
            db.food(9, "Intrus", "111")
            fail("index unique attendu")
        } catch (_: SQLiteConstraintException) {
        }
        db.food(10, "NULL ok", null)
        db.close()
    }

    @Test
    fun migrate2To3AddsCiqualWithoutTouchingFood() {
        helper.createDatabase("mig23", 2).apply {
            food(1, "Pâtes", "3017620422003")
            close()
        }
        val db = helper.runMigrationsAndValidate("mig23", 3, true, Migrations.MIGRATION_2_3)
        db.query("SELECT name, barcode, ciqual_code FROM food WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("Pâtes", it.getString(0))
            assertEquals("3017620422003", it.getString(1))
            assertEquals(true, it.isNull(2))
        }
        db.execSQL("INSERT INTO food (name, source, base_unit, energy_kcal, protein_g, carbs_g, fat_g, ciqual_code, created_at, updated_at) VALUES ('A','CIQUAL','G',1,0,0,0,5,1,1)")
        try {
            db.execSQL("INSERT INTO food (name, source, base_unit, energy_kcal, protein_g, carbs_g, fat_g, ciqual_code, created_at, updated_at) VALUES ('B','CIQUAL','G',1,0,0,0,5,1,1)")
            fail("index unique ciqual_code attendu")
        } catch (_: SQLiteConstraintException) {
        }
        db.close()
    }

    @Test
    fun migrate3To4AddsSportTablesAndKeepsData() {
        helper.createDatabase("mig34", 3).apply {
            food(1, "Pâtes", "3017620422003")
            close()
        }
        val db = helper.runMigrationsAndValidate("mig34", 4, true, Migrations.MIGRATION_3_4)
        db.query("SELECT name FROM food WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("Pâtes", it.getString(0))
        }
        db.execSQL(
            "INSERT INTO activity_session (id, uuid, type, status, source, started_at, zone_id, local_date, " +
                "moving_ms, distance_m, elevation_gain_m, elevation_loss_m, created_at, updated_at) " +
                "VALUES (1, 'u', 'WALK', 'FINISHED', 'RECORDED', 1, 'UTC', 0, 0, 0, 0, 0, 1, 1)",
        )
        db.execSQL(
            "INSERT INTO track_point (session_id, seq, segment, time_ms, lat, lon, h_acc_m) " +
                "VALUES (1, 0, 0, 1, 48.0, 2.0, 5.0)",
        )
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM activity_session WHERE id = 1")
        db.query("SELECT COUNT(*) FROM track_point").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0)) // CASCADE
        }
        db.close()
    }

    @Test
    fun migrateAll1To4() {
        helper.createDatabase("mig14", 1).close()
        helper.runMigrationsAndValidate("mig14", 4, true, *TallyDatabase.ALL_MIGRATIONS).close()
    }

    /** Gel du schéma : chaque version exportée a sa migration n → n+1, et la dernière = `version` de la base. */
    @Test
    fun everyExportedSchemaVersionHasAMigrationToTheNext() {
        val dir = java.io.File("schemas/org.tally.data.db.TallyDatabase")
        val versions = dir.listFiles { f -> f.name.endsWith(".json") }!!
            .map { it.name.removeSuffix(".json").toInt() }.sorted()
        assertEquals((1..versions.last()).toList(), versions) // pas de trou dans l'historique
        val chain = TallyDatabase.ALL_MIGRATIONS.map { it.startVersion to it.endVersion }
        assertEquals((1 until versions.last()).map { it to it + 1 }, chain)
    }
}
