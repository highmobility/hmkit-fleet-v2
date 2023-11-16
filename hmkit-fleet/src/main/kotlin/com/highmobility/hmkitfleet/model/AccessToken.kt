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
@file:UseSerializers(ZonedDateTimeSerializer::class)

package com.highmobility.hmkitfleet.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Clock.systemUTC
import java.time.ZonedDateTime

@Serializable
internal data class AccessToken(
  @SerialName("access_token")
  val accessToken: String,
  @SerialName("expires_in")
  val expiresIn: Int
) {
  private val validUntil: ZonedDateTime = ZonedDateTime.now(systemUTC()).plusSeconds(expiresIn.toLong())

  fun isExpired(): Boolean {
    val now = ZonedDateTime.now(systemUTC())
    return now.isAfter(validUntil.minusMinutes(1))
  }
}

internal object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: ZonedDateTime) {
    encoder.encodeString(value.toString())
  }

  override fun deserialize(decoder: Decoder): ZonedDateTime {
    return ZonedDateTime.parse(decoder.decodeString())
  }
}
