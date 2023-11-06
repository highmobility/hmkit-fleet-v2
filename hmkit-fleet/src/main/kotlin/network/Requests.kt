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
import com.highmobility.hmkitfleet.ServiceAccountApiConfiguration
import com.highmobility.utils.Base64
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import org.slf4j.Logger

internal open class Requests(
  val client: OkHttpClient,
  val logger: Logger,
  val baseUrl: String,
) {
  val mediaType = "application/json; charset=utf-8".toMediaType()
  val baseHeaders = Headers.Builder().add("Content-Type", "application/json").build()

  inline fun <T> tryParseResponse(
    response: Response,
    expectedResponseCode: Int,
    block: (body: String) -> (com.highmobility.hmkitfleet.network.Response<T>)
  ): com.highmobility.hmkitfleet.network.Response<T> {
    val responseBody = printResponse(response)

    return try {
      if (response.code == expectedResponseCode) {
        block(responseBody)
      } else {
        parseError(responseBody)
      }
    } catch (e: Exception) {
      val detail = e.message.toString()
      Response(null, genericError(detail))
    }
  }

  fun printRequest(request: Request) {
    val format = Json { prettyPrint = true }

    // parse into json, so can log it out with pretty print
    val body = request.bodyAsString()
    var bodyInPrettyPrint = ""
    if (!body.isNullOrBlank()) {
      val jsonElement = format.decodeFromString<JsonElement>(body)
      bodyInPrettyPrint = format.encodeToString(jsonElement)
    }

    logger.debug(
      "sending ${request.method} ${request.url}:" +
          "\nheaders: ${request.headers}" +
          "body: $bodyInPrettyPrint"
    )
  }

  fun printResponse(response: Response): String {
    val body = response.body?.string()
    logger.debug("${response.request.url} response:\n${response.code}: $body")
    return body!!
  }

  fun <T> parseError(responseBody: String): com.highmobility.hmkitfleet.network.Response<T> {
    val json = Json.parseToJsonElement(responseBody)
    if (json is JsonObject) {
      // there are 3 error formats
      val errors = json["errors"] as? JsonArray

      return if (errors != null && errors.size > 0) {
        val error =
          Json.decodeFromJsonElement<Error>(errors.first())
        Response(null, error)
      } else {
        val error = Error(
          json["error"]?.jsonPrimitive?.content ?: "Unknown server response",
          json["error_description"]?.jsonPrimitive?.content
        )

        Response(null, error)
      }
    } else if (json is JsonArray) {
      if (json.size > 0) {
        val error = Json.decodeFromJsonElement<Error>(json.first())
        return Response(null, error)
      }
    }

    return Response(null, genericError("Unknown server response"))
  }

  internal fun requestBody(values: Map<String, String>): RequestBody {
    val completeBody = buildJsonObject {
      for (item in values) {
        put(item.key, item.value)
      }
    }

    return completeBody.toString().toRequestBody(mediaType)
  }

  internal fun getJwt(configuration: ServiceAccountApiConfiguration, crypto: Crypto): String {
    val header = buildJsonObject {
      put("alg", "ES256")
      put("typ", "JWT")
    }.toString()

    val jwtBody = buildJsonObject {
      put("ver", 2) // configuration.version)
      put("iss", configuration.serviceAccountPrivateKeyId)
      put("aud", baseUrl)
      put("jti", configuration.createJti())
      put("iat", configuration.createIat())
    }.toString()

    val headerBase64 = Base64.encodeUrlSafe(header.toByteArray())
    val bodyBase64 = Base64.encodeUrlSafe(jwtBody.toByteArray())
    val jwtContent = String.format("%s.%s", headerBase64, bodyBase64)
    val privateKey = configuration.getServiceAccountHmPrivateKey()
    val jwtSignature = crypto.signJWT(jwtContent.toByteArray(), privateKey)

    return String.format("%s.%s", jwtContent, jwtSignature.base64UrlSafe)
  }
}

internal fun Request.bodyAsString(): String? {
  if (this.body == null) return null
  val buffer = Buffer()
  this.body?.writeTo(buffer)
  return buffer.readUtf8()
}

internal fun genericError(detail: String? = null): Error {
  val genericError = Error("Unknown server response", detail)
  return genericError
}
