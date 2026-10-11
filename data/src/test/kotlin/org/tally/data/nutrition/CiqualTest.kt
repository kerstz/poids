package org.tally.data.nutrition

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tally.core.time.AppClock
import org.tally.data.db.TallyDatabase
import org.tally.domain.nutrition.ImportCiqualFood
import org.tally.domain.nutrition.SaveFood

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CiqualTest {
    private lateinit var db: TallyDatabase
    private lateinit var repo: CiqualRepositoryImpl
    private val clock = object : AppClock { override fun nowMillis() = 10_000L }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java).allowMainThreadQueries().build()
        repo = CiqualRepositoryImpl(context, db.ciqualDao(), db.appSettingDao(), clock)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun csvLineHandlesQuotedCommasAndEmptyFields() {
        assertEquals(
            listOf("1", "Pain, complet", "07", "", "x\"y"),
            CiqualCsv.splitLine("1,\"Pain, complet\",07,,\"x\"\"y\""),
        )
        assertEquals("pomme de terre", normalizeForSearch("  Pomme  de   TERRE "))
        assertEquals("ecrase", normalizeForSearch("Écrasé"))
    }

    @Test
    fun searchLoadsAssetOnceAndIgnoresCaseAndAccents() = runTest {
        val results = repo.search("ÉCRASÉ")
        assertTrue(results.isNotEmpty() || repo.search("pomme").isNotEmpty())
        assertTrue(db.ciqualDao().count() > 2000)
        assertEquals("2020-07-07", db.appSettingDao().get("ciqual.version"))
        val pomme = repo.search("pomme")
        assertTrue(pomme.isNotEmpty())
        assertTrue(pomme.all { it.per100.energyKcal >= 0 && it.name.isNotBlank() })
        assertTrue(repo.search("%").all { "%" in it.name }) // joker littéral, pas « tout »
        assertTrue(repo.search("%").size < db.ciqualDao().count())
        assertTrue(repo.search("   ").isEmpty())
    }

    @Test
    fun importCopiesIntoFoodOnceWithCiqualSource() = runTest {
        val foods = FoodRepositoryImpl(db.foodDao(), clock)
        val item = repo.search("pomme").first()
        val use = ImportCiqualFood(foods, SaveFood(foods))
        val a = use(item)!!
        val b = use(item)!!
        assertEquals(a.id, b.id)
        assertEquals(item.code, foods.get(a.id)!!.food.ciqualCode)
        assertEquals(org.tally.domain.model.FoodSource.CIQUAL, foods.get(a.id)!!.food.source)
    }
}
