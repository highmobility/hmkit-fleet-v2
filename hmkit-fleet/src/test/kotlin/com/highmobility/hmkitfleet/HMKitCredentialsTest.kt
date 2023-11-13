package com.highmobility.hmkitfleet.com.highmobility.hmkitfleet

import com.highmobility.crypto.Crypto
import com.highmobility.hmkitfleet.BaseTest
import com.highmobility.hmkitfleet.HMKitCredentials
import com.highmobility.hmkitfleet.HMKitOAuthCredentials
import com.highmobility.hmkitfleet.HMKitPrivateKeyCredentials
import com.highmobility.utils.Base64
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
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

    val crypto = mockk<Crypto> {
      every {
        signJWT(
          any<ByteArray>(),
          any()
        )
      } returns mockk {
        every { base64UrlSafe } returns "base64Signature"
      }
    }

    val jwtProvider = object : HMKitCredentials.JwtProvider {
      override fun getBaseUrl() = "base_url"
      override fun getCrypto() = crypto
      override fun generateUuid() = uuid
      override fun getTimestamp() = timestamp
    }

    val requestBody = credentials.getTokenRequestBody(jwtProvider)
    val expected = expectedHeaderAndBody(privateKeyId, jwtProvider)

    verify { crypto.signJWT(any<ByteArray>(), any()) }

    val json = Json.decodeFromString<JsonObject>(requestBody)
    val jwt = json["client_assertion"]?.jsonPrimitive?.content?.split(".")
    assert(jwt?.get(0) == expected.first)
    assert(jwt?.get(1) == expected.second)
    assert(jwt?.get(2) == "base64Signature")
    assert(json["client_id"]?.jsonPrimitive?.content == "client_id")
    assert(json["grant_type"]?.jsonPrimitive?.content == "client_credentials")
    assert(json["client_assertion_type"]?.jsonPrimitive?.content == "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
  }

  private fun expectedHeaderAndBody(
    privateKeyId: String,
    jwtProvider: HMKitCredentials.JwtProvider,
  ): Pair<String, String> {
    val header = buildJsonObject {
      put("alg", "ES256")
      put("typ", "JWT")
    }

    val body = buildJsonObject {
      put("ver", 2)
      put("iss", privateKeyId)
      put("aud", jwtProvider.getBaseUrl())
      put("jti", jwtProvider.generateUuid())
      put("iat", jwtProvider.getTimestamp() / 1000)
    }

    val headerBase64 = Base64.encodeUrlSafe(header.toString().toByteArray())
    val bodyBase64 = Base64.encodeUrlSafe(body.toString().toByteArray())

    return Pair(headerBase64, bodyBase64)
  }
}