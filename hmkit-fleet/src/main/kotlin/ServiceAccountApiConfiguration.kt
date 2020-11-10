import com.highmobility.utils.Base64
import kotlinx.serialization.Serializable

import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec

@Serializable
data class ServiceAccountApiConfiguration @JvmOverloads constructor(
    val apiKey: String,
    val privateKey: String,
    val environment: Environment = Environment.PRODUCTION
) {
    enum class Environment {
        PRODUCTION, SANDBOX, DEV_SANDBOX;

        val url: String
            get() {
                return when (this) {
                    PRODUCTION -> "https://api.high-mobility.com/v1"
                    SANDBOX -> "https://sandbox.api.high-mobility.com/v1"
                    DEV_SANDBOX -> "https://sandbox.api.develop.high-mobility.net/v1"
                }
            }

        /*
        Note that the base URL is https://sandbox.api.high-mobility.com/v1 when working with the car
        emulators and https://api.high-mobility.com/v1 for cars in production mode.
         */
    }

    val baseUrl = environment.url
    val version = 2

    fun getHmPrivateKey(): com.highmobility.crypto.value.PrivateKey {
        var encodedKeyString = privateKey
        encodedKeyString = encodedKeyString.replace("-----BEGIN PRIVATE KEY-----\n", "")
        encodedKeyString = encodedKeyString.replace("\n-----END PRIVATE KEY-----\n\n", "")
        val decodedPrivateKey = Base64.decode(encodedKeyString)
        val keySpec = PKCS8EncodedKeySpec(decodedPrivateKey)
        // how to convert PKCS#8 to EC private key https://stackoverflow.com/a/52301461/599743
        val kf = KeyFactory.getInstance("EC")
        val ecPrivateKey = kf.generatePrivate(keySpec) as ECPrivateKey
        val bigIntegerBytes = ecPrivateKey.s.toByteArray()
        val privateKeyBytes = ByteArray(32)
        for (i in 0..31) {
            privateKeyBytes[i] = bigIntegerBytes[32 - i]
        }

        return com.highmobility.crypto.value.PrivateKey(privateKeyBytes)
    }
}