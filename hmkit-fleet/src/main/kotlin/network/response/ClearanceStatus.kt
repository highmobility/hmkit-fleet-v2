@file:UseSerializers(ClearanceStatus.EmumSerializer::class)

package network.response

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.internal.StringDescriptor

@Serializable
data class ClearanceStatus(val vin: String, val status: Status) {
    enum class Status {
        PENDING,
        APPROVED,
        FAILED
    }

    @Serializer(forClass = Status::class)
    object EmumSerializer : KSerializer<Status> {
        override val descriptor: SerialDescriptor = StringDescriptor
        override fun serialize(output: Encoder, obj: Status) {
            output.encodeString(obj.toString().toLowerCase())
        }

        override fun deserialize(input: Decoder): Status {
            return Status.valueOf(input.decodeString().toUpperCase())
        }
    }

}