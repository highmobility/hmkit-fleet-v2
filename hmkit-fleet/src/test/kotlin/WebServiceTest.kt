import com.highmobility.crypto.Crypto
import com.highmobility.crypto.value.Signature
import com.highmobility.hmkit.HMKit
import com.highmobility.utils.Base64
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import network.WebService
import okhttp3.*
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.component.get
import org.koin.core.component.inject
import ru.gildor.coroutines.okhttp.await

class WebServiceTest : BaseTest() {
    //    val webService by inject<WebService>()

    val client = mockk<OkHttpClient>()
    val hmkitOem by inject<HMKit>()

    val privateKey = configuration.getHmPrivateKey()
    val signature = Signature(privateKey.concat(privateKey))
    val crypto = mockk<Crypto> {
        every { signJWT(any<ByteArray>(), any()) } returns signature
    }
    val webService = WebService(configuration, client, hmkitOem, get())

    var vins = listOf("vin1", "vin2")

    @BeforeEach
    fun setUp() {
        every { hmkitOem.crypto } returns crypto
    }

    @After
    fun tearDown() {

    }

    @Test
    fun createsAccessTokenBeforeRequest() {
        val jwtMap = mapOf(
            "ver" to configuration.version,
            "iss" to configuration.apiKey,
            "aud" to configuration.baseUrl,
            "iat" to System.currentTimeMillis() / 1000
        )

        val jwtContent = Base64.encode(jwtMap.toString().toByteArray())
        val request = slot<Request>()
        val call = mockk<Call>()
        val response = mockk<Response>()
        val responseBody = "{\"token\":\"token\"}".toResponseBody("application/json".toMediaTypeOrNull())
        every { response.body } returns responseBody
        val callback = slot<Callback>()
        // extension functions need to be mocked https://github.com/mockk/mockk/issues/344

        mockkStatic("ru.gildor.coroutines.okhttp.CallAwaitKt")

        coEvery { call.await() } returns response
        every { call.enqueue(capture(callback)) } just Runs
        every { crypto.signJWT(any<ByteArray>(), any()) } returns signature
        every { client.newCall(capture(request)) } returns call

        val status = runBlocking {
            webService.clearVehicle(vins[0])
        }

        coVerify { crypto.signJWT(jwtContent.toByteArray(), privateKey) }

        coVerify { client.newCall(capture(request)) }

        assertTrue(request.captured.url.toString().endsWith("/auth_tokens"))

        // TODO: verify the returned auth token used in request
    }

    @Test
    fun doesNotCreateAccessTokenIfExists() {
        // check that no request is made when token exists
        fail<WebServiceTest>()
    }

    @Test
    fun someTest() = runBlocking {

        return@runBlocking
    }
}