// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.nutrition

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tally.data.db.TallyDatabase
import org.tally.data.db.entity.FoodEntity
import org.tally.domain.model.BaseUnit
import org.tally.domain.model.FoodSource

/**
 * Mesure de la recherche `LIKE '%q%'` (scan complet) sur table `food` volumineuse.
 * Sert à trancher LIKE vs FTS4 (docs/AUDIT.md R13). Garde-fou large : le chiffre est rapporté.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FoodSearchBenchmarkTest {
    private lateinit var db: TallyDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), TallyDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun likeSearchStaysFastAtRealisticAndExtremeSizes() = runTest {
        val dao = db.foodDao()
        var inserted = 0
        for (target in listOf(5_000, 50_000)) {
            db.runInTransaction {
                kotlinx.coroutines.runBlocking {
                    while (inserted < target) {
                        dao.insert(
                            FoodEntity(
                                name = "Produit $inserted ${if (inserted % 97 == 0) "avoine" else "autre"}",
                                brand = "Marque ${inserted % 500}",
                                source = FoodSource.OFF,
                                baseUnit = BaseUnit.G,
                                energyKcal = 100.0, proteinG = 1.0, carbsG = 1.0, fatG = 1.0,
                                createdAt = 0, updatedAt = 0,
                            ),
                        )
                        inserted++
                    }
                }
            }
            val runs = 5
            val start = System.nanoTime()
            repeat(runs) { dao.search("avoine").first() }
            val avgMs = (System.nanoTime() - start) / 1e6 / runs
            println("BENCH like-search rows=$target avg=${"%.1f".format(avgMs)} ms")
            assertTrue("LIKE trop lent à $target lignes : $avgMs ms", avgMs < 1_500)
        }
    }
}
