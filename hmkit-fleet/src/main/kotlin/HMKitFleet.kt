import com.highmobility.hmkit.HMKit
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import network.ClearVehicleResponse
import network.WebService
import org.koin.core.component.inject
import org.slf4j.Logger
import java.util.concurrent.CompletableFuture

class HMKitFleet private constructor(apiKey: String) : DI.FleetSdkKoinComponent {
    private val logger by inject<Logger>()
    private val hmkit by inject<HMKit>()
     val webService by inject<WebService>()

    init {
        DI.start(apiKey)
        logger.info("HMKit Fleet initialised")
    }

    fun requestClearance(vin: String): CompletableFuture<ClearVehicleResponse> {
        // fleets/vehicles endpoint
        // post /v1/fleets/vehicles
        // auth header: token

        // flow:
        /*
        1) I have the api key, now have to use JWT for requests.
         */

        /*
        Once a vehicle VIN has been cleared, it’s possible to get an access token for that vehicle.
        With the access token it will be possible to use our REST API (or any SDK) to get data.
        */

        /*
        By using the POST /fleets/access_tokens endpoint, the data customer passes in the VIN and
         receives an access token (and a refresh token) The access token is used with the REST API
         to fetch data
         */
        logger.debug("HMKitFleet: requestClearance: $vin")
        /*
        VIN clearing can take a long time. For BMW we need to send email.
         */
        return GlobalScope.future { webService.clearVehicle(vin) }
    }

    fun revokeClearance(vin: String): CompletableFuture<Boolean> {
        // TODO: 30/10/20 implement
        /*
        Although the OAuth2 API is not used to get an access token, it is used to refresh the access
        token and to revoke access. Revoking access means that the VIN is removed from the clearance list.

        By using the POST /access_tokens endpoint, the data customer passes in the refresh token and
         receives a new access token

        By using the DELETE /access_tokens endpoint, the data customer passes in the access token
        and the vehicle is deleted from the clearance list
         */

        return CompletableFuture()
    }

    // TODO: endpoints
    //  The SDK implements all endpoints of the Service Account API except for the device certificate
    //  endpoints, which are not needed

    companion object {
        @Volatile
        private var INSTANCE: HMKitFleet? = null

        /**
         * Get the Fleet SDK instance.
         *
         * @param apiKey The API key
         */
        @JvmStatic
        fun getInstance(apiKey: String): HMKitFleet =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildFleetSdk(apiKey).also { INSTANCE = it }
            }

        private fun buildFleetSdk(apiKey: String) = HMKitFleet(apiKey)
    }
}