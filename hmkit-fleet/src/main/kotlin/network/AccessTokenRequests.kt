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

import com.highmobility.hmkitfleet.ServiceAccountApiConfiguration
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import com.highmobility.hmkitfleet.model.AccessToken
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.Logger
import utils.await
import java.net.HttpURLConnection

internal class AccessTokenRequests(
    client: OkHttpClient,
    logger: Logger,
    baseUrl: String,
    private val authTokenRequests: AuthTokenRequests,
    private val configuration: ServiceAccountApiConfiguration,
) : Requests(
    client,
    logger, baseUrl
) {
    suspend fun getAccessToken(
        vin: String
    ): Response<AccessToken> {
        val authToken = authTokenRequests.getAuthToken()

        if (authToken.response == null) return Response(null, authToken.error)

        val request = Request.Builder()
            .url("${baseUrl}/fleets/access_tokens")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${authToken.response.authToken}")
            .post(getTokenBody(vin))
            .build()

        printRequest(request)

        val call = client.newCall(request)
        val response = call.await()

        return tryParseResponse(response, HttpURLConnection.HTTP_OK) { body ->
            val format = Json { ignoreUnknownKeys = true }
            val accessToken = format.decodeFromString<AccessToken>(body)
            Response(accessToken, null)
        }
    }

    suspend fun deleteAccessToken(accessToken: AccessToken): Response<Boolean> {
        val request = Request.Builder()
            .url("${baseUrl}/access_tokens")
            .header("Content-Type", "application/json")
            .method("DELETE", deleteTokenBody(accessToken))
            .build()

        printRequest(request)

        val call = client.newCall(request)
        val response = call.await()

        return tryParseResponse(response, HttpURLConnection.HTTP_OK) {
            Response(true, null)
        }
    }

    private fun deleteTokenBody(accessToken: AccessToken): RequestBody {
        val completeBody = buildJsonObject {
            put("token", accessToken.refreshToken)
            put("client_id", configuration.oauthClientId)
            put("client_secret", configuration.oauthClientSecret)
            put("token_type_hint", "refresh_token")
        }

        val body = completeBody.toString().toRequestBody(mediaType)
        return body
    }

    private fun getTokenBody(vin: String): RequestBody {
        val completeBody = buildJsonObject {
            put("vin", vin)
        }

        val body = completeBody.toString().toRequestBody(mediaType)
        return body
    }
}