package org.tally.domain.nutrition

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tally.domain.model.FoodSource

class RemoteUseCasesTest {
    private class FakeFoods : FoodRepository {
        val stored = mutableListOf<Food>()
        override fun search(query: String): Flow<List<Food>> = emptyFlow()
        override suspend fun get(id: Long): FoodWithPortions? = null
        override suspend fun findByBarcode(barcode: String) = stored.firstOrNull { it.barcode == barcode }
        override suspend fun save(food: Food, portions: List<FoodPortion>): Long {
            val id = stored.size + 1L
            stored += food.copy(id = id)
            return id
        }
        override suspend fun archive(id: Long) = Unit
    }

    private class FakeRemote(var result: RemoteResult<RemoteFood>) : FoodRemoteSource {
        var calls = 0
        override suspend fun findByBarcode(barcode: String): RemoteResult<RemoteFood> { calls++; return result }
        override suspend fun search(query: String): RemoteResult<List<RemoteFood>> = RemoteResult.Unavailable
    }

    private val nutella = RemoteFood("3017620422003", "Nutella", "Ferrero", Macros(539.0, 6.3, 57.5, 30.9), 15.0, null)

    @Test
    fun invalidBarcodeNeverHitsNetwork() = runTest {
        val remote = FakeRemote(RemoteResult.Success(nutella))
        assertEquals(BarcodeLookup.NotFound, ResolveBarcode(FakeFoods(), remote)("123"))
        assertEquals(0, remote.calls)
    }

    @Test
    fun localFoodWinsOverNetwork() = runTest {
        val foods = FakeFoods().apply { stored += Food(1, "Nutella perso", barcode = nutella.barcode, per100 = Macros(1.0)) }
        val remote = FakeRemote(RemoteResult.Success(nutella))
        val result = ResolveBarcode(foods, remote)(nutella.barcode)
        assertTrue(result is BarcodeLookup.Local)
        assertEquals(0, remote.calls)
    }

    @Test
    fun remoteFallbackAndRateLimitPropagate() = runTest {
        val remote = FakeRemote(RemoteResult.Success(nutella))
        assertTrue(ResolveBarcode(FakeFoods(), remote)(nutella.barcode) is BarcodeLookup.Remote)
        remote.result = RemoteResult.RateLimited(5_000)
        assertEquals(BarcodeLookup.RateLimited(5_000), ResolveBarcode(FakeFoods(), remote)(nutella.barcode))
    }

    @Test
    fun importCopiesOnceAndKeepsServingAsPortion() = runTest {
        val foods = FakeFoods()
        val useCase = ImportRemoteFood(foods, SaveFood(foods))
        val first = useCase(nutella)!!
        val second = useCase(nutella)!!
        assertEquals(first.id, second.id)
        assertEquals(1, foods.stored.size)
        assertEquals(FoodSource.OFF, foods.stored.single().source)
    }
}
