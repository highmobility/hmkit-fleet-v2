package network

import com.highmobility.hmkit.HMKit
import kotlinx.serialization.json.*
import model.AuthToken
import network.response.ClearanceStatus
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
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
    ): Response<ClearanceStatus> {
        val body = requestBody(vin)

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
                val statuses = jsonElement["vins"] as JsonArray
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
            val detail = e.message.toString()
            return Response(null, genericError(detail))
        }
    }

    suspend fun getClearanceStatuses(
        authToken: AuthToken,
    ): Response<List<ClearanceStatus>> {
        // fleets/vehicles endpoint
        // GET /v1/fleets/vehicles
        // auth header: token

        val request = Request.Builder()
            .url("${baseUrl}/fleets/vehicles")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${authToken.authToken}")
            .build()

        printRequest(request)

        val call = client.newCall(request)
        val response = call.await()
        val responseBody = printResponse(response)

        try {
            return if (response.code == HttpURLConnection.HTTP_OK) {
                val statuses = Json.parseToJsonElement(responseBody) as JsonArray

                val builder = Array(statuses.size) {
                    val statusElement = statuses[it]
                    val status = Json.decodeFromJsonElement<ClearanceStatus>(statusElement)
                    status
                }

                Response(builder.toList())
            } else {
                parseError(responseBody)
            }
        } catch (e: Exception) {
            val detail = e.message.toString()
            return Response(null, genericError(detail))
        }
    }

    private fun requestBody(vin: String): RequestBody {
        val vehicle = JsonObject(mapOf("vin" to JsonPrimitive(vin)))
        val arrayOfVehicles = JsonArray(listOf(vehicle))
        val completeBody = JsonObject(mapOf("vehicles" to arrayOfVehicles))

        val body = completeBody.toString().toRequestBody(mediaType)
        return body
    }
}