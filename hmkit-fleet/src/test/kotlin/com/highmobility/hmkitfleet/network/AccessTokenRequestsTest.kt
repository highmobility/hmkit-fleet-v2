/*
 * The MIT License
 *
 * Copyright (c) 2023- High-Mobility GmbH (https://high-mobility.com)
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
@file:Suppress("MaxLineLength")

package com.highmobility.hmkitfleet.network

import com.highmobility.crypto.Crypto
import com.highmobility.hmkitfleet.BaseTest
import com.highmobility.hmkitfleet.model.AccessToken
import com.highmobility.hmkitfleet.notExpiredAccessToken
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
  private val mockWebServer = MockWebServer()
  private val client = OkHttpClient()

  private lateinit var crypto: Crypto
  private val cache = mockk<Cache>()

  @BeforeEach
  fun setUp() {
    every { cache setProperty "accessToken" value any<AccessToken>() } just Runs

    crypto = mockk()

    mockWebServer.start()
  }

  @AfterEach
  fun tearDown() {
    mockWebServer.shutdown()
  }

  @Test
  fun downloadsAccessTokenAndWritesToCacheIfDoesNotExistOrExpired() {
    val responseAccessToken = notExpiredAccessToken()
    // return null from cache at first, then on next call a new one
    // if auth token is expired, the cache does not return it also
    every { cache getProperty "accessToken" } returnsMany listOf(null, responseAccessToken)

    val response = runBlocking {
      mockSuccessfulRequest(responseAccessToken).getAccessToken()
    }

    val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
    assertTrue(recordedRequest.path!!.endsWith("/access_tokens"))

    verifyNewAccessTokenReturned(responseAccessToken, response)
    verify { cache setProperty "accessToken" value responseAccessToken }
  }

  private fun verifyNewAccessTokenReturned(expected: AccessToken, response: Response<AccessToken>) {
    assertTrue(response.response!!.accessToken == expected.accessToken)
    assertTrue(response.response!!.expiresIn == expected.expiresIn)
  }

  @Test
  fun doesNotDownloadAccessTokenIfExistsAndNotExpired() {
    val responseAccessToken = notExpiredAccessToken()
    // return null from cache at first, then on next call a new one
    every { cache getProperty "accessToken" } returns responseAccessToken
    val response = runBlocking { mockSuccessfulRequest(responseAccessToken).getAccessToken() }

    // this means request is not made
    verify(exactly = 0) { crypto.signJWT(any<ByteArray>(), any()) }
    verify(exactly = 0) { cache setProperty "accessToken" value any<AccessToken>() }

    verifyNewAccessTokenReturned(responseAccessToken, response)
  }

  @Test
  fun authTokenErrorResponse() {
    every { cache getProperty "accessToken" } returns null

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
      AccessTokenRequests(
        client,
        crypto,
        mockLogger,
        baseUrl.toString(),
        privateKeyConfiguration.credentials,
        cache
      )

    val status = runBlocking {
      webService.getAccessToken()
    }

    assertTrue(status.error!!.title == "Not authorized")
    assertTrue(status.error!!.source == "assertion")
    assertTrue(
      status.error!!.detail == "Missing or invalid assertion. It must be a JWT signed with the service account key."
    )
  }

  @Test
  fun authTokenInvalidResponse() {
    every { cache getProperty "accessToken" } returns null

    val mockResponse = MockResponse()
      .setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
      .setBody(
        "{\"invalidKey\":\"invalidValue\"}"
      )
    mockWebServer.enqueue(mockResponse)
    val baseUrl: HttpUrl = mockWebServer.url("")
    val webService =
      AccessTokenRequests(
        client,
        crypto,
        mockLogger,
        baseUrl.toString(),
        privateKeyConfiguration.credentials,
        cache
      )

    val status = runBlocking {
      webService.getAccessToken()
    }

    val genericError = genericError("")
    assertTrue(status.error!!.title == genericError.title)
  }

  private fun mockSuccessfulRequest(responseAccessToken: AccessToken): AccessTokenRequests {
    // return null from cache at first, then on next call a new one
    val json = Json.encodeToString(responseAccessToken)

    val mockResponse = MockResponse()
      .setResponseCode(HttpURLConnection.HTTP_CREATED)
      .setBody(json)

    mockWebServer.enqueue(mockResponse)
    val baseUrl: HttpUrl = mockWebServer.url("")

    return AccessTokenRequests(
      client,
      crypto,
      mockLogger,
      baseUrl.toString(),
      privateKeyConfiguration.credentials,
      cache
    )
  }
}
