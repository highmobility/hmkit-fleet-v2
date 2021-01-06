package network

import BaseTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import model.Brand
import model.ClearanceStatus
import model.ControlMeasure
import model.Odometer
import notExpiredAuthToken
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection

internal class ClearanceRequestsTest : BaseTest() {
    val mockWebServer = MockWebServer()
    val client = OkHttpClient()
    val authToken = notExpiredAuthToken()
    val authTokenRequests = mockk<AuthTokenRequests>()

    val controlMeasures = listOf<ControlMeasure>(
        Odometer(
            100000L,
            Odometer.Length.KILOMETERS
        )
    )

    @BeforeEach
    fun setUp() {
        mockWebServer.start()
        coEvery { authTokenRequests.getAuthToken() } returns Response(authToken)
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
        val webService = ClearanceRequests(client, mockLogger, mockUrl, authTokenRequests)

        val status = runBlocking {
            webService.requestClearance("WBADT43452G296403", Brand.DAIMLER_FLEET, controlMeasures)
        }

        coVerify { authTokenRequests.getAuthToken() }

        val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
        assertTrue(recordedRequest.path!!.endsWith("/fleets/vehicles"))

        // verify request
        assertTrue(recordedRequest.headers["Authorization"] == "Bearer ${authToken.authToken}")

        val jsonBody = Json.parseToJsonElement(recordedRequest.body.readUtf8())
        val array = jsonBody.jsonObject["vehicles"] as JsonArray
        val firstVehicle = array.first() as JsonObject
        assertTrue(firstVehicle?.get("vin")?.jsonPrimitive?.contentOrNull == "WBADT43452G296403")
        assertTrue(firstVehicle?.get("brand")?.jsonPrimitive?.contentOrNull == "daimler-fleet")
        val controlMeasures = firstVehicle?.get("control_measures")?.jsonObject
        val odometer = controlMeasures?.get("odometer")?.jsonObject
        assertTrue(odometer?.get("value")?.jsonPrimitive?.contentOrNull == "100000")
        assertTrue(odometer?.get("unit")?.jsonPrimitive?.contentOrNull == "kilometers")

        // verify response
        assertTrue(status.response!!.status == ClearanceStatus.Status.PENDING)
        assertTrue(status.response!!.vin == "WBADT43452G296403")
    }

    @Test
    fun requestClearanceAuthTokenError() = runBlocking {
        testAuthTokenErrorReturned(mockWebServer, authTokenRequests) { mockUrl ->
            val webService = ClearanceRequests(client, mockLogger, mockUrl, authTokenRequests)
            webService.requestClearance(
                "WBADT43452G296403",
                Brand.DAIMLER_FLEET,
                controlMeasures
            )
        }
    }

    @Test
    fun requestClearanceErrorResponse() = runBlocking {
        testErrorResponseReturned(mockWebServer) { mockUrl ->
            val webService = ClearanceRequests(client, mockLogger, mockUrl, authTokenRequests)
            webService.requestClearance(
                "WBADT43452G296403",
                Brand.DAIMLER_FLEET,
                controlMeasures
            )
        }
    }

    @Test
    fun requestClearanceUnknownResponse() = runBlocking {
        testForUnknownResponseGenericErrorReturned(mockWebServer) { mockUrl ->
            val webService = ClearanceRequests(client, mockLogger, mockUrl, authTokenRequests)
            webService.requestClearance(
                "WBADT43452G296403",
                Brand.DAIMLER_FLEET,
                controlMeasures
            )
        }
    }

    @Test
    fun getClearanceStatusesSuccessResponse() {
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
        val webService = ClearanceRequests(client, mockLogger, mockUrl, authTokenRequests)

        val status = runBlocking {
            webService.getClearanceStatuses()
        }

        coVerify { authTokenRequests.getAuthToken() }

        val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
        assertTrue(recordedRequest.headers["Authorization"] == "Bearer ${authToken.authToken}")

        assertTrue(recordedRequest.path!!.endsWith("/fleets/vehicles"))
        assertTrue(recordedRequest.method == "GET")

        assertTrue(status.response!![0].status == ClearanceStatus.Status.PENDING)
        assertTrue(status.response!![0].vin == "WBADT43452G296403")

        assertTrue(status.response!![1].status == ClearanceStatus.Status.PENDING)
        assertTrue(status.response!![1].vin == "WBADT43452G296404")
    }

    @Test
    fun getClearanceStatusesAuthTokenError() = runBlocking {
        testAuthTokenErrorReturned(mockWebServer, authTokenRequests) { mockUrl ->
            val webService = ClearanceRequests(client, mockLogger, mockUrl, authTokenRequests)
            webService.getClearanceStatuses()
        }
    }

    @Test
    fun getClearanceErrorResponse() = runBlocking {
        testErrorResponseReturned(mockWebServer) { mockUrl ->
            val webService = ClearanceRequests(client, mockLogger, mockUrl, authTokenRequests)
            webService.getClearanceStatuses()
        }
    }

    @Test
    fun getClearanceUnknownResponse() = runBlocking {
        testForUnknownResponseGenericErrorReturned(mockWebServer) { mockUrl ->
            val webService = ClearanceRequests(client, mockLogger, mockUrl, authTokenRequests)
            webService.getClearanceStatuses()
        }
    }
}
