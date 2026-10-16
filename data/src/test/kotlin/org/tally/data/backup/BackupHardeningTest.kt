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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tally.core.time.AppClock
import org.tally.data.db.TallyDatabase
import org.tally.data.db.entity.FoodEntity
import org.tally.data.remote.toRemoteFood
import org.tally.domain.data.RestoreError
import org.tally.domain.data.RestoreResult
import org.tally.domain.model.BaseUnit
import org.tally.domain.model.FoodSource

/** Intégrité (détail OFF négatif) et robustesse mémoire de l'import. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupHardeningTest {
    private lateinit var context: Context
    private lateinit var db: TallyDatabase
    private val clock = object : AppClock { override fun nowMillis() = 1_760_000_000_000L }

    private fun newDb() = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java)
        .allowMainThreadQueries().addCallback(TallyDatabase.seedCallback).build()

    private fun repo(d: TallyDatabase) = BackupRepositoryImpl(context, d, AppVersion("0.1.0", 1), clock)

    @Before
    fun setUp() { context = ApplicationProvider.getApplicationContext(); db = newDb() }

    @After
    fun tearDown() = db.close()

    private suspend fun export(d: TallyDatabase = db) = ByteArrayOutputStream().also { repo(d).export(it) }.toByteArray()

    private fun entries(zip: ByteArray): Map<String, ByteArray> =
        java.io.File.createTempFile("hard", ".zip").let { f ->
            f.writeBytes(zip)
            ZipFile(f).use { z -> z.entries().asSequence().associate { it.name to z.getInputStream(it).readBytes() } }
                .also { f.delete() }
        }

    private fun rebuild(base: Map<String, ByteArray>, replace: String, writer: (ZipOutputStream) -> Unit): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { z ->
            base.forEach { (n, c) ->
                z.putNextEntry(ZipEntry(n))
                if (n == replace) writer(z) else z.write(c)
                z.closeEntry()
            }
        }
        return bos.toByteArray()
    }

    private suspend fun assertRejected(archive: ByteArray, error: RestoreError) {
        val result = repo(newDb().also { extra -> extras += extra }).restore(ByteArrayInputStream(archive))
        assertTrue(result.toString(), result is RestoreResult.Rejected && result.error == error)
    }

    private val extras = mutableListOf<TallyDatabase>()

    @After
    fun closeExtras() = extras.forEach { it.close() }

    // ---- H8 ----

    @Test
    fun offProductWithNegativeDetailIsStoredAsNullAndBackupRestores() = runTest {
        val remote = Json.parseToJsonElement(
            """{"code":"3017620422003","product_name":"P","nutriments":{"energy-kcal_100g":100,"sugars_100g":-4,"salt_100g":-1}}""",
        ).jsonObject.toRemoteFood()!!
        db.backupDao().insertFoods(
            listOf(
                FoodEntity(
                    name = remote.name, barcode = remote.barcode, source = FoodSource.OFF, baseUnit = BaseUnit.G,
                    energyKcal = remote.per100.energyKcal, proteinG = remote.per100.proteinG,
                    carbsG = remote.per100.carbsG, fatG = remote.per100.fatG,
                    sugarsG = remote.per100Detail.sugarsG, saltG = remote.per100Detail.saltG,
                    createdAt = 1, updatedAt = 1,
                ),
            ),
        )
        val target = newDb().also { extras += it }
        val result = repo(target).restore(ByteArrayInputStream(export()))
        assertTrue(result.toString(), result is RestoreResult.Success)
        assertNull(target.backupDao().foods().single().sugarsG)
    }

    @Test
    fun negativeOptionalAlreadyInDatabaseIsSanitizedOnExport() = runTest {
        db.backupDao().insertFoods(
            listOf(
                FoodEntity(
                    name = "Ancien", source = FoodSource.USER, baseUnit = BaseUnit.G, energyKcal = 50.0, proteinG = 1.0,
                    carbsG = 2.0, fatG = 3.0, sugarsG = -1.0, satFatG = -2.0, fiberG = 4.0, saltG = -0.5,
                    densityGPerMl = -1.0, createdAt = 1, updatedAt = 1,
                ),
            ),
        )
        val target = newDb().also { extras += it }
        val result = repo(target).restore(ByteArrayInputStream(export()))
        assertTrue(result.toString(), result is RestoreResult.Success)
        val f = target.backupDao().foods().single()
        assertNull(f.sugarsG); assertNull(f.satFatG); assertNull(f.saltG); assertNull(f.densityGPerMl)
        assertEquals(4.0, f.fiberG!!, 0.0)
        assertEquals(50.0, f.energyKcal, 0.0) // macros obligatoires inchangées
    }

    // ---- M3 ----

    @Test
    fun lineWithoutNewlineBeyondLimitIsRejected() = runTest {
        val base = entries(export())
        val big = rebuild(base, "data/app_setting.jsonl") { z ->
            val chunk = ByteArray(64 * 1024) { 'a'.code.toByte() }
            repeat(18) { z.write(chunk) } // ~1,1 Mo sans '\n'
        }
        assertRejected(big, RestoreError.TOO_LARGE)
    }

    @Test
    fun manifestBeyond64KbIsRejected() = runTest {
        val base = entries(export())
        val big = rebuild(base, "manifest.json") { z ->
            z.write("{\"pad\":\"".toByteArray()); z.write(ByteArray(70 * 1024) { 'a'.code.toByte() }); z.write("\"}".toByteArray())
        }
        assertRejected(big, RestoreError.TOO_LARGE)
    }

    @Test
    fun zipBombWithForgedSmallDeclaredSizeIsRejectedByActualByteCount() = runTest {
        val base = entries(export())
        val bomb = rebuild(base, "data/app_setting.jsonl") { z ->
            // 300 lignes de ~1 Mo d'espaces (< MAX_LINE) : ~300 Mo décompressés, quelques centaines de Ko compressés.
            val spaces = ByteArray(1_000_000) { ' '.code.toByte() }
            val json = "{\"key\":\"k\",\"value\":\"v\",\"updated_at\":1}\n".toByteArray()
            repeat(300) { z.write(spaces); z.write(json) }
        }
        forgeUncompressedSize(bomb, "data/app_setting.jsonl", 10)
        // Le test n'a de sens que si la taille déclarée est bien falsifiée.
        java.io.File.createTempFile("bomb", ".zip").also { it.writeBytes(bomb) }.let { f ->
            ZipFile(f).use { z -> assertEquals(10L, z.getEntry("data/app_setting.jsonl").size) }
            f.delete()
        }
        assertRejected(bomb, RestoreError.TOO_LARGE)
    }

    /** Réécrit la taille non compressée dans l'en-tête du répertoire central (octets 24..27 de l'entrée). */
    private fun forgeUncompressedSize(zip: ByteArray, name: String, size: Int) {
        val nameBytes = name.toByteArray()
        for (i in 0..zip.size - 46) {
            if (zip[i] == 0x50.toByte() && zip[i + 1] == 0x4b.toByte() && zip[i + 2] == 0x01.toByte() && zip[i + 3] == 0x02.toByte()) {
                val len = (zip[i + 28].toInt() and 0xff) or ((zip[i + 29].toInt() and 0xff) shl 8)
                if (len == nameBytes.size && zip.copyOfRange(i + 46, i + 46 + len).contentEquals(nameBytes)) {
                    for (k in 0..3) zip[i + 24 + k] = ((size shr (8 * k)) and 0xff).toByte()
                    return
                }
            }
        }
        error("entrée introuvable")
    }
}
