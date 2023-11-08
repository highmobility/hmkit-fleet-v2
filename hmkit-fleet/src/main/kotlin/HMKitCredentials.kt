package com.highmobility.hmkitfleet

import com.highmobility.crypto.Crypto
import com.highmobility.crypto.value.PrivateKey
import com.highmobility.utils.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

abstract class HMKitCredentials {
  internal abstract fun getTokenRequestBody(jwtProvider: JwtProvider?): String

  interface JwtProvider {
    fun getBaseUrl(): String
    fun getCrypto(): Crypto
    fun generateUuid(): String
    fun getTimestamp(): Long
  }
}

@Serializable
data class HMKitOAuthCredentials(
  /**
   * The OAuth client ID.
   */
  val clientId: String,

  /**
   * The OAuth client secret.
   */
  val clientSecret: String,
) : HMKitCredentials() {
  override fun getTokenRequestBody(
    jwtProvider: JwtProvider?
  ): String {
    return buildJsonObject {
      put("client_id", clientId)
      put("client_secret", clientSecret)
      put("grant_type", "client_credentials")
    }.toString()
  }
}

@Serializable
data class HMKitPrivateKeyCredentials(
  /**
   * The OAuth client ID.
   */
  val clientId: String,
  /**
   * The full contents of the private key JSON file
   */
  val privateKey: String,
) : HMKitCredentials() {
  override fun getTokenRequestBody(
    jwtProvider: JwtProvider?
  ): String {
    val credentials = OAuthPrivateKey(privateKey)

    val jwt = getJwt(
      credentials,
      jwtProvider!!
    )

    return buildJsonObject {
      put("client_id", clientId)
      put("client_assertion", jwt)
      put("grant_type", "client_credentials")
      put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
    }.toString()
  }
}

internal fun getJwt(
  privateKey: OAuthPrivateKey,
  jwtProvider: HMKitCredentials.JwtProvider
): String {
  val crypto: Crypto = jwtProvider.getCrypto()
  val baseUrl = jwtProvider.getBaseUrl()
  val uuid = jwtProvider.generateUuid()
  val timestamp = jwtProvider.getTimestamp()

  val header = buildJsonObject {
    put("alg", "ES256")
    put("typ", "JWT")
  }.toString()

  val jwtBody = buildJsonObject {
    put("ver", 2)
    put("iss", privateKey.serviceAccountPrivateKeyId)
    put("aud", baseUrl)
    put("jti", uuid)
    put("iat", timestamp / 1000)
  }.toString()

  val headerBase64 = Base64.encodeUrlSafe(header.toByteArray())
  val bodyBase64 = Base64.encodeUrlSafe(jwtBody.toByteArray())
  val jwtContent = String.format("%s.%s", headerBase64, bodyBase64)
  val cryptoPrivateKey = privateKey.getServiceAccountHmPrivateKey()
  val jwtSignature = crypto.signJWT(jwtContent.toByteArray(), cryptoPrivateKey)

  return String.format("%s.%s", jwtContent, jwtSignature.base64UrlSafe)
}

@Serializable
class OAuthPrivateKey {
  @SerialName("private_key")
  private val serviceAccountPrivateKey: String

  @SerialName("id")
  val serviceAccountPrivateKeyId: String

  constructor(privateKeyJson: String) {
    val json = Json.decodeFromString<JsonObject>(privateKeyJson)

    // PKCS 8 format
    serviceAccountPrivateKey = json["private_key"]!!.jsonPrimitive.content
    serviceAccountPrivateKeyId = json["id"]!!.jsonPrimitive.content
  }

  internal fun getServiceAccountHmPrivateKey(): PrivateKey {
    return PrivateKey(serviceAccountPrivateKey, PrivateKey.Format.PKCS8)
  }
}