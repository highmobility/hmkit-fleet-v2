import com.highmobility.crypto.value.PrivateKey
import com.highmobility.utils.Base64
import kotlinx.serialization.Serializable

import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

@Serializable
data class ServiceAccountApiConfiguration @JvmOverloads constructor(
    val apiKey: String,
    val privateKey: String,
) {
    val version = 1

    fun createJti() = UUID.randomUUID().toString()
    fun createIat() = (System.currentTimeMillis() / 1000)

    fun getHmPrivateKey(): PrivateKey {
        val bigIntegerBytes = getJavaPrivateKey().s.toByteArray()
        val privateKeyBytes = ByteArray(32)

        for (i in 0..31) {
            privateKeyBytes[i] = bigIntegerBytes[i + 1]
        }

        return PrivateKey(privateKeyBytes)
    }

    fun getJavaPrivateKey(): ECPrivateKey {
        var encodedKeyString = privateKey
        encodedKeyString = encodedKeyString.replace("-----BEGIN PRIVATE KEY----", "")
        encodedKeyString = encodedKeyString.replace("-----END PRIVATE KEY-----", "")
        val decodedPrivateKey = Base64.decode(encodedKeyString)
        val keySpec = PKCS8EncodedKeySpec(decodedPrivateKey)
        // how to convert PKCS#8 to EC private key https://stackoverflow.com/a/52301461/599743
        val kf = KeyFactory.getInstance("EC")
        val ecPrivateKey = kf.generatePrivate(keySpec) as ECPrivateKey
        return ecPrivateKey
    }
}