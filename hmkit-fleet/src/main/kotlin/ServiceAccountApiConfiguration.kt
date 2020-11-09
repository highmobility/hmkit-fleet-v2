import com.highmobility.utils.Base64
import com.highmobility.value.Bytes
import kotlinx.serialization.Serializable

import java.security.KeyFactory
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

    fun getPrivateKeyBytes(): Bytes {
        val bytes = Bytes("00")

        var encodedKeyString = privateKey
        encodedKeyString = encodedKeyString.replace("-----BEGIN PRIVATE KEY-----\n", "")
        encodedKeyString = encodedKeyString.replace("\n-----END PRIVATE KEY-----\n\n", "")
        val keySpec = PKCS8EncodedKeySpec(Base64.decode(encodedKeyString))

        val kf = KeyFactory.getInstance("EC")
        val privKey = kf.generatePrivate(keySpec)
        val keyBytes = privKey.encoded

        return Bytes(keyBytes)
    }
}