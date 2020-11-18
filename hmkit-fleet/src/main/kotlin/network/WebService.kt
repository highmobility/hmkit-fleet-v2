package network

import HMKitFleet
import ServiceAccountApiConfiguration
import com.highmobility.hmkit.HMKit
import com.highmobility.utils.Base64
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import model.AuthToken
import network.response.ClearVehicle
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import org.slf4j.Logger
import ru.gildor.coroutines.okhttp.await
import java.lang.Exception
import java.net.HttpURLConnection

internal class WebService(
    val client: OkHttpClient,
    val hmkitOem: HMKit,
    val logger: Logger,
    val baseUrl: String
) {
    suspend fun getAuthToken(configuration: ServiceAccountApiConfiguration): network.Response<AuthToken> {
        val body = FormBody.Builder()
            .add("assertion", getJwt(configuration))
            .build()

        val request = Request.Builder()
            .url("${baseUrl}/auth_tokens")
            .header("Content-Type", "application/json")
            .post(body)
            .build()
        printRequest(request)
        val call = client.newCall(request)

        val response = call.await()
        val responseBody = printResponse(response)

        try {
            if (response.code == HttpURLConnection.HTTP_CREATED) {
                val token = Json.decodeFromString<AuthToken>(responseBody)
                return Response(token)
            } else {
                val json = Json.parseToJsonElement(responseBody)
                if (json is JsonObject) {
                    val errors = json["errors"] as JsonArray
                    if (errors.size > 0) {
                        val error =
                            Json.decodeFromJsonElement<network.response.Error>(errors.first())
                        return Response(null, error)
                    }
                }

                return Response(null, genericError("invalid error structure"))
            }
        } catch (e: Exception) {
            val detail = e.message.toString()
            return Response(null, genericError(detail))
        }
    }

    suspend fun clearVehicle(vin: String, authToken: AuthToken): network.Response<ClearVehicle> {
        // fleets/vehicles endpoint
        // post /v1/fleets/vehicles
        // auth header: token

        // 2: send /fleets/access_tokens request. receive access token and refresh token
        // The access token is used with the REST API to fetch data
        val response = ClearVehicle(vin, ClearVehicle.Status.PENDING)

        // 3. return request response.  user can poll the status
        // TODO: 17/11/20 parse error or object
        return network.Response(response)
    }

    fun genericError(detail: String): network.response.Error {
        val genericError = network.response.Error("Invalid server response", detail)
        return genericError
    }

    private fun getJwt(configuration: ServiceAccountApiConfiguration): String {
        val header = buildJsonObject {
            put("alg", "ES256")
            put("typ", "JWT")
        }.toString()

        val jwtBody = buildJsonObject {
            put("ver", configuration.version)
            put("iss", configuration.apiKey)
            put("aud", HMKitFleet.Environment.PRODUCTION.url)
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

    private fun printResponse(response: Response): String {
        val body = response.body?.string()
        logger.debug("${response.request.url} response:\n${response.code}: ${body}")
        return body!!
    }
}