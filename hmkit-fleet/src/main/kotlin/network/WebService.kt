package network

import okhttp3.OkHttpClient
import kotlinx.coroutines.*
import org.slf4j.Logger



class WebService(val apiKey: String, val client: OkHttpClient, val logger: Logger) {


    interface IClearVehicleResult {
        fun onResult(result: ClearVehicleResponse)
    }

    suspend fun clearVehicle(vin: String): ClearVehicleResponse {
        val authToken = getAuthToken()
        val response = ClearVehicleResponse(vin, ClearVehicleResponse.Status.PENDING)

        val extraDelay = (1000L * Math.random()).toLong()
        delay(300L + extraDelay)
        logger.debug("webService response: $vin")

        // .. do the clear vehicles requests
        return response

        // POST

        // 2.
        // /v1​/fleets​/vehicles
        // auth header: service account token.

        // 3. return request response. otherwise user can poll the status

        /*
        Register a fleet vehicle for data access clearance. Once the VIN has its status changed to
        "approved", an access token can be retrieved for the VIN.
         */
    }

    private suspend fun getAuthToken(): String {
        // 1. create jwt auth token
        /*
            val header = "{\"alg\":\"ES256\",\"typ\":\"JWT\"}"
            val body = "{\"code_verifier\":\"${nonce.hex}\",\"serial_number\":\"${deviceSerial.hex}\"}"

            val headerBase64 = Base64.encodeUrlSafe(header.toByteArray())
            val bodyBase64 = Base64.encodeUrlSafe(body.toByteArray())

            val jwtContent = String.format("%s.%s", headerBase64, bodyBase64)
            val jwtSignature = crypto.signJWT(jwtContent.toByteArray(), privateKey)

            return String.format("%s.%s", jwtContent, jwtSignature.base64UrlSafe)
         */
        /*
        {
          "version": "2.0",
          "type": "rest_api",
          "private_key_id": "302ee1ad-4781-444f-bb23-65d80db32263",
          "private_key": "***REMOVED***",
          "app_uri": "https://rest-api.high-mobility.com/v3",
          "app_id": "8F063617071E4426F013D44E"
        }

        val config = {"inserted_at":"2020-10-28T03:21:15","private_key":"***REMOVED***","id":"38d09f99-6e9d-464f-b8cd-eb9d1ceddfb3"}
         */

        /*
        jwt example
        {
          "iss": "<i>Api Key uuid</i>",
          "aud": "https://sandbox.api.high-mobility.com/v1",
          "iat": "<i>Current datetime in <a href="https://tools.ietf.org/html/rfc7519#page-6">UNIX timestamp</a></a></i>",
          "jti": "<i>A random and unique UUIDv4</i>",
          "ver": 1
        }
         */

        delay(500)
        return "fakeToken ${Math.random()}"
    }

    companion object {
        val url = "https://develop.high-mobility.net/"
    }
}