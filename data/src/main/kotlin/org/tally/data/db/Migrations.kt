package org.tally.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    /**
     * v1 → v2 : journal figé (nom, marque, nutriments optionnels), `food.barcode` unique.
     * Aucune suppression de ligne : les doublons de code-barres perdent seulement leur code.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `journal_entry` ADD COLUMN `name` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `journal_entry` ADD COLUMN `brand` TEXT")
            db.execSQL("ALTER TABLE `journal_entry` ADD COLUMN `sugars_g` REAL")
            db.execSQL("ALTER TABLE `journal_entry` ADD COLUMN `sat_fat_g` REAL")
            db.execSQL("ALTER TABLE `journal_entry` ADD COLUMN `fiber_g` REAL")
            db.execSQL("ALTER TABLE `journal_entry` ADD COLUMN `salt_g` REAL")
            // Nom et marque : état courant de l'aliment/recette (la donnée d'origine v1 n'existe plus).
            db.execSQL(
                "UPDATE journal_entry SET " +
                    "name = COALESCE((SELECT name FROM food WHERE id = journal_entry.food_id), " +
                    "(SELECT name FROM recipe WHERE id = journal_entry.recipe_id), ''), " +
                    "brand = (SELECT brand FROM food WHERE id = journal_entry.food_id)",
            )
            // Nutriments optionnels : seulement si l'aliment n'a pas changé depuis (énergie recalculée égale).
            db.execSQL(
                "UPDATE journal_entry SET " +
                    "sugars_g = (SELECT f.sugars_g * journal_entry.quantity_base / 100.0 FROM food f WHERE f.id = journal_entry.food_id), " +
                    "sat_fat_g = (SELECT f.sat_fat_g * journal_entry.quantity_base / 100.0 FROM food f WHERE f.id = journal_entry.food_id), " +
                    "fiber_g = (SELECT f.fiber_g * journal_entry.quantity_base / 100.0 FROM food f WHERE f.id = journal_entry.food_id), " +
                    "salt_g = (SELECT f.salt_g * journal_entry.quantity_base / 100.0 FROM food f WHERE f.id = journal_entry.food_id) " +
                    "WHERE food_id IS NOT NULL AND quantity_unit IN ('G', 'ML') " +
                    "AND EXISTS (SELECT 1 FROM food f WHERE f.id = journal_entry.food_id " +
                    "AND ABS(f.energy_kcal * journal_entry.quantity_base / 100.0 - journal_entry.energy_kcal) < 0.01)",
            )
            // Doublons : on garde la ligne non archivée, puis la plus récente, puis le plus grand id.
            db.execSQL(
                "UPDATE food SET barcode = NULL WHERE barcode IS NOT NULL AND id <> (" +
                    "SELECT f2.id FROM food f2 WHERE f2.barcode = food.barcode " +
                    "ORDER BY (f2.archived_at IS NULL) DESC, f2.updated_at DESC, f2.id DESC LIMIT 1)",
            )
            db.execSQL("DROP INDEX IF EXISTS `index_food_barcode`")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_food_barcode` ON `food` (`barcode`)")
        }
    }
}
