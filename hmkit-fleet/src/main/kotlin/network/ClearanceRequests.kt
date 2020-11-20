package network

import com.highmobility.hmkit.HMKit
import kotlinx.serialization.json.*
import model.AuthToken
import network.response.ClearanceStatus
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.Logger
import ru.gildor.coroutines.okhttp.await
import java.net.HttpURLConnection

internal class ClearanceRequests(
    client: OkHttpClient,
    hmkitOem: HMKit,
    logger: Logger,
    baseUrl: String
) : Requests(
    client, hmkitOem,
    logger, baseUrl
) {
    suspend fun requestClearance(
        authToken: AuthToken,
        vin: String
    ): network.Response<ClearanceStatus> {
        // fleets/vehicles endpoint
        // post /v1/fleets/vehicles
        // auth header: token
        val vehicle = JsonObject(mapOf("vin" to JsonPrimitive(vin)))
        val arrayOfVehicles = JsonArray(listOf(vehicle))
        val completeBody = JsonObject(mapOf("vehicles" to arrayOfVehicles))

        val body = completeBody.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("${baseUrl}/fleets/vehicles")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${authToken.authToken}")
            .post(body)
            .build()
        printRequest(request)
        val call = client.newCall(request)

        val response = call.await()
        val responseBody = printResponse(response)

        try {
            if (response.code == HttpURLConnection.HTTP_OK) {
                val jsonElement = Json.parseToJsonElement(responseBody) as JsonObject
                val statuses =  jsonElement["vins"] as JsonArray
                for (statusElement in statuses) {
                    val status = Json.decodeFromJsonElement<ClearanceStatus>(statusElement)
                    if (status.vin == vin) {
                        return Response(status, null)
                    }
                }
                return Response(null, genericError())
            } else {
                return parseError(responseBody)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val detail = e.message.toString()
            return Response(null, genericError(detail))
        }
    }

    suspend fun getClearanceStatus(
        authToken: AuthToken,
        vin: String
    ): network.Response<ClearanceStatus> {
        // fleets/vehicles endpoint
        // post /v1/fleets/vehicles
        // auth header: token

        // 2: send /fleets/access_tokens request. receive access token and refresh token
        // The access token is used with the REST API to fetch data
        val response = ClearanceStatus(vin, ClearanceStatus.Status.PENDING)

        // 3. return request response.  user can poll the status
        // TODO: 17/11/20 parse error or object
        return network.Response(response)
    }

}