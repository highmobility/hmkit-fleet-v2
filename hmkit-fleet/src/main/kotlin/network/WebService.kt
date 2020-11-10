package network

import ServiceAccountApiConfiguration
import com.highmobility.hmkit.HMKit
import com.highmobility.utils.Base64
import kotlinx.coroutines.delay
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import ru.gildor.coroutines.okhttp.await

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

        // 2: send /fleets/access_tokens request. receive access token and refresh token
        // The access token is used with the REST API to fetch data
        val response = ClearVehicleResponse(vin, ClearVehicleResponse.Status.PENDING)

        val extraDelay = (1000L * Math.random()).toLong()
        delay(300L + extraDelay)
        logger.debug("webService response: $vin")

        // 3. return request response. otherwise user can poll the status
        return response
    }

    private suspend fun getAuthToken(): String {
        // TODO: first check if auth token exists, then use that instead.

        // 1. create jwt auth token
        val jwtMap = mapOf(
            "ver" to configuration.version,
            "iss" to configuration.apiKey,
            "aud" to configuration.baseUrl,
            "iat" to System.currentTimeMillis() / 1000
        )

        val privateKey = configuration.getHmPrivateKey()

        println("priv key bytes $privateKey")

        val jwtContent = Base64.encode(jwtMap.toString().toByteArray())
        val signature = hmkitOem.crypto.signJWT(jwtContent.toByteArray(), privateKey)

//        val jwt = String.format("%s.%s", jwtContent, signature.base64UrlSafe)

        // 2. make the auth tokes request
        // {base}/auth_tokens
        val body = FormBody.Builder()
            .add("assertion", signature.base64UrlSafe)
            .build()

        val request = Request.Builder()
            .url("${configuration.baseUrl}/auth_tokens")
            .header("Content-Type", "application/json")
            .post(body)
            .build()
        val call = client.newCall(request)

        val response = call.await()

        return response.body.toString()
    }
}