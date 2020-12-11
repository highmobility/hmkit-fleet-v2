package network

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import model.AccessToken
import model.Brand
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
    baseUrl: String,
    private val authTokenRequests: AuthTokenRequests
) : Requests(
    client,
    logger, baseUrl
) {
    suspend fun getAccessToken(
        vin: String,
        brand: Brand
    ): Response<AccessToken> {
        val authToken = authTokenRequests.getAuthToken()

        if (authToken.response == null) return Response(null, authToken.error)

        val request = Request.Builder()
            .url("${baseUrl}/fleets/access_tokens")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${authToken.response.authToken}")
            .post(getTokenBody(vin, brand))
            .build()

        printRequest(request)

        val call = client.newCall(request)
        val response = call.await()

        return tryParseResponse(response, HttpURLConnection.HTTP_OK) { body ->
            val format = Json { ignoreUnknownKeys = true }
            val accessToken = format.decodeFromString<AccessToken>(body)
            Response(accessToken, null)
        }
    }

    suspend fun deleteAccessToken(accessToken: AccessToken): Response<Boolean> {
        val request = Request.Builder()
            .url("${baseUrl}/fleets/access_tokens")
            .header("Content-Type", "application/json")
            .post(deleteTokenBody(accessToken))
            .build()

        printRequest(request)

        val call = client.newCall(request)
        val response = call.await()

        return tryParseResponse(response, HttpURLConnection.HTTP_OK) {
            Response(true, null)
        }
    }

    private fun deleteTokenBody(accessToken: AccessToken): RequestBody {
        val completeBody = buildJsonObject {
            TODO()
        }

        val body = completeBody.toString().toRequestBody(mediaType)
        return body
    }

    private fun getTokenBody(vin: String, brand: Brand): RequestBody {
        val completeBody = buildJsonObject {
            put("vin", vin)
            put("oem", Json.encodeToJsonElement(brand))
        }

        val body = completeBody.toString().toRequestBody(mediaType)
        return body
    }
}