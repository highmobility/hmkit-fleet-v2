package network

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import model.AuthToken
import network.response.AccessToken
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.Logger
import ru.gildor.coroutines.okhttp.await
import java.net.HttpURLConnection

internal class AccessTokenRequests(
    client: OkHttpClient,
    logger: Logger,
    baseUrl: String
) : Requests(
    client,
    logger, baseUrl
) {
    suspend fun createAccessToken(
        authToken: AuthToken,
        vin: String,
        oem: String
    ): Response<AccessToken> {
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

        return tryParseResponse(response, HttpURLConnection.HTTP_OK) { body ->
            val accessToken = Json.decodeFromString<AccessToken>(body)
            Response(accessToken, null)
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