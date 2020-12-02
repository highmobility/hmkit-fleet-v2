package network

import BaseTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import model.Brand
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection

internal class AccessTokenRequestsTest : BaseTest() {
    val mockWebServer = MockWebServer()
    val client = OkHttpClient()
    val authTokenRequests = mockk<AuthTokenRequests>()

    @BeforeEach
    fun setUp() {
        coEvery { authTokenRequests.getAuthToken() } returns Response(notExpiredAuthToken())
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
        val accessTokenRequests =
            AccessTokenRequests(client, mockLogger, baseUrl.toString(), authTokenRequests)

        val response = runBlocking {
            accessTokenRequests.createAccessToken(
                "WBADT43452G296403",
                Brand.MERCEDES_BENZ
            )
        }

        coVerify { authTokenRequests.getAuthToken() }

        val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
        assertTrue(recordedRequest.path!!.endsWith("/access_tokens"))

        // verify request
        val jsonBody = Json.parseToJsonElement(recordedRequest.body.readUtf8()) as JsonObject
        assertTrue(jsonBody["vin"]!!.jsonPrimitive?.contentOrNull == "WBADT43452G296403")
        assertTrue(jsonBody["brand"]!!.jsonPrimitive?.contentOrNull == "mercedes-benz")
        assertTrue(recordedRequest.headers["Authorization"] == "Bearer ${notExpiredAuthToken().authToken}")

        // verify response
        assertTrue(response.response?.expiresIn == 600)
        assertTrue(response.response?.refreshToken == "7f7a9be0-04c9-4202-a59f-35d55079b6ba")
        assertTrue(response.response?.accessToken == "a50e89e5-093c-4727-8101-4c6e81addabe")
        assertTrue(response.response?.scope == "diagnostics.mileage door_locks.locks windows.windows_positions")
    }

    // error responses

    @Test
    fun getAuthTokenErrorReturned() = runBlocking {
        testAuthTokenErrorReturned(mockWebServer, authTokenRequests) {
            val accessTokenRequests = AccessTokenRequests(client, mockLogger, it, authTokenRequests)
            accessTokenRequests.createAccessToken("WBADT43452G296403", Brand.MERCEDES_BENZ)
        }
    }
    
    @Test
    fun getClearanceErrorResponse() = runBlocking {
        testErrorResponseReturned(mockWebServer) { mockUrl ->
            val webService = AccessTokenRequests(client, mockLogger, mockUrl, authTokenRequests)
            webService.createAccessToken(
                "WBADT43452G296403",
                Brand.MERCEDES_BENZ
            )
        }
    }

    @Test
    fun getClearanceUnknownResponse() = runBlocking {
        testForUnknownResponseGenericErrorReturned(mockWebServer) { mockUrl ->
            val webService = AccessTokenRequests(client, mockLogger, mockUrl, authTokenRequests)
            webService.createAccessToken(
                "WBADT43452G296403",
                Brand.MERCEDES_BENZ
            )
        }
    }
}
