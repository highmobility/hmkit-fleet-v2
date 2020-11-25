package network

import BaseTest
import HMKitFleet
import com.highmobility.crypto.Crypto
import com.highmobility.crypto.value.Signature
import com.highmobility.hmkit.HMKit
import com.highmobility.utils.Base64
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.component.get
import org.koin.core.component.inject
import java.net.HttpURLConnection

internal class AuthTokenRequestsTest : BaseTest() {
    val mockWebServer = MockWebServer()

    val client = OkHttpClient()

    val hmkitOem by inject<HMKit>()
    val privateKey = configuration.getHmPrivateKey()
    val signature = Signature(privateKey.concat(privateKey))
    val crypto = mockk<Crypto> {
        every { signJWT(any<ByteArray>(), any()) } returns signature
    }

    @BeforeEach
    fun setUp() {
        every { hmkitOem.crypto } returns crypto
        every { crypto.signJWT(any<ByteArray>(), any()) } returns signature

        mockWebServer.start()
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun authTokenSuccessResponse() {
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_CREATED)
            .setBody(
                "{\"auth_token\":\"e903cb43-27b1-4e47-8922-c04ecd5d2019\"," +
                        "\"valid_from\":\"2020-11-17T04:50:16\"," +
                        "\"valid_until\":\"2020-11-17T05:50:16\"}"
            )
        mockWebServer.enqueue(mockResponse)
        val baseUrl: HttpUrl = mockWebServer.url("")
        val webService = AuthTokenRequests(client, hmkitOem, get(), baseUrl.toString())
        val jwtContent = getJwtContent()

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
    fun authTokenErrorResponse() {
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .setBody(
                "{\"errors\":" +
                        "[{\"detail\":\"Missing or invalid assertion. It must be a JWT signed with the service account key.\"," +
                        "\"source\":\"assertion\"," +
                        "\"title\":\"Not authorized\"}]}"
            )
        mockWebServer.enqueue(mockResponse)
        val baseUrl: HttpUrl = mockWebServer.url("")
        val webService = AuthTokenRequests(client, hmkitOem, get(), baseUrl.toString())

        val status = runBlocking {
            webService.getAuthToken(configuration)
        }

        assertTrue(status.error!!.title == "Not authorized")
        assertTrue(status.error!!.source == "assertion")
        assertTrue(status.error!!.detail == "Missing or invalid assertion. It must be a JWT signed with the service account key.")
    }

    @Test
    fun authTokenInvalidResponse() {
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .setBody(
                "{\"invalidKey\":\"invalidValue\"}"
            )
        mockWebServer.enqueue(mockResponse)
        val baseUrl: HttpUrl = mockWebServer.url("")
        val webService = AuthTokenRequests(client, hmkitOem, get(), baseUrl.toString())

        val status = runBlocking {
            webService.getAuthToken(configuration)
        }

        val genericError = webService.genericError("")
        assertTrue(status.error!!.title == genericError.title)
    }

    @Test
    fun requestClearanceSuccessResponse() {

    }

    @Test
    fun requestClearanceErrorResponse() {

    }

    @Test
    fun requestClearanceUnknownResponse() {

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
}