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
package com.highmobility.hmkitfleet

import io.jsonwebtoken.Jwts
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

class HMKitCredentialsTest : BaseTest() {
  @Test
  fun oAuthCredentials() {
    val credentials: HMKitCredentials = HMKitOAuthCredentials(
      "client_id",
      "client_secret"
    )

    val requestBody = credentials.getTokenRequestBody(null)

    val json = Json.decodeFromString<JsonObject>(requestBody)
    assert(json["client_id"]?.jsonPrimitive?.content == "client_id")
    assert(json["client_secret"]?.jsonPrimitive?.content == "client_secret")
  }

  @Test
  fun jwtCredentials() {
    val timestamp = 1000L
    val uuid = "00000000-0000-0000-0000-000000000000"

    val privateKeyJson = Json.decodeFromString<JsonObject>(readPrivateKeyJsonString())
    val privateKey = privateKeyJson["private_key"]?.jsonPrimitive?.content ?: ""
    val privateKeyId = privateKeyJson["id"]?.jsonPrimitive?.content ?: ""

    val credentials: HMKitCredentials = HMKitPrivateKeyCredentials(
      "client_id",
      privateKey,
      privateKeyId
    )

    val jwtProvider = object : HMKitCredentials.JwtProvider {
      override fun getBaseUrl() = "base_url"
      override fun generateUuid() = uuid
      override fun getTimestamp() = timestamp
    }

    val requestBody = credentials.getTokenRequestBody(jwtProvider)
    val expected = expectedHeaderAndBody(privateKeyId, privateKey, jwtProvider)

    val json = Json.decodeFromString<JsonObject>(requestBody)
    val jwt = json["client_assertion"]?.jsonPrimitive?.content?.split(".")
    println("${jwt?.get(0)}:${jwt?.get(1)}:${jwt?.get(2)}")
    println("${expected.first}:${expected.second}:${expected.third}")
    assert(jwt?.get(0) == expected.first)
    assert(jwt?.get(1) == expected.second)
    // assert(jwt?.get(2) == expected.third) // signature can be different
    assert(json["client_id"]?.jsonPrimitive?.content == "client_id")
    assert(json["grant_type"]?.jsonPrimitive?.content == "client_credentials")
    assert(
      json["client_assertion_type"]?.jsonPrimitive?.content == "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
    )
  }

  private fun expectedHeaderAndBody(
    privateKeyId: String,
    privateKey: String,
    jwtProvider: HMKitCredentials.JwtProvider,
  ): Triple<String, String, String> {
    val jwt = Jwts.builder()
      .header() // alg is set automatically
      .type("JWT")
      .and()
      .claim("ver", 2)
      .claim("iss", privateKeyId)
      .claim("jti", jwtProvider.generateUuid())
      .claim("iat", jwtProvider.getTimestamp())
      .audience()
      .single(jwtProvider.getBaseUrl())
      .signWith(getPrivateKey(privateKey))
      .compact()

    val split = jwt.split(".")

    return Triple(split[0], split[1], split[2])
  }
}
