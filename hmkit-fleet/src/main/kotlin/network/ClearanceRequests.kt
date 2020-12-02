package network

import kotlinx.serialization.json.*
import model.Brand
import model.ControlMeasure
import model.ClearanceStatus
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.Logger
import ru.gildor.coroutines.okhttp.await
import java.net.HttpURLConnection

internal class ClearanceRequests(
    client: OkHttpClient,
    logger: Logger,
    baseUrl: String,
    private val authTokenRequests: AuthTokenRequests
) : Requests(
    client,
    logger, baseUrl
) {
    suspend fun requestClearance(
        vin: String,
        brand: Brand,
        controlMeasures: List<ControlMeasure>?
    ): Response<ClearanceStatus> {
        val body = requestBody(vin, brand, controlMeasures)
        val authToken = authTokenRequests.getAuthToken()

        if (authToken.error != null) return Response(null, authToken.error)

        val request = Request.Builder()
            .url("${baseUrl}/fleets/vehicles")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $authToken")
            .post(body)
            .build()

        printRequest(request)

        val call = client.newCall(request)
        val response = call.await()

        return tryParseResponse(response, HttpURLConnection.HTTP_OK) { responseBody ->
            val jsonElement = Json.parseToJsonElement(responseBody) as JsonObject
            val statuses = jsonElement["vins"] as JsonArray
            for (statusElement in statuses) {
                val status =
                    Json.decodeFromJsonElement<ClearanceStatus>(statusElement)
                if (status.vin == vin) {
                    return Response(status, null)
                }
            }
            Response(null, genericError())
        }
    }

    suspend fun getClearanceStatuses(): Response<List<ClearanceStatus>> {
        // GET /v1/fleets/vehicles
        val authToken = authTokenRequests.getAuthToken()

        if (authToken.error != null) return Response(null, authToken.error)

        val request = Request.Builder()
            .url("${baseUrl}/fleets/vehicles")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $authToken")
            .build()

        printRequest(request)

        val call = client.newCall(request)
        val response = call.await()

        return tryParseResponse(response, HttpURLConnection.HTTP_OK) { responseBody ->
            val statuses = Json.parseToJsonElement(responseBody) as JsonArray

            val builder = Array(statuses.size) {
                val statusElement = statuses[it]
                val status = Json.decodeFromJsonElement<ClearanceStatus>(statusElement)
                status
            }

            Response(builder.toList())
        }
    }

    private fun requestBody(
        vin: String,
        brand: Brand,
        controlMeasures: List<ControlMeasure>?
    ): RequestBody {
        val vehicle = buildJsonObject {
            put("vin", vin)
            put("brand", Json.encodeToJsonElement(brand))
            if (controlMeasures != null) {
                putJsonObject("control_measures") {
                    for (controlMeasure in controlMeasures) {
                        // polymorphism adds type key to child controlmeasure classes. remove with filter
                        val json = Json.encodeToJsonElement(controlMeasure)
                        val valuesWithoutType = json.jsonObject.filterNot { it.key == "type" }
                        val jsonTrimmed = Json.encodeToJsonElement(valuesWithoutType)
                        put("odometer", jsonTrimmed)
                    }
                }
            }
        }

        val arrayOfVehicles = JsonArray(listOf(vehicle))
        val completeBody = JsonObject(mapOf("vehicles" to arrayOfVehicles))

        val body = completeBody.toString().toRequestBody(mediaType)
        return body
    }
}