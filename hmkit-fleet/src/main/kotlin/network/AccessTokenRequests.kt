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
            .post(requestBody(vin, brand))
            .build()

        printRequest(request)

        val call = client.newCall(request)
        val response = call.await()

        return tryParseResponse(response, HttpURLConnection.HTTP_OK) { body ->
            val accessToken = Json.decodeFromString<AccessToken>(body)
            Response(accessToken, null)
        }
    }

    private fun requestBody(vin: String, brand: Brand): RequestBody {
        val completeBody = buildJsonObject {
            put("vin", vin)
            put("brand", Json.encodeToJsonElement(brand))
        }

        val body = completeBody.toString().toRequestBody(mediaType)
        return body
    }
}