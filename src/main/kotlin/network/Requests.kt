package network

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import org.slf4j.Logger

internal open class Requests(
    val client: OkHttpClient,
    val logger: Logger,
    val baseUrl: String
) {
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val baseHeaders = Headers.Builder().add("Content-Type", "application/json").build()

    inline fun <T> tryParseResponse(
        response: Response,
        expectedResponseCode: Int,
        block: (body: String) -> (network.Response<T>)
    ): network.Response<T> {
        val responseBody = printResponse(response)

        return try {
            if (response.code == expectedResponseCode) {
                block(responseBody)
            } else {
                parseError(responseBody)
            }
        } catch (e: Exception) {
            val detail = e.message.toString()
            Response(null, genericError(detail))
        }
    }

    fun printRequest(request: Request) {
        val format = Json { prettyPrint = true }

        // parse into json, so can log it out with pretty print
        val body = request.bodyAsString()
        var bodyInPrettyPrint = ""
        if (body != null) {
            val jsonElement = format.decodeFromString<JsonElement>(body)
            bodyInPrettyPrint = format.encodeToString(jsonElement)
        }

        logger.debug(
            "sending ${request.method} ${request.url}:" +
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
            val errors = json["errors"] as? JsonArray
            if (errors != null && errors.size > 0) {
                val error =
                    Json.decodeFromJsonElement<Error>(errors.first())
                return Response(null, error)
            }
        }

        return Response(null, genericError("Unknown server response"))
    }

    internal fun requestBody(values: Map<String, String>): RequestBody {
        val completeBody = buildJsonObject {
            for (item in values) {
                put(item.key, item.value)
            }
        }

        return completeBody.toString().toRequestBody(mediaType)
    }
}

internal fun Request.bodyAsString(): String? {
    if (this.body == null) return null
    val buffer = Buffer()
    this.body?.writeTo(buffer)
    return buffer.readUtf8()
}

internal fun genericError(detail: String? = null): Error {
    val genericError = Error("Invalid server response", detail)
    return genericError
}