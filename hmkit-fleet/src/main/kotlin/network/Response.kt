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

import com.highmobility.value.Bytes
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

data class Response<T>(
    val response: T? = null,
    val error: Error? = null
)

/**
 * Telematics command response object.
 *
 * Either [response] or [errors] with at least 1 error is present. Never both of these. Please note that the [TelematicsCommandResponse] object could contain an error from the car. In that case the [errors] list will still be empty.
 */
data class TelematicsResponse(
    /**
     * Response regarding the telematics command. e.g. success/failure/vehicle not found
     * Filled when statusCode is 200, 400, 404, 408
     */
    val response: TelematicsCommandResponse? = null,

    /**
     * Some other error
     * Filled when statusCode is 422, or some other server/SDK error occurred.
     */
    val errors: List<Error> = emptyList()
)

@Serializable
data class TelematicsCommandResponse(
    val message: String,
    @Serializable(with = BytesSerializer::class)
    @SerialName("response_data")
    val responseData: Bytes,
    @Serializable(with = Status.Serializer::class) val status: Status
) {
    enum class Status {
        OK, ERROR, TIMEOUT;

        object Serializer : KSerializer<Status> {
            override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Enum", PrimitiveKind.STRING)

            override fun serialize(encoder: Encoder, value: Status) {
                encoder.encodeString(value.toString())
            }

            override fun deserialize(decoder: Decoder): Status {
                // we deserialize server response also, which is lowercase
                val string = decoder.decodeString()
                return Status.valueOf(string.uppercase())
            }
        }
    }
}

object BytesSerializer : KSerializer<Bytes> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Bytes", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Bytes) {
        encoder.encodeString(value.base64)
    }

    override fun deserialize(decoder: Decoder): Bytes {
        val string = decoder.decodeString()
        return Bytes(string)
    }
}
