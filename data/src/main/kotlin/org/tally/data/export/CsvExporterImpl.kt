// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.export

import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tally.data.db.TallyDatabase
import org.tally.domain.data.CsvExporter

/**
 * Exports CSV v1 (la version est dans le nom de fichier, pas dans une ligne `#`).
 * Valeurs brutes en unités SI ; `null` = champ vide ; texte libre protégé contre l'injection de formules.
 */
@Singleton
class CsvExporterImpl @Inject constructor(private val db: TallyDatabase) : CsvExporter {

    override suspend fun exportWeights(out: OutputStream): Int = withContext(Dispatchers.IO) {
        val dao = db.backupDao()
        val weightType = dao.measurementTypes().firstOrNull { it.key == WEIGHT_KEY }
        val rows = if (weightType == null) {
            emptyList()
        } else {
            dao.measurements().filter { it.typeId == weightType.id }.sortedWith(compareBy({ it.measuredAt }, { it.id }))
        }
        val writer = out.bufferedWriter(Charsets.UTF_8)
        val csv = CsvWriter(writer)
        csv.row("date", "time", "zone_id", "weight_kg", "note", "measured_at_utc")
        rows.forEach { m ->
            val at = Instant.ofEpochMilli(m.measuredAt)
            val local = at.atZone(zone(m.zoneId))
            csv.row(
                DATE.format(local), TIME.format(local), m.zoneId, CsvWriter.number(m.valueSi),
                CsvWriter.text(m.note), at.toString(),
            )
        }
        writer.flush()
        rows.size
    }

    override suspend fun exportJournal(out: OutputStream): Int = withContext(Dispatchers.IO) {
        val dao = db.backupDao()
        val slots = dao.mealSlots().associateBy { it.id }
        val foodSource = dao.foods().associate { it.id to it.source.name }
        val rows = dao.journalEntries().sortedWith(compareBy({ it.localEpochDay }, { it.loggedAt }, { it.id }))
        val writer = out.bufferedWriter(Charsets.UTF_8)
        val csv = CsvWriter(writer)
        csv.row(
            "date", "meal", "name", "brand", "quantity", "quantity_unit", "energy_kcal", "protein_g", "carbs_g",
            "fat_g", "sugars_g", "sat_fat_g", "fiber_g", "salt_g", "zone_id", "logged_at_utc", "source",
        )
        rows.forEach { j ->
            val slot = slots[j.mealSlotId]
            // Libellé stable : nom saisi, sinon clé interne (indépendante de la langue).
            val meal = slot?.name ?: slot?.builtinKey.orEmpty()
            val source = if (j.recipeId != null) "RECIPE" else foodSource[j.foodId].orEmpty()
            csv.row(
                DATE.format(LocalDate.ofEpochDay(j.localEpochDay)),
                CsvWriter.text(meal), CsvWriter.text(j.name), CsvWriter.text(j.brand),
                CsvWriter.number(j.quantityBase), j.quantityUnit.name.lowercase(),
                CsvWriter.number(j.energyKcal), CsvWriter.number(j.proteinG), CsvWriter.number(j.carbsG),
                CsvWriter.number(j.fatG), CsvWriter.number(j.sugarsG), CsvWriter.number(j.satFatG),
                CsvWriter.number(j.fiberG), CsvWriter.number(j.saltG), j.zoneId,
                Instant.ofEpochMilli(j.loggedAt).toString(), source,
            )
        }
        writer.flush()
        rows.size
    }

    private fun zone(id: String): ZoneId = runCatching { ZoneId.of(id) }.getOrDefault(ZoneId.of("UTC"))

    private companion object {
        const val WEIGHT_KEY = "builtin.weight"
        val DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    }
}
