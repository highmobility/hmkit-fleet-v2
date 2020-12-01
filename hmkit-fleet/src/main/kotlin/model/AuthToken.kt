@file:UseSerializers(DateTimeSerializer::class)

package model

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime

@Serializable
internal data class AuthToken(
    @SerialName("auth_token")
    val authToken: String,
    @SerialName("valid_from")
    val validFrom: LocalDateTime,
    @SerialName("valid_until")
    val validUntil: LocalDateTime
)

internal object DateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(output: Encoder, obj: LocalDateTime) {
        output.encodeString(obj.toString())
    }

    override fun deserialize(input: Decoder): LocalDateTime {
        return LocalDateTime.parse(input.decodeString())
    }
}