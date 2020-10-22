import com.highmobility.hmkit.HMKit

object FleetSdk {
    private val logger = Logger.getLogger()

    var hmkit: HMKit = HMKit.getInstance()

    fun init(apiKey: String) {
        /*
        Open question: should the SDK be initiated with the production app client certificate as well? Or should the
        production app ID be sent in and the SDK downloads a device/client certificate automatically?
         */
        logger.debug("Fleet sdk initialised")
        val keypair = hmkit.crypto.createKeypair()
        logger.debug(keypair.privateKey.toString())
    }

    fun clearVehicles(vins: List<String>, result: (Boolean) -> Unit) {
        // fleets/vehicles endpoint

        /*
        Once a vehicle VIN has been cleared, it’s possible to get an access token for that vehicle.
        With the access token it will be possible to use our REST API (or any SDK) to get data.
        */

        /*
        By using the POST /fleets/access_tokens endpoint, the data customer passes in the VIN and
         receives an access token (and a refresh token) The access token is used with the REST API
         to fetch data
         */
    }

    // TODO: endpoints
    //  The SDK implements all endpoints of the Service Account API except for the device certificate
    //  endpoints, which are not needed


    fun deleteVehicleClearance(vin: String) {
        /*
        Although the OAuth2 API is not used to get an access token, it is used to refresh the access
        token and to revoke access. Revoking access means that the VIN is removed from the clearance list.

        By using the POST /access_tokens endpoint, the data customer passes in the refresh token and
         receives a new access token

        By using the DELETE /access_tokens endpoint, the data customer passes in the access token
        and the vehicle is deleted from the clearance list
         */
    }
}