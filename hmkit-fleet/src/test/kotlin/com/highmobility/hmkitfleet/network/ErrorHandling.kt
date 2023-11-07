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
package com.highmobility.hmkitfleet.network

import io.mockk.coEvery
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertTrue
import java.net.HttpURLConnection

/**
 * Respond with an error for this [request] and assert the fields are parsed into the response
 */
internal inline fun <T> testErrorResponseReturned(
  mockWebServer: MockWebServer,
  request: (mockUrl: String) -> Response<T>
) {
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

  val response = request(mockUrl)

  assertTrue(response.error!!.title == "Not authorized")
  assertTrue(response.error!!.source == "assertion")
  assertTrue(
    response.error!!.detail == "Missing or invalid assertion. It must be a JWT signed with the service account key."
  )
}

/**
 * Respond with unknown response and assert generic error is returned
 */
internal inline fun <T> testForUnknownResponseGenericErrorReturned(
  mockWebServer: MockWebServer,
  request: (mockUrl: String) -> Response<T>
) {
  val mockResponse = MockResponse()
    .setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
    .setBody(
      "{\"invalidKey\":\"invalidValue\"}"
    )
  mockWebServer.enqueue(mockResponse)
  val mockUrl = mockWebServer.url("").toString()

  val response = request(mockUrl)

  val genericError = genericError("")
  assertTrue(response.error!!.title == genericError.title)
}

/**
 * Auth token request returns error
 */
internal inline fun <T> testAuthTokenErrorReturned(
  mockWebServer: MockWebServer,
  accessTokenRequests: AccessTokenRequests,
  request: (mockUrl: String) -> Response<T>
) {
  coEvery { accessTokenRequests.getAccessToken() } returns Response(
    null,
    genericError("auth token error")
  )

  val mockUrl = mockWebServer.url("").toString()

  val response = request(mockUrl)

  assertTrue(response.response == null)
  assertTrue(response.error!!.detail == "auth token error")
}
