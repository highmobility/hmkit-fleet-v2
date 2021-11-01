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

package com.highmobility.hmkitfleet

import com.highmobility.crypto.DeviceCertificate
import com.highmobility.crypto.value.PrivateKey
import com.highmobility.value.Bytes
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

typealias ClientCertificate = DeviceCertificate

/**
 * Values required for initialising the Fleet SDK.
 */
@Serializable
data class ServiceAccountApiConfiguration(
    /**
     * Service account API key
     */
    val serviceAccountApiKey: String,
    /**
     * This private key is downloaded when creating a Service Account API key. It should be in
     * PKCS 8 format
     */
    val serviceAccountPrivateKey: String,

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
    val oauthClientId: String,

    /**
     * The OAuth client secret.
     */
    val oauthClientSecret: String
) {
    /**
     * Client Certificate in base64 or hex
     */
    constructor(
        serviceAccountApiKey: String,
        serviceAccountPrivateKey: String,
        clientCertificate: String,
        clientPrivateKey: String,
        oauthClientId: String,
        oauthClientSecret: String
    ) : this(
        serviceAccountApiKey,
        serviceAccountPrivateKey,
        ClientCertificate(clientCertificate),
        clientPrivateKey,
        oauthClientId,
        oauthClientSecret
    )

    val version = 1

    internal fun createJti() = UUID.randomUUID().toString()
    internal fun createIat() = (System.currentTimeMillis() / 1000)

    internal fun getServiceAccountHmPrivateKey(): PrivateKey {
        return PrivateKey(serviceAccountPrivateKey, PrivateKey.Format.PKCS8)
    }

    internal fun getClientPrivateKey(): PrivateKey {
        return PrivateKey(clientPrivateKey)
    }
}

object DeviceCertificateSerializer : KSerializer<DeviceCertificate> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DeviceCertificate) {
        encoder.encodeString((value as Bytes).toString())
    }

    override fun deserialize(decoder: Decoder): DeviceCertificate {
        return DeviceCertificate(Bytes(decoder.decodeString()))
    }
}