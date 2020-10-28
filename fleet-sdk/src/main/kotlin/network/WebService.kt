package network

import okhttp3.OkHttpClient
import org.koin.core.KoinComponent
import org.koin.core.inject

class WebService(val apiKey: String) : KoinComponent {
    val client by inject<OkHttpClient>()

    fun clearVehicles(vins: List<String>) {
        getAccessToken()
        // POST
        /*
         /v1​/fleets​/vehicles
            auth header: service account token.
        */

        /*
        Register a fleet vehicle for data access clearance. Once the VIN has its status changed to
        "approved", an access token can be retrieved for the VIN.
         */
    }

    private fun getAccessToken() {

    }

    companion object {
        val url = "https://develop.high-mobility.net/"
    }
}