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

        val privateKeyEncoded = configuration.privateKey
        val privateKeyBytes = configuration.getPrivateKeyBytes()

        // url is {base}/auth_tokens

        delay(500)
        return "fakeToken ${Math.random()}"
    }
}