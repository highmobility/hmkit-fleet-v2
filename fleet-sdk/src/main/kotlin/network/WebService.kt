package network

import okhttp3.OkHttpClient
import kotlinx.coroutines.*
import kotlin.coroutines.Continuation

class WebService(val apiKey: String, client: OkHttpClient) {
    class RequestClearanceResponse(val vin: String, val status: Status, val error: Error? = null) {
        enum class Status { PENDING, APPROVED, FAILED }
        class Error(val title: String?, val description: String, val source: String?)
    }

    interface IClearVehicleResult {
        fun onResult(result: RequestClearanceResponse)
    }

    suspend fun clearVehicle(vin: String) : RequestClearanceResponse {
        val authToken = getAuthToken()

        delay(500)

        // .. do the clear vehicles requests
        return RequestClearanceResponse(vin, RequestClearanceResponse.Status.PENDING)

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

        delay(500)
        return "fakeToken ${Math.random()}"
    }

    companion object {
        val url = "https://develop.high-mobility.net/"
    }
}