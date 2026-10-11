package org.tally.data.nutrition

import android.content.Context
import java.io.InputStream
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tally.core.time.AppClock
import org.tally.data.db.dao.AppSettingDao
import org.tally.data.db.dao.CiqualDao
import org.tally.data.db.entity.AppSettingEntity
import org.tally.data.db.entity.CiqualFoodEntity
import org.tally.domain.nutrition.CiqualFood
import org.tally.domain.nutrition.CiqualRepository
import org.tally.domain.nutrition.Macros
import org.tally.domain.nutrition.NutrientDetail

/** Minuscules, sans accents, espaces réduits : forme de recherche de `ciqual_food.name_norm`. */
internal fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

/** Lit le CSV produit par `tools/ciqual/convert.py` (en-tête, 11 colonnes, guillemets RFC 4180). */
internal object CiqualCsv {
    fun parse(input: InputStream): List<CiqualFoodEntity> =
        input.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.drop(1).filter { it.isNotBlank() }.map { row(splitLine(it)) }.toList()
        }

    private fun row(c: List<String>): CiqualFoodEntity {
        require(c.size == COLUMNS) { "ligne Ciqual invalide : ${c.size} colonnes" }
        fun d(i: Int) = c[i].toDouble()
        fun n(i: Int) = c[i].takeIf { it.isNotEmpty() }?.toDouble()
        return CiqualFoodEntity(
            code = c[0].toInt(), name = c[1], nameNorm = normalizeForSearch(c[1]), groupCode = c[2],
            energyKcal = d(3), proteinG = d(4), carbsG = d(5), fatG = d(6),
            sugarsG = n(7), satFatG = n(8), fiberG = n(9), saltG = n(10),
        )
    }

    internal fun splitLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                quoted && ch == '"' && line.getOrNull(i + 1) == '"' -> { sb.append('"'); i++ }
                ch == '"' -> quoted = !quoted
                ch == ',' && !quoted -> { out += sb.toString(); sb.clear() }
                else -> sb.append(ch)
            }
            i++
        }
        out += sb.toString()
        return out
    }

    private const val COLUMNS = 11
}

@Singleton
class CiqualRepositoryImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val dao: CiqualDao,
    private val settings: AppSettingDao,
    private val clock: AppClock,
) : CiqualRepository {
    private val loadLock = Mutex()

    override suspend fun search(query: String): List<CiqualFood> {
        val q = normalizeForSearch(query)
        if (q.isEmpty()) return emptyList()
        ensureLoaded()
        val escaped = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return dao.search(escaped).map { it.toDomain() }
    }

    /** Charge l'asset une fois par version (clé `ciqual.version`) ; relancé si la table est vide. */
    private suspend fun ensureLoaded() = loadLock.withLock {
        if (settings.get(VERSION_KEY) == VERSION && dao.count() > 0) return
        val items = withContext(Dispatchers.IO) { context.assets.open(ASSET).use(CiqualCsv::parse) }
        dao.replaceAll(items)
        settings.put(AppSettingEntity(VERSION_KEY, VERSION, clock.nowMillis()))
    }

    private fun CiqualFoodEntity.toDomain() = CiqualFood(
        code, name, groupCode, Macros(energyKcal, proteinG, carbsG, fatG),
        NutrientDetail(sugarsG, satFatG, fiberG, saltG),
    )

    private companion object {
        const val ASSET = "ciqual/ciqual_2020_07_07.csv"
        const val VERSION = "2020-07-07"
        const val VERSION_KEY = "ciqual.version"
    }
}
