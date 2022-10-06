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

import com.highmobility.crypto.Crypto
import com.highmobility.hmkitfleet.BaseTest
import com.highmobility.hmkitfleet.HMKitFleet
import com.highmobility.hmkitfleet.mockSignature
import com.highmobility.hmkitfleet.model.AuthToken
import com.highmobility.hmkitfleet.notExpiredAuthToken
import com.highmobility.utils.Base64
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
import java.net.HttpURLConnection

internal class AuthTokenRequestsTest : BaseTest() {
    private val mockWebServer = MockWebServer()
    private val client = OkHttpClient()

    private lateinit var crypto: Crypto
    private val cache = mockk<Cache>()

    private val privateKey = configuration.getServiceAccountHmPrivateKey()

    @BeforeEach
    fun setUp() {
        every { configuration.createJti() } returns "jti"
        every { configuration.createIat() } returns 1001
        every { cache setProperty "authToken" value any<AuthToken>() } just Runs

        crypto = mockk()
        every { crypto.signJWT(any<ByteArray>(), any()) } returns mockSignature

        mockWebServer.start()
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun downloadsAuthTokenAndWritesToCacheIfDoesNotExistOrExpired() {
        val responseAuthToken = notExpiredAuthToken()
        // return null from cache at first, then on next call a new one
        // if auth token is expired, the cache does not return it also
        every { cache getProperty "authToken" } returnsMany listOf(null, responseAuthToken)

        val response = runBlocking {
            mockSuccessfulRequest(responseAuthToken).getAuthToken()
        }

        verifyAuthTokenRequestedFromServer()

        verifyNewAuthTokenReturned(responseAuthToken, response)
        verify { cache setProperty "authToken" value responseAuthToken }
    }

    private fun verifyNewAuthTokenReturned(expected: AuthToken, response: Response<AuthToken>) {
        assertTrue(response.response!!.authToken == expected.authToken)
        assertTrue(response.response!!.validFrom.toString() == expected.validFrom.toString())
        assertTrue(response.response!!.validUntil.toString() == expected.validUntil.toString())
    }

    private fun verifyAuthTokenRequestedFromServer() {
        val jwtContent = getJwtContent().toByteArray()
        coVerify { crypto.signJWT(jwtContent, privateKey) }

        val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
        assertTrue(recordedRequest.path!!.endsWith("/auth_tokens"))
    }

    @Test
    fun doesNotDownloadAuthTokenIfExistsAndNotExpired() {
        val responseAuthToken = notExpiredAuthToken()
        // return null from cache at first, then on next call a new one
        every { cache getProperty "authToken" } returns responseAuthToken
        val response = runBlocking { mockSuccessfulRequest(responseAuthToken).getAuthToken() }

        // this means request is not made
        verify(exactly = 0) { crypto.signJWT(any<ByteArray>(), any()) }
        verify(exactly = 0) { cache setProperty "authToken" value any<AuthToken>() }

        verifyNewAuthTokenReturned(responseAuthToken, response)
    }

    @Test
    fun authTokenErrorResponse() {
        every { cache getProperty "authToken" } returns null

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
        val webService =
            AuthTokenRequests(
                client,
                crypto,
                mockLogger,
                baseUrl.toString(),
                configuration,
                cache
            )

        val status = runBlocking {
            webService.getAuthToken()
        }

        assertTrue(status.error!!.title == "Not authorized")
        assertTrue(status.error!!.source == "assertion")
        assertTrue(status.error!!.detail == "Missing or invalid assertion. It must be a JWT signed with the service account key.")
    }

    @Test
    fun authTokenInvalidResponse() {
        every { cache getProperty "authToken" } returns null

        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .setBody(
                "{\"invalidKey\":\"invalidValue\"}"
            )
        mockWebServer.enqueue(mockResponse)
        val baseUrl: HttpUrl = mockWebServer.url("")
        val webService =
            AuthTokenRequests(
                client,
                crypto,
                mockLogger,
                baseUrl.toString(),
                configuration,
                cache
            )

        val status = runBlocking {
            webService.getAuthToken()
        }

        val genericError = genericError("")
        assertTrue(status.error!!.title == genericError.title)
    }

    private fun mockSuccessfulRequest(responseAuthToken: AuthToken): AuthTokenRequests {
        // return null from cache at first, then on next call a new one
        val json = Json.encodeToString(responseAuthToken)

        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_CREATED)
            .setBody(json)

        mockWebServer.enqueue(mockResponse)
        val baseUrl: HttpUrl = mockWebServer.url("")

        return AuthTokenRequests(
            client,
            crypto,
            mockLogger,
            baseUrl.toString(),
            configuration,
            cache
        )
    }

    fun getJwtContent(): String {
        every { configuration.createJti() } returns "jti"
        every { configuration.createIat() } returns 1001

        val header = buildJsonObject {
            put("alg", "ES256")
            put("typ", "JWT")
        }.toString()

        val jwtBody = buildJsonObject {
            put("ver", configuration.version)
            put("iss", configuration.serviceAccountApiKey)
            put("aud", HMKitFleet.Environment.jwtUrl)
            put("jti", configuration.createJti())
            put("iat", configuration.createIat())
        }.toString()

        val headerBase64 = Base64.encodeUrlSafe(header.toByteArray())
        val bodyBase64 = Base64.encodeUrlSafe(jwtBody.toByteArray())

        return String.format("%s.%s", headerBase64, bodyBase64)
    }
}