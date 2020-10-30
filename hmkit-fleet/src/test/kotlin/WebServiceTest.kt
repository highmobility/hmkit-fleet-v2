import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.WebService
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.koin.test.inject

class WebServiceTest : BaseTest() {
    val webService by inject<WebService>()
    val client by inject<OkHttpClient>()
    var vins = listOf("vin1", "vin2")

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    // TODO: test that apiKey used is the same from koin init

    @Test
    fun createsAccessTokenBeforeRequest() = runBlocking {
        // TODO:
        webService.clearVehicle(vins[0])
        verify { client.newCall(any()) }
    }

    @Test
    fun doesNotCreateAccessTokenIfExists() {
        fail<WebServiceTest>()
    }
}