import com.highmobility.crypto.Crypto
import com.highmobility.crypto.value.Signature
import com.highmobility.hmkit.HMKit
import com.highmobility.utils.Base64
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import model.Database
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
    val client = mockk<OkHttpClient>()
    val hmkitOem by inject<HMKit>()
    val database = mockk<Database>()
    val privateKey = configuration.getHmPrivateKey()
    val signature = Signature(privateKey.concat(privateKey))
    val crypto = mockk<Crypto> {
        every { signJWT(any<ByteArray>(), any()) } returns signature
    }
    val webService = WebService(configuration, client, hmkitOem, database, get())

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
        val request = slot<Request>()
        val call = mockk<Call>()
        val response = mockk<Response>(relaxed = true)
        val responseBody =
            "{\"token\":\"token\"}".toResponseBody("application/json".toMediaTypeOrNull())
        every { response.body } returns responseBody
        val callback = slot<Callback>()

        // extension functions need to be mocked https://github.com/mockk/mockk/issues/344
        mockkStatic("ru.gildor.coroutines.okhttp.CallAwaitKt")
        coEvery { call.await() } returns response
        every { call.enqueue(capture(callback)) } just Runs
        every { crypto.signJWT(any<ByteArray>(), any()) } returns signature
        every { client.newCall(capture(request)) } returns call

        val jwtContent = getJwtContent()
        val privateKey = configuration.getHmPrivateKey()

        val status = runBlocking {
            webService.clearVehicle(vins[0])
        }

        coVerify { crypto.signJWT(jwtContent.toByteArray(), privateKey) }

        coVerify { client.newCall(capture(request)) }

        assertTrue(request.captured.url.toString().endsWith("/auth_tokens"))

        // TODO: verify the returned auth token used in request
    }

    fun getJwtContent(): String {
        // otherwise these would be created again in the web service and they would not match
        every { configuration.createJti() } returns "jti"
        every { configuration.createIat() } returns 1001

        val header = buildJsonObject {
            put("alg", "ES256")
            put("typ", "JWT")
        }.toString()

        val jwtBody = buildJsonObject {
            put("ver", configuration.version)
            put("iss", configuration.apiKey)
            put("aud", ServiceAccountApiConfiguration.Environment.PRODUCTION.url)
            put("jti", configuration.createJti())
            put("iat", configuration.createIat())
        }.toString()

        val headerBase64 = Base64.encodeUrlSafe(header.toByteArray())
        val bodyBase64 = Base64.encodeUrlSafe(jwtBody.toByteArray())
        return String.format("%s.%s", headerBase64, bodyBase64)
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