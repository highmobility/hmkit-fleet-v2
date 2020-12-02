package network

import BaseTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
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
import java.net.HttpURLConnection

internal class AccessTokenRequestsTest : BaseTest() {
    val mockWebServer = MockWebServer()
    val client = OkHttpClient()

    @BeforeEach
    fun setUp() {
        mockWebServer.start()
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun createAccessToken() {
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(
                "{\n" +
                        "  \"token_type\": \"bearer\",\n" +
                        "  \"scope\": \"diagnostics.mileage door_locks.locks windows.windows_positions\",\n" +
                        "  \"refresh_token\": \"7f7a9be0-04c9-4202-a59f-35d55079b6ba\",\n" +
                        "  \"expires_in\": 600,\n" +
                        "  \"access_token\": \"a50e89e5-093c-4727-8101-4c6e81addabe\"\n" +
                        "}"
            )
        mockWebServer.enqueue(mockResponse)
        val baseUrl: HttpUrl = mockWebServer.url("")
        val webService = AccessTokenRequests(client, get(), baseUrl.toString())

        val status = runBlocking {
            webService.createAccessToken(
                mockk { every { authToken } returns "token1" },
                "WBADT43452G296403",
                "mercedes-benz"
            )
        }

        val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
        assertTrue(recordedRequest.path!!.endsWith("/access_tokens"))

        // verify request
        val jsonBody = Json.parseToJsonElement(recordedRequest.body.readUtf8()) as JsonObject
        assertTrue(jsonBody["vin"]!!.jsonPrimitive?.contentOrNull == "WBADT43452G296403")
        assertTrue(jsonBody["oem"]!!.jsonPrimitive?.contentOrNull == "mercedes-benz")

        assertTrue(recordedRequest.headers["Authorization"] == "Bearer token1")

        assertTrue(status.response?.expiresIn == 600)
        assertTrue(status.response?.refreshToken == "7f7a9be0-04c9-4202-a59f-35d55079b6ba")
        assertTrue(status.response?.accessToken == "a50e89e5-093c-4727-8101-4c6e81addabe")
        assertTrue(status.response?.scope == "diagnostics.mileage door_locks.locks windows.windows_positions")
    }

    @Test
    fun getAuthTokenErrorReturned() {
        TODO()
    }

    // error responses

    @Test
    fun getClearanceErrorResponse() {
        runBlocking {
            testErrorResponseHandled(mockWebServer) { mockUrl ->
                val webService = AccessTokenRequests(client, get(), mockUrl)
                webService.createAccessToken(
                    mockk { every { authToken } returns "token1" },
                    "WBADT43452G296403",
                    "mercedes-benz"
                )
            }
        }
    }

    @Test
    fun getClearanceUnknownResponse() {
        runBlocking {
            testUnknownResponseHandled(mockWebServer) { mockUrl ->
                val webService = AccessTokenRequests(client, get(), mockUrl)
                webService.createAccessToken(
                    mockk { every { authToken } returns "token1" },
                    "WBADT43452G296403",
                    "mercedes-benz"
                )
            }
        }
    }
}