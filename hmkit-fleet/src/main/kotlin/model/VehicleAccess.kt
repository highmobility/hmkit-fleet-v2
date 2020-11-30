@file:UseSerializers(AccessCertificateSerializer::class)

package model

import com.highmobility.crypto.AccessCertificate
import com.highmobility.value.Bytes
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import network.response.AccessToken

@Serializable
data class VehicleAccess(
    val vin: String,
    val brand: Brand,
    val accessToken: AccessToken,
    val accessCertificate: AccessCertificate
)

@Serializer(forClass = AccessCertificate::class)
object AccessCertificateSerializer : KSerializer<AccessCertificate> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(output: Encoder, obj: AccessCertificate) {
        output.encodeString((obj as Bytes).toString())
    }

    override fun deserialize(input: Decoder): AccessCertificate {
        return AccessCertificate(Bytes(input.decodeString()))
    }
}

enum class Brand {
    BMW, MERCEDES_BENZ, MINI
}