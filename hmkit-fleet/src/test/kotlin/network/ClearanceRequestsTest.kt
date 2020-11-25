package network

import BaseTest
import com.highmobility.crypto.Crypto
import com.highmobility.crypto.value.Signature
import com.highmobility.hmkit.HMKit
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import model.AuthToken
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
        val webService = ClearanceRequests(client, get(), mockUrl)

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
        val webService = ClearanceRequests(client, get(), mockUrl)

        val status = runBlocking {
            webService.requestClearance(authToken, "WBADT43452G296403")
        }

        assertTrue(status.error!!.title == "Not authorized")
        assertTrue(status.error!!.source == "assertion")
        assertTrue(status.error!!.detail == "Missing or invalid assertion. It must be a JWT signed with the service account key.")

        // TODO: 20/11/20 the error spec will be updated. Update the tests then
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
        val webService = ClearanceRequests(client, get(), mockUrl)

        val status = runBlocking {
            webService.requestClearance(authToken, "WBADT43452G296403")
        }

        val genericError = webService.genericError("")
        assertTrue(status.error!!.title == genericError.title)
    }

    @Test
    fun getClearanceSuccessResponse() {
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(
                "[\n" +
                        "  {\n" +
                        "    \"vin\": \"WBADT43452G296403\",\n" +
                        "    \"status\": \"pending\"\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"vin\": \"WBADT43452G296404\",\n" +
                        "    \"status\": \"pending\"\n" +
                        "  }\n" +
                        "]"
            )
        mockWebServer.enqueue(mockResponse)
        val mockUrl = mockWebServer.url("").toString()
        val webService = ClearanceRequests(client, get(), mockUrl)

        val status = runBlocking {
            webService.getClearanceStatuses(authToken)
        }

        val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
        assertTrue(recordedRequest.path!!.endsWith("/fleets/vehicles"))
        assertTrue(recordedRequest.method == "GET")

        assertTrue(status.response!![0].status == ClearanceStatus.Status.PENDING)
        assertTrue(status.response!![0].vin == "WBADT43452G296403")

        assertTrue(status.response!![1].status == ClearanceStatus.Status.PENDING)
        assertTrue(status.response!![1].vin == "WBADT43452G296404")
    }

    @Test
    fun getClearanceErrorResponse() {
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
        val webService = ClearanceRequests(client, get(), mockUrl)

        val status = runBlocking {
            webService.getClearanceStatuses(authToken)
        }

        assertTrue(status.error!!.title == "Not authorized")
        assertTrue(status.error!!.source == "assertion")
        assertTrue(status.error!!.detail == "Missing or invalid assertion. It must be a JWT signed with the service account key.")

        // TODO: 20/11/20 the error spec will be updated. Update the tests then
    }

    @Test
    fun getClearanceUnknownResponse() {
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .setBody(
                "{\"invalidKey\":\"invalidValue\"}"
            )
        mockWebServer.enqueue(mockResponse)
        val mockUrl = mockWebServer.url("").toString()
        val webService = ClearanceRequests(client, get(), mockUrl)

        val status = runBlocking {
            webService.getClearanceStatuses(authToken)
        }

        val genericError = webService.genericError("")
        assertTrue(status.error!!.title == genericError.title)
    }
}