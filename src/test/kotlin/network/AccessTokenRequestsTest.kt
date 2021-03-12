/*
 * The MIT License
 *
 * Copyright (c) 2014- High-Mobility GmbH (https://high-mobility.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
import newAccessToken
import notExpiredAuthToken
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
            AccessTokenRequests(client, mockLogger, baseUrl.toString(), authTokenRequests, configuration)

        val response = runBlocking {
            accessTokenRequests.getAccessToken(
                "WBADT43452G296403"
            )
        }

        coVerify { authTokenRequests.getAuthToken() }

        val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
        assertTrue(recordedRequest.path!!.endsWith("/access_tokens"))

        // verify request
        assertTrue(recordedRequest.headers["Authorization"] == "Bearer ${notExpiredAuthToken().authToken}")
        val jsonBody = Json.parseToJsonElement(recordedRequest.body.readUtf8()) as JsonObject
        assertTrue(jsonBody["vin"]!!.jsonPrimitive.contentOrNull == "WBADT43452G296403")
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
            val accessTokenRequests = AccessTokenRequests(client, mockLogger, it, authTokenRequests, configuration)
            accessTokenRequests.getAccessToken("WBADT43452G296403")
        }
    }

    @Test
    fun getAccessTokenErrorResponse() = runBlocking {
        testErrorResponseReturned(mockWebServer) { mockUrl ->
            val webService = AccessTokenRequests(client, mockLogger, mockUrl, authTokenRequests, configuration)
            webService.getAccessToken(
                "WBADT43452G296403"
            )
        }
    }

    @Test
    fun getAccessTokenUnknownResponse() = runBlocking {
        testForUnknownResponseGenericErrorReturned(mockWebServer) { mockUrl ->
            val webService = AccessTokenRequests(client, mockLogger, mockUrl, authTokenRequests, configuration)
            webService.getAccessToken(
                "WBADT43452G296403"
            )
        }
    }

    @Test
    fun deleteAccessToken() {
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)

        mockWebServer.enqueue(mockResponse)
        val mockUrl = mockWebServer.url("").toString()
        val accessTokenRequests = AccessTokenRequests(client, mockLogger, mockUrl, mockk(), configuration)

        runBlocking {
            accessTokenRequests.deleteAccessToken(newAccessToken)
        }

        // verify request
        val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
        assertTrue(recordedRequest.path!!.endsWith("/access_tokens"))
        assertTrue(recordedRequest.method == "DELETE")

        val jsonBody = Json.parseToJsonElement(recordedRequest.body.readUtf8()) as JsonObject
        assertTrue(jsonBody["token"]?.jsonPrimitive?.contentOrNull == newAccessToken.refreshToken)
        assertTrue(jsonBody["client_id"]?.jsonPrimitive?.contentOrNull == configuration.oauthClientId)
        assertTrue(jsonBody["client_secret"]?.jsonPrimitive?.contentOrNull == configuration.oauthClientSecret)
        assertTrue(jsonBody["token_type_hint"]?.jsonPrimitive?.contentOrNull == "refresh_token")

        // the response is empty 200, nothing to verify
    }

    // error responses

    @Test
    fun deleteAccessTokenErrorResponse() = runBlocking {
        testErrorResponseReturned(mockWebServer) { mockUrl ->
            val webService = AccessTokenRequests(client, mockLogger, mockUrl, authTokenRequests, configuration)
            webService.deleteAccessToken(newAccessToken)
        }
    }

    @Test
    fun deleteAccessTokenUnknownResponse() = runBlocking {
        testForUnknownResponseGenericErrorReturned(mockWebServer) { mockUrl ->
            val webService = AccessTokenRequests(client, mockLogger, mockUrl, authTokenRequests, configuration)
            webService.deleteAccessToken(newAccessToken)
        }
    }
}
