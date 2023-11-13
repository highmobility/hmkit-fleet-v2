package com.highmobility.hmkitfleet

import com.highmobility.crypto.Crypto
import com.highmobility.crypto.value.PrivateKey
import com.highmobility.utils.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
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
   * The PKCS8 formatted private key. It is included in the .json file downloaded from the developer console after creating a new private key in the App>OAuth section.
   */
  val privateKey: String,
  /**
   * The private key ID. It is included in the .json file downloaded from the developer console after creating a new private key in the App>OAuth section.
   */
  val privateKeyId: String
) : HMKitCredentials() {
  override fun getTokenRequestBody(
    jwtProvider: JwtProvider?
  ): String {
    val jwt = getJwt(
      privateKey,
      privateKeyId,
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
  privateKey: String,
  privateKeyId: String,
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
    put("iss", privateKeyId)
    put("aud", baseUrl)
    put("jti", uuid)
    put("iat", timestamp / 1000)
  }.toString()

  val hmPrivateKey = PrivateKey(privateKey, PrivateKey.Format.PKCS8)
  val headerBase64 = Base64.encodeUrlSafe(header.toByteArray())
  val bodyBase64 = Base64.encodeUrlSafe(jwtBody.toByteArray())
  val jwtContent = String.format("%s.%s", headerBase64, bodyBase64)
  val jwtSignature = crypto.signJWT(jwtContent.toByteArray(), hmPrivateKey)

  return String.format("%s.%s", jwtContent, jwtSignature.base64UrlSafe)
}