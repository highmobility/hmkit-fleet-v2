import io.mockk.mockkConstructor
import io.mockk.verify
import network.WebService
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

//import org.junit.jupiter.api.fail

class WebServiceTest {
    lateinit var webService:WebService
    var vins = listOf("vin1", "vin2")

    @BeforeEach
    fun before() {
        mockkConstructor(OkHttpClient::class)
        webService = WebService("apiKey")
    }

    // TODO: test that apiKey used is the same from koin init

    @Test
    fun createsAccessTokenBeforeRequest() {
        webService.clearVehicles(vins)
        verify { anyConstructed<OkHttpClient>().newCall(any()) }
    }

    @Test
    fun doesNotCreateAccessTokenIfExists() {
        fail<WebServiceTest>()
    }
}