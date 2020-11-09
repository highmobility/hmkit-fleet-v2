package network

import ServiceAccountApiConfiguration
import com.highmobility.hmkit.HMKit
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import org.slf4j.Logger

class WebService(
    val configuration: ServiceAccountApiConfiguration,
    val client: OkHttpClient,
    val hmkitOem: HMKit,
    val logger: Logger
) {
    suspend fun clearVehicle(vin: String): ClearVehicleResponse {
        // fleets/vehicles endpoint
        // post /v1/fleets/vehicles
        // auth header: token

        // flow:
        // 1) create jwt with apiKey and exchange it to auth token
        val authToken = getAuthToken()

        /*
        Once a vehicle VIN has been cleared, it’s possible to get an access token for that vehicle.
        With the access token it will be possible to use our REST API (or any SDK) to get data.
        */

        /*
        By using the POST /fleets/access_tokens endpoint, the data customer passes in the VIN and
         receives an access token (and a refresh token) The access token is used with the REST API
         to fetch data
         */
        /*
        VIN clearing can take a long time. For BMW we need to send email. we will only return
        response here
         */

        val response = ClearVehicleResponse(vin, ClearVehicleResponse.Status.PENDING)

        val extraDelay = (1000L * Math.random()).toLong()
        delay(300L + extraDelay)
        logger.debug("webService response: $vin")

        // 3. return request response. otherwise user can poll the status
        /*
        Register a fleet vehicle for data access clearance. Once the VIN has its status changed to
        "approved", an access token can be retrieved for the VIN.
         */
        return response
    }

    private suspend fun getAuthToken(): String {
        // 1. create jwt auth token
        val jwtContent = mapOf(
            "ver" to configuration.version,
            "iss" to configuration.apiKey,
            "aud" to configuration.baseUrl,
            "iat" to System.currentTimeMillis() / 1000
        )

        // "***REMOVED***"
        val privateKeyEncoded = configuration.privateKey
        val privateKeyBytes = configuration.getPrivateKeyBytes()

        println("priv key bytes $privateKeyBytes")
        val i = 1
        // TODO: get the private key bytes from encoded string

        /*
        Client certificate:
        {
          "version": "2.0",
          "type": "rest_api",
          "private_key_id": "302ee1ad-4781-444f-bb23-65d80db32263",
          "private_key": "***REMOVED***",
          "app_uri": "https://rest-api.high-mobility.com/v3",
          "app_id": "8F063617071E4426F013D44E"
        }

        val config = {
        "inserted_at":"2020-10-28T03:21:15",
        "private_key":"***REMOVED***",
        "id":"38d09f99-6e9d-464f-b8cd-eb9d1ceddfb3"
        }
         */

        /*
        jwt example from tutorial
        {
          "iss": "<i>Api Key uuid</i>",
          "aud": "https://sandbox.api.high-mobility.com/v1",
          "iat": "<i>Current datetime in <a href="https://tools.ietf.org/html/rfc7519#page-6">UNIX timestamp</a></a></i>",
          "jti": "<i>A random and unique UUIDv4</i>",
          "ver": 1
        }
         */

        /*
        hmkit-android example
        val header = "{\"alg\":\"ES256\",\"typ\":\"JWT\"}"
        val body = "{\"code_verifier\":\"${nonce.hex}\",\"serial_number\":\"${deviceSerial.hex}\"}"

        val headerBase64 = Base64.encodeUrlSafe(header.toByteArray())
        val bodyBase64 = Base64.encodeUrlSafe(body.toByteArray())

        val jwtContent = String.format("%s.%s", headerBase64, bodyBase64)
        val jwtSignature = crypto.signJWT(jwtContent.toByteArray(), privateKey)

        return String.format("%s.%s", jwtContent, jwtSignature.base64UrlSafe)
         */

        /*
        get public key from client cert:
        and @doofyus did you have a script to print the public key from a device certifiate hex e.g.
***REMOVED***

         */

        // url is {base}/auth_tokens

        delay(500)
        return "fakeToken ${Math.random()}"
    }
}