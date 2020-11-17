import com.highmobility.crypto.Crypto
import com.highmobility.crypto.value.Signature
import com.highmobility.hmkit.HMKit
import com.highmobility.utils.Base64
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import network.WebService
import okhttp3.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.koin.core.component.get
import org.koin.core.component.inject
import java.net.HttpURLConnection

internal class WebServiceTest : BaseTest() {
    val mockWebServer = MockWebServer()

    val client = OkHttpClient()

    val hmkitOem by inject<HMKit>()
    val privateKey = configuration.getHmPrivateKey()
    val signature = Signature(privateKey.concat(privateKey))
    val crypto = mockk<Crypto> {
        every { signJWT(any<ByteArray>(), any()) } returns signature
    }
//    val webService = WebService(client, hmkitOem, get(), HMKitFleet.Environment.DEV.url)

    var vins = listOf("vin1", "vin2")

    @BeforeEach
    fun setUp() {
        every { hmkitOem.crypto } returns crypto
        mockWebServer.start()
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun parsesAuthTokenResponse() {
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_CREATED)
            .setBody(
                "{\"auth_token\":\"e903cb43-27b1-4e47-8922-c04ecd5d2019\"," +
                        "\"valid_from\":\"2020-11-17T04:50:16\"," +
                        "\"valid_until\":\"2020-11-17T05:50:16\"}"
            )

        mockWebServer.enqueue(mockResponse)

        val baseUrl: HttpUrl = mockWebServer.url("")
        val webService = WebService(client, hmkitOem, get(), baseUrl.toString())

        every { crypto.signJWT(any<ByteArray>(), any()) } returns signature

        val jwtContent = getJwtContent()
        val privateKey = configuration.getHmPrivateKey()

        val status = runBlocking {
            webService.getAuthToken(configuration)
        }

        coVerify { crypto.signJWT(jwtContent.toByteArray(), privateKey) }

        val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
        assertTrue(recordedRequest.path!!.endsWith("/auth_tokens"))

        assertTrue(status.response!!.authToken == "e903cb43-27b1-4e47-8922-c04ecd5d2019")
        assertTrue(status.response!!.validFrom.toString() == "2020-11-17T04:50:16")
        assertTrue(status.response!!.validUntil.toString() == "2020-11-17T05:50:16")
    }

    @Test
    fun parsesAuthTokenResponseError() {
        // TODO:
        // possible error response:
        // {"errors":[{"detail":"Missing or invalid assertion. It must be a JWT signed with the service account key.","source":"assertion","title":"Not authorized"}]}

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
            put("aud", HMKitFleet.Environment.PRODUCTION.url)
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