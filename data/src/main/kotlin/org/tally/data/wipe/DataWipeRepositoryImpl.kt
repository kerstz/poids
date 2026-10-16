// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.wipe

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tally.data.db.TallyDatabase
import org.tally.data.db.entity.MealSlotEntity
import org.tally.data.db.entity.MeasurementTypeEntity
import org.tally.domain.data.DataWipeRepository
import org.tally.domain.model.MeasurementDimension

/**
 * Effacement total des données utilisateur (RGPD art. 17). Les réglages vivent dans Room (`app_setting`),
 * il n'y a pas de DataStore. Le référentiel CIQUAL est conservé ; créneaux de repas et types de mesure
 * intégrés sont recréés. Le fichier de sauvegarde de sécurité (`pre-restore.zip`) est supprimé.
 */
@Singleton
class DataWipeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: TallyDatabase,
) : DataWipeRepository {
    override suspend fun wipeAll() = withContext(Dispatchers.IO) {
        db.backupDao().wipeUserData(PRESERVED_SETTINGS, builtinSlots(), builtinTypes())
        // Le fichier de sécurité contient une copie complète des données : suppression après la base.
        File(File(context.filesDir, "backup"), "pre-restore.zip").delete()
        File(File(context.filesDir, "backup"), "pre-restore.tmp").delete()
        // Copies temporaires éventuelles d'une restauration interrompue.
        File(context.cacheDir, "restore.zip").delete()
        File(context.cacheDir, "undo.zip").delete()
        Unit
    }

    private fun builtinSlots() =
        listOf("breakfast", "lunch", "dinner", "snack").mapIndexed { i, key ->
            MealSlotEntity(name = null, builtinKey = key, position = i)
        }

    private fun builtinTypes() = listOf(
        MeasurementTypeEntity(key = "builtin.weight", dimension = MeasurementDimension.MASS, position = 0),
        MeasurementTypeEntity(key = "builtin.body_fat", dimension = MeasurementDimension.PERCENT, position = 1),
        MeasurementTypeEntity(key = "builtin.waist", dimension = MeasurementDimension.LENGTH, position = 2),
        MeasurementTypeEntity(key = "builtin.hips", dimension = MeasurementDimension.LENGTH, position = 3),
    )

    private companion object {
        /** Réglage technique lié au référentiel CIQUAL conservé. */
        val PRESERVED_SETTINGS = listOf("ciqual.version")
    }
}
