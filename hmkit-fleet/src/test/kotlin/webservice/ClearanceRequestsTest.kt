package webservice

import BaseTest
import com.highmobility.crypto.Crypto
import com.highmobility.crypto.value.Signature
import com.highmobility.hmkit.HMKit
import configuration
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import model.AuthToken
import network.AuthTokenRequests
import network.ClearanceRequests
import network.response.ClearanceStatus
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
import java.time.LocalDateTime

internal class ClearanceRequestsTest : BaseTest() {
    val mockWebServer = MockWebServer()

    val client = OkHttpClient()

    val hmkitOem by inject<HMKit>()
    val privateKey = configuration.getHmPrivateKey()
    val signature = Signature(privateKey.concat(privateKey))
    val crypto = mockk<Crypto> {
        every { signJWT(any<ByteArray>(), any()) } returns signature
    }

    val authToken = AuthToken("token", LocalDateTime.now(), LocalDateTime.now())

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
    fun requestClearanceSuccessResponse() {
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(
                "{\n" +
                        "  \"vins\": [\n" +
                        "    {\n" +
                        "      \"vin\": \"WBADT43452G296403\",\n" +
                        "      \"status\": \"pending\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}"
            )
        mockWebServer.enqueue(mockResponse)
        val mockUrl = mockWebServer.url("").toString()
        val webService = ClearanceRequests(client, hmkitOem, get(), mockUrl)

        val status = runBlocking {
            webService.requestClearance(authToken, "WBADT43452G296403")
        }

        val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
        assertTrue(recordedRequest.path!!.endsWith("/fleets/vehicles"))

        // verify request
        val jsonBody = Json.parseToJsonElement(recordedRequest.body.readUtf8())
        val array = jsonBody.jsonObject["vehicles"] as JsonArray
        val firstVehicle = array.first() as JsonObject
        assertTrue(firstVehicle["vin"]!!.jsonPrimitive?.contentOrNull == "WBADT43452G296403")

        // verify response
        assertTrue(status.response!!.status == ClearanceStatus.Status.PENDING)
        assertTrue(status.response!!.vin == "WBADT43452G296403")
    }

    @Test
    fun requestClearanceErrorResponse() {
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .setBody(
                "{\"errors\":" +
                        "[{\"detail\":\"Missing or invalid assertion. It must be a JWT signed with the service account key.\"," +
                        "\"source\":\"assertion\"," +
                        "\"title\":\"Not authorized\"}]}"
            )
        mockWebServer.enqueue(mockResponse)
        val mockUrl = mockWebServer.url("").toString()
        val webService = AuthTokenRequests(client, hmkitOem, get(), mockUrl)

        val status = runBlocking {
            webService.getAuthToken(configuration)
        }

        assertTrue(status.error!!.title == "Not authorized")
        assertTrue(status.error!!.source == "assertion")
        assertTrue(status.error!!.detail == "Missing or invalid assertion. It must be a JWT signed with the service account key.")
    }

    @Test
    fun requestClearanceUnknownResponse() {
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .setBody(
                "{\"invalidKey\":\"invalidValue\"}"
            )
        mockWebServer.enqueue(mockResponse)
        val mockUrl = mockWebServer.url("").toString()
        val webService = AuthTokenRequests(client, hmkitOem, get(), mockUrl)

        val status = runBlocking {
            webService.getAuthToken(configuration)
        }

        val genericError = webService.genericError("")
        assertTrue(status.error!!.title == genericError.title)
    }
}