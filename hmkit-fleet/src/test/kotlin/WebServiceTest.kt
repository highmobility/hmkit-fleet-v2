import com.highmobility.crypto.Crypto
import com.highmobility.hmkit.HMKit
import com.highmobility.value.Bytes
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import network.WebService
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.koin.core.component.get
import org.koin.core.component.inject

class WebServiceTest : BaseTest() {
//    val webService by inject<WebService>()
    val client by inject<OkHttpClient>()
    val hmkitOem by inject<HMKit>()
    val crypto = mockk<Crypto>()
    val webService = WebService(configuration, client, hmkitOem, get())

    var vins = listOf("vin1", "vin2")

    @Before
    fun setUp() {
        every { hmkitOem.crypto } returns crypto
    }

    @After
    fun tearDown() {

    }

    @Test
    fun createsAccessTokenBeforeRequest() = runBlocking {
        webService.clearVehicle(vins[0])

        // TODO: verify correct input from configuration
        verify { crypto.signJWT(any<Bytes>(), any()) }

        // TODO: verify call made
        verify { client.newCall(any()) }
    }

    @Test
    fun doesNotCreateAccessTokenIfExists() {
        fail<WebServiceTest>()
    }

    @Test
    fun someTest() = runBlocking {

        return@runBlocking
    }
}