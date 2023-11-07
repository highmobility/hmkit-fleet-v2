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

import com.highmobility.crypto.Crypto
import com.highmobility.hmkitfleet.HMKitCredentials
import com.highmobility.hmkitfleet.model.AccessToken
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.Logger
import utils.await
import java.net.HttpURLConnection

private val JSON: MediaType = "application/json; charset=utf-8".toMediaTypeOrNull()!!

internal class AccessTokenRequests(
  client: OkHttpClient,
  private val crypto: Crypto,
  logger: Logger,
  baseUrl: String,
  private val credentials: HMKitCredentials,
  private val cache: Cache,
) : Requests(
  client,
  logger,
  baseUrl,
) {
  private fun getUrlEncodedJsonRequest(): Request {
    val json = credentials.getTokenRequestBody(crypto, baseUrl)
    val requestBody = json.toRequestBody(JSON)

    val request = Request.Builder()
      .url("$baseUrl/access_tokens")
      .header("Content-Type", "application/json")
      .post(requestBody)
      .build()

    return request
  }

  private val json = Json { ignoreUnknownKeys = true }

  suspend fun getAccessToken(): Response<AccessToken> {
    val cachedToken = cache.accessToken
    if (cachedToken != null) return Response(cachedToken)

    val request = getUrlEncodedJsonRequest()

    printRequest(request)
    val call = client.newCall(request)

    val response = call.await()
    val responseBody = printResponse(response)

    return try {
      if (response.code == HttpURLConnection.HTTP_CREATED || response.code == HttpURLConnection.HTTP_OK) {
        cache.accessToken = json.decodeFromString(responseBody)
        Response(cache.accessToken)
      } else {
        parseError(responseBody)
      }
    } catch (e: java.lang.Exception) {
      val detail = e.message.toString()
      Response(null, genericError(detail))
    }
  }
}
