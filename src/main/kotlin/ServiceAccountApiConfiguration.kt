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
@file:UseSerializers(DeviceCertificateSerializer::class)

import com.highmobility.crypto.DeviceCertificate
import com.highmobility.crypto.value.PrivateKey
import com.highmobility.utils.Base64
import com.highmobility.value.Bytes
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

typealias ClientCertificate = DeviceCertificate

@Serializable
data class ServiceAccountApiConfiguration(
    /**
     * Service account API key
     */
    val apiKey: String,
    /**
     * This private key is downloaded when creating a Service Account API key. It should be in
     * PKCS 8 format
     */
    val serviceAccountApiPrivateKey: String,

    /**
     * The client certificate
     */
    val clientCertificate: ClientCertificate,

    /**
     * This key is paired with the app's client certificate. It should be the 32 bytes of the
     * ANSI X9.62 Prime 256v1 curve in hex or base64.
     */
    val clientPrivateKey: String,

    /**
     * The OAuth client ID.
     */
    val clientId: String,

    /**
     * The OAuth client secret.
     */
    val clientSecret: String
) {
    val version = 1

    fun createJti() = UUID.randomUUID().toString()
    fun createIat() = (System.currentTimeMillis() / 1000)

    fun getServiceAccountHmPrivateKey(): PrivateKey {
        val bigIntegerBytes = getServiceAccountJavaPrivateKey().s.toByteArray()
        val privateKeyBytes = ByteArray(32)

        for (i in 0..31) {
            privateKeyBytes[i] = bigIntegerBytes[i + 1]
        }

        return PrivateKey(privateKeyBytes)
    }

    fun getServiceAccountJavaPrivateKey(): ECPrivateKey {
        var encodedKeyString = serviceAccountApiPrivateKey
        encodedKeyString = encodedKeyString.replace("-----BEGIN PRIVATE KEY----", "")
        encodedKeyString = encodedKeyString.replace("-----END PRIVATE KEY-----", "")
        val decodedPrivateKey = Base64.decode(encodedKeyString)
        val keySpec = PKCS8EncodedKeySpec(decodedPrivateKey)
        // how to convert PKCS#8 to EC private key https://stackoverflow.com/a/52301461/599743
        val kf = KeyFactory.getInstance("EC")
        val ecPrivateKey = kf.generatePrivate(keySpec) as ECPrivateKey
        return ecPrivateKey
    }

    fun getClientPrivateKey(): PrivateKey {
        return PrivateKey(clientPrivateKey)
    }
}

object DeviceCertificateSerializer : KSerializer<DeviceCertificate> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(output: Encoder, obj: DeviceCertificate) {
        output.encodeString((obj as Bytes).toString())
    }

    override fun deserialize(input: Decoder): DeviceCertificate {
        return DeviceCertificate(Bytes(input.decodeString()))
    }
}