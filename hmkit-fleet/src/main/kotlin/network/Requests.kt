package network

import com.highmobility.hmkit.HMKit
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import org.slf4j.Logger

internal open class Requests(
    val client: OkHttpClient,
    val hmkitOem: HMKit,
    val logger: Logger,
    val baseUrl: String
) {
    val mediaType = "application/json; charset=utf-8".toMediaType()

    fun genericError(detail: String? = null): network.response.Error {
        val genericError = network.response.Error("Invalid server response", detail)
        return genericError
    }

    fun printRequest(request: Request) {
        val format = Json { prettyPrint = true }

        // parse into json, so can log it out with pretty print
        val jsonElement = format.decodeFromString<JsonElement>(request.asString())
        val bodyInPrettyPrint = format.encodeToString(jsonElement)

        logger.debug(
            "sending ${request.url}:" +
                    "\nheaders: ${request.headers}" +
                    "body: $bodyInPrettyPrint"
        )
    }

    fun printResponse(response: Response): String {
        val body = response.body?.string()
        logger.debug("${response.request.url} response:\n${response.code}: ${body}")
        return body!!
    }

    fun <T> parseError(responseBody: String): network.Response<T> {
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
}

fun Request.asString(): String {
    val buffer = Buffer()
    this.body?.writeTo(buffer)
    return buffer.readUtf8()
}