package network

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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

        val request = Request.Builder()
            .url("${baseUrl}/fleets/access_tokens")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${authToken.authToken}")
            .post(requestBody(vin, oem))
            .build()

        printRequest(request)

        val call = client.newCall(request)
        val response = call.await()

        return tryParseResponse(response, HttpURLConnection.HTTP_OK) { body ->
            val accessToken = Json.decodeFromString<AccessToken>(body)
            Response(accessToken, null)
        }
    }

    private fun requestBody(vin: String, oem: String): RequestBody {
        val completeBody =
            JsonObject(
                mapOf(
                    "vin" to JsonPrimitive(vin),
                    "oem" to JsonPrimitive(oem) // TODO: will be removed
                )
            )

        val body = completeBody.toString().toRequestBody(mediaType)
        return body
    }
}