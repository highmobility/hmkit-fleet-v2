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
package com.highmobility.hmkitfleet.network

import com.highmobility.hmkitfleet.BaseTest
import com.highmobility.hmkitfleet.model.Brand
import com.highmobility.hmkitfleet.model.EligibilityStatus
import com.highmobility.hmkitfleet.notExpiredAuthToken
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection

internal class UtilityRequestsTest : BaseTest() {
    val mockWebServer = MockWebServer()
    val client = OkHttpClient()
    val authToken = notExpiredAuthToken()
    val authTokenRequests = mockk<AuthTokenRequests>()

    @BeforeEach
    fun setUp() {
        mockWebServer.start()
        coEvery { authTokenRequests.getAuthToken() } returns Response(authToken)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // get eligibility

    @Test
    fun getEligibilitySuccess() {
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(
                """
                {
                  "vin": "WBADT43452G296403",
                  "eligible": true,
                  "data_delivery": [
                    "pull",
                    "push"
                  ]
                }
                """.trimIndent()
            )
        mockWebServer.enqueue(mockResponse)
        val mockUrl = mockWebServer.url("").toString()
        val webService = UtilityRequests(client, mockLogger, mockUrl, authTokenRequests)

        val response = runBlocking {
            webService.getEligibility("WBADT43452G296403", Brand.MERCEDES_BENZ)
        }

        coVerify { authTokenRequests.getAuthToken() }

        val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
        assertTrue(recordedRequest.path!!.endsWith("/eligibility"))

        // verify request
        assertTrue(recordedRequest.headers["Authorization"] == "Bearer ${authToken.authToken}")
        val jsonBody = Json.parseToJsonElement(recordedRequest.body.readUtf8())
        assertTrue(jsonBody.jsonObject["vin"]?.jsonPrimitive?.contentOrNull == "WBADT43452G296403")
        assertTrue(jsonBody.jsonObject["brand"]?.jsonPrimitive?.contentOrNull == "mercedes-benz")

        // verify response
        val status = response.response!!
        assertTrue(status.vin == "WBADT43452G296403")
        assertTrue(status.eligible)
        assertTrue(status.dataDelivery.size == 2)
        assertTrue(status.dataDelivery.find { it == EligibilityStatus.DataDelivery.PULL } != null)
        assertTrue(status.dataDelivery.find { it == EligibilityStatus.DataDelivery.PUSH } != null)
        assertTrue(status.connectivityStatus == null)
        assertTrue(status.primaryUserAssigned == null)
    }

    @Test
    fun getEligibilitySuccessOptionalValues() {
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(
                """
                {
                  "vin": "WBADT43452G296403",
                  "eligible": true,
                  "data_delivery": [
                    "pull",
                    "push"
                  ],
                  "connectivity_status": "deactivated",
                  "primary_user_assigned": true
                }
                """.trimIndent()
            )

        mockWebServer.enqueue(mockResponse)
        val mockUrl = mockWebServer.url("").toString()
        val webService = UtilityRequests(client, mockLogger, mockUrl, authTokenRequests)

        val status = runBlocking {
            webService.getEligibility("WBADT43452G296403", Brand.MERCEDES_BENZ)
        }.response!!

        coVerify { authTokenRequests.getAuthToken() }

        val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
        assertTrue(recordedRequest.path!!.endsWith("/eligibility"))

        // verify request
        assertTrue(recordedRequest.headers["Authorization"] == "Bearer ${authToken.authToken}")
        val jsonBody = Json.parseToJsonElement(recordedRequest.body.readUtf8())
        assertTrue(jsonBody.jsonObject["vin"]?.jsonPrimitive?.contentOrNull == "WBADT43452G296403")
        assertTrue(jsonBody.jsonObject["brand"]?.jsonPrimitive?.contentOrNull == "mercedes-benz")

        // verify response
        assertTrue(status.vin == "WBADT43452G296403")
        assertTrue(status.eligible)
        assertTrue(status.dataDelivery.size == 2)
        assertTrue(status.dataDelivery.find { it == EligibilityStatus.DataDelivery.PULL } != null)
        assertTrue(status.dataDelivery.find { it == EligibilityStatus.DataDelivery.PUSH } != null)

        assertTrue(status.connectivityStatus == EligibilityStatus.ConnectivityStatus.DEACTIVATED)
        assertTrue(status.primaryUserAssigned == true)
    }

    @Test
    fun requestClearanceAuthTokenError() = runBlocking {
        testAuthTokenErrorReturned(mockWebServer, authTokenRequests) { mockUrl ->
            val webService = UtilityRequests(client, mockLogger, mockUrl, authTokenRequests)
            webService.getEligibility(
                "WBADT43452G296403",
                Brand.MERCEDES_BENZ,
            )
        }
    }

    @Test
    fun requestClearanceErrorResponse() = runBlocking {
        testErrorResponseReturned(mockWebServer) { mockUrl ->
            val webService = UtilityRequests(client, mockLogger, mockUrl, authTokenRequests)
            webService.getEligibility(
                "WBADT43452G296403",
                Brand.MERCEDES_BENZ,
            )
        }
    }

    @Test
    fun requestClearanceUnknownResponse() = runBlocking {
        testForUnknownResponseGenericErrorReturned(mockWebServer) { mockUrl ->
            val webService = UtilityRequests(client, mockLogger, mockUrl, authTokenRequests)
            webService.getEligibility(
                "WBADT43452G296403",
                Brand.MERCEDES_BENZ,
            )
        }
    }
}
