package network

import ServiceAccountApiConfiguration
import com.highmobility.hmkit.HMKit
import com.highmobility.utils.Base64
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import model.Database
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import org.slf4j.Logger
import ru.gildor.coroutines.okhttp.await

class WebService(
    val configuration: ServiceAccountApiConfiguration,
    val client: OkHttpClient,
    val hmkitOem: HMKit,
    val database: Database,
    val logger: Logger
) {
    suspend fun clearVehicle(vin: String): ClearVehicleResponse {
        // fleets/vehicles endpoint
        // post /v1/fleets/vehicles
        // auth header: token

        // flow:
        // 1) create jwt with apiKey and exchange it to auth token
        val authToken = getAuthToken() // TODO: implement error handling
        // {"errors":[{"title":"Internal server error"}]}

        // 2: send /fleets/access_tokens request. receive access token and refresh token
        // The access token is used with the REST API to fetch data
        val response = ClearVehicleResponse(vin, ClearVehicleResponse.Status.PENDING)

        // 3. return request response.  user can poll the status
        return response
    }

    private suspend fun getAuthToken(): String {
        // TODO: first check if auth token exists, then use that instead.

        // 1. create/read jwt auth token

        // 2. make the auth tokes request
        // {base}/auth_tokens
        val body = FormBody.Builder()
            .add("assertion", getJwt())
            .build()

        val request = Request.Builder()
            .url("${configuration.baseUrl}/auth_tokens")
            .header("Content-Type", "application/json")
            .post(body)
            .build()
        printRequest(request)
        val call = client.newCall(request)

        val response = call.await()
        printResponse(response)

        return response.body.toString()
    }

    private fun getJwt(): String {
        val header = buildJsonObject {
            put("alg", "ES256")
            put("typ", "JWT")
        }.toString()

        val jwtBody = buildJsonObject {
            put("ver", configuration.version)
            put("iss", configuration.apiKey)
            put("aud", ServiceAccountApiConfiguration.Environment.PRODUCTION.url)
            put("jti", configuration.createJti())
            put("iat", configuration.createIat())
        }.toString()

        val headerBase64 = Base64.encodeUrlSafe(header.toByteArray())
        val bodyBase64 = Base64.encodeUrlSafe(jwtBody.toByteArray())
        val jwtContent = String.format("%s.%s", headerBase64, bodyBase64)
        val privateKey = configuration.getHmPrivateKey()
        val jwtSignature = hmkitOem.crypto.signJWT(jwtContent.toByteArray(), privateKey)

        return String.format("%s.%s", jwtContent, jwtSignature.base64UrlSafe)
    }

    private fun printRequest(request: Request) {
        val buffer = Buffer()
        request.body?.writeTo(buffer)
        logger.debug(
            "sending ${request.url}:" +
                    "\nheaders: ${request.headers}" +
                    "body: ${(buffer.readUtf8())}"
        )
    }

    private fun printResponse(response: Response) {
        logger.debug("${response.request.url} response:\n${response.code}: ${response.body?.string()}")
    }
}