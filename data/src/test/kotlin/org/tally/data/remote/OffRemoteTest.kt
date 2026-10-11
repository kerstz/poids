package org.tally.data.remote

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tally.core.time.AppClock
import org.tally.core.unit.DistanceUnit
import org.tally.core.unit.EnergyUnit
import org.tally.core.unit.LengthUnit
import org.tally.core.unit.MassUnit
import org.tally.data.db.TallyDatabase
import org.tally.domain.nutrition.RemoteFood
import org.tally.domain.nutrition.RemoteResult
import org.tally.domain.settings.SettingsRepository
import org.tally.domain.settings.UnitPreferences

private const val PRODUCT_JSON = """{"code":"3017620422003","status":1,"product":{"code":"3017620422003",
"product_name":"Nutella","brands":"Nutella, Ferrero","serving_quantity":"15",
"nutriments":{"energy-kcal_100g":539,"proteins_100g":6.3,"carbohydrates_100g":57.5,"fat_100g":30.9},
"last_modified_t":1791733889}}"""

private const val SEARCH_JSON = """{"hits":[
{"code":"80050698","product_name":"Nutella","brands":["ferrero","Nutella"],"nutriments":{"energy_100g":2252,"proteins_100g":6}},
{"code":"111","product_name":"","brands":[],"nutriments":{"energy-kcal_100g":10}},
{"code":"222","product_name":"Sans énergie","nutriments":{"proteins_100g":3}}]}"""

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OffRemoteTest {
    private class FakeSettings(enabled: Boolean = true) : SettingsRepository {
        val off = MutableStateFlow(enabled)
        override val unitPreferences: Flow<UnitPreferences> = off.map { UnitPreferences() }
        override val offEnabled: Flow<Boolean> = off
        override suspend fun setOffEnabled(enabled: Boolean) { off.value = enabled }
        override suspend fun setMassUnit(unit: MassUnit) = Unit
        override suspend fun setLengthUnit(unit: LengthUnit) = Unit
        override suspend fun setDistanceUnit(unit: DistanceUnit) = Unit
        override suspend fun setEnergyUnit(unit: EnergyUnit) = Unit
    }

    private class Network {
        var code = 200
        var body = PRODUCT_JSON
        var headers = mapOf<String, String>()
        val requests = mutableListOf<okhttp3.Request>()

        val interceptor = Interceptor { chain ->
            requests += chain.request()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_2)
                .code(code)
                .message("test")
                .apply { headers.forEach { (k, v) -> header(k, v) } }
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }
    }

    private var now = 1_000_000L
    private val clock = object : AppClock { override fun nowMillis() = now }
    private val network = Network()
    private lateinit var db: TallyDatabase
    private lateinit var settings: FakeSettings
    private lateinit var source: CachedOffRemoteSource

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java).allowMainThreadQueries().build()
        settings = FakeSettings()
        val api = OffApi(
            client = OkHttpClient.Builder().addInterceptor(network.interceptor).build(),
            userAgent = "Tally/test (Android)",
            language = { "fr" },
        )
        source = CachedOffRemoteSource(api, db.offCacheDao(), settings, clock)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun userAgentCarriesAppNameVersionAndContactUrl() {
        val ua = offUserAgent("Tally", "0.1.0", "https://github.com/kerstz/poids")
        assertEquals("Tally/0.1.0 (Android; +https://github.com/kerstz/poids)", ua)
        assertTrue(ua.contains("Tally") && ua.contains("https://"))
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { offUserAgent("Tally", "0.1.0", "mailto:x@y.z") }
    }

    @Test
    fun productIsParsedAndSentWithUserAgent() = runTest {
        val result = source.findByBarcode("3017620422003") as RemoteResult.Success<RemoteFood>
        assertEquals("Nutella", result.value.name)
        assertEquals("Nutella", result.value.brand)
        assertEquals(539.0, result.value.per100.energyKcal, 0.0)
        assertEquals(15.0, result.value.servingQuantityG!!, 0.0)
        assertEquals("Tally/test (Android)", network.requests.single().header("User-Agent"))
        assertTrue(network.requests.single().url.toString().startsWith("https://world.openfoodfacts.org/api/v2/product/"))
    }

    @Test
    fun secondLookupIsServedFromCache() = runTest {
        source.findByBarcode("3017620422003")
        source.findByBarcode("3017620422003")
        assertEquals(1, network.requests.size)
        now += CachedOffRemoteSource.FOUND_TTL_MS + 1
        source.findByBarcode("3017620422003")
        assertEquals(2, network.requests.size)
    }

    @Test
    fun unknownProductIsCachedAsNotFound() = runTest {
        network.body = """{"code":"0000000000000","status":0,"status_verbose":"no code or invalid code"}"""
        assertEquals(RemoteResult.NotFound, source.findByBarcode("0000000000000"))
        assertEquals(RemoteResult.NotFound, source.findByBarcode("0000000000000"))
        assertEquals(1, network.requests.size)
    }

    @Test
    fun searchKeepsOnlyUsableProductsAndConvertsKilojoules() = runTest {
        network.body = SEARCH_JSON
        val result = source.search("nutella") as RemoteResult.Success<List<RemoteFood>>
        assertEquals(1, result.value.size)
        assertEquals(2252.0 / 4.184, result.value.single().per100.energyKcal, 1e-6)
        assertEquals("ferrero", result.value.single().brand)
        assertTrue(network.requests.single().url.toString().contains("langs=fr"))
    }

    @Test
    fun shortQueryDoesNotHitNetwork() = runTest {
        assertEquals(RemoteResult.Success(emptyList<RemoteFood>()), source.search("ab"))
        assertEquals(0, network.requests.size)
    }

    @Test
    fun searchIsLimitedLocallyBelowOffQuota() = runTest {
        network.body = SEARCH_JSON
        repeat(CachedOffRemoteSource.SEARCH_PER_MINUTE) { assertTrue(source.search("nutella") is RemoteResult.Success) }
        val limited = source.search("nutella")
        assertTrue(limited is RemoteResult.RateLimited)
        assertEquals(CachedOffRemoteSource.SEARCH_PER_MINUTE, network.requests.size)
        now += CachedOffRemoteSource.MINUTE_MS
        assertTrue(source.search("nutella") is RemoteResult.Success)
    }

    @Test
    fun serverRateLimitBlocksFurtherRequests() = runTest {
        network.code = 429
        network.headers = mapOf("Retry-After" to "30")
        val first = source.search("nutella") as RemoteResult.RateLimited
        assertEquals(30_000L, first.retryAfterMs)
        assertTrue(source.search("nutella") is RemoteResult.RateLimited)
        assertEquals(1, network.requests.size)
    }

    @Test
    fun disabledSettingBlocksAllRequests() = runTest {
        settings.setOffEnabled(false)
        assertEquals(RemoteResult.Disabled, source.findByBarcode("3017620422003"))
        assertEquals(RemoteResult.Disabled, source.search("nutella"))
        assertEquals(0, network.requests.size)
    }

    @Test
    fun serverErrorIsUnavailable() = runTest {
        network.code = 500
        assertEquals(RemoteResult.Unavailable, source.findByBarcode("3017620422003"))
    }
}
