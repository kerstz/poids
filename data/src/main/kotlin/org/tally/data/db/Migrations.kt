package org.tally.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    /**
     * v1 → v2 : journal figé (nom, marque, colonnes de détail à NULL), `food.barcode` unique.
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
            // Nutriments optionnels laissés NULL (« inconnu ») : un backfill déduit pourrait être faux.
            // GTIN-13 canonique avant la dédup : UPC-A (12) et EAN-13 « 0 »+UPC deviennent le même code.
            db.execSQL("UPDATE food SET barcode = '0' || barcode WHERE length(barcode) = 12")
            db.execSQL("UPDATE food SET barcode = substr(barcode, 2) WHERE length(barcode) = 14 AND barcode LIKE '0%'")
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
