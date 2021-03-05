package network

import ClientCertificate
import com.highmobility.crypto.AccessCertificate
import com.highmobility.crypto.Crypto
import com.highmobility.crypto.value.PrivateKey
import com.highmobility.value.Bytes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import ru.gildor.coroutines.okhttp.await
import java.net.HttpURLConnection

internal class TelematicsRequests(
    client: OkHttpClient,
    logger: Logger,
    baseUrl: String,
    private val privateKey: PrivateKey,
    private val certificate: ClientCertificate,
    private val crypto: Crypto
) : Requests(
    client,
    logger, baseUrl
) {
    suspend fun sendCommand(
        command: Bytes,
        accessCertificate: AccessCertificate
    ): Response<Bytes> {
        val nonce = getNonce()

        if (nonce.error != null) return Response(null, nonce.error)

        val encryptedCommand =
            crypto.createTelematicsContainer(
                command,
                privateKey,
                certificate.serial,
                accessCertificate,
                Bytes(nonce.response!!)
            )

        val encryptedCommandResponse = postCommand(encryptedCommand, accessCertificate)

        if (encryptedCommandResponse.error != null) return encryptedCommandResponse

        val decryptedResponseCommand = crypto.getPayloadFromTelematicsContainer(
            encryptedCommandResponse.response!!,
            privateKey,
            accessCertificate,
        )

        return Response(decryptedResponseCommand)
    }

    private suspend fun getNonce(): Response<String> {
        val request = Request.Builder()
            .url("${baseUrl}/nonces")
            .headers(baseHeaders)
            .post(
                requestBody(
                    mapOf(
                        "serial_number" to certificate.serial.hex
                    )
                )
            )
            .build()

        printRequest(request)

        val call = client.newCall(request)
        val response = call.await()

        return tryParseResponse(response, HttpURLConnection.HTTP_CREATED) { body ->
            val jsonResponse = Json.parseToJsonElement(body) as JsonObject
            val nonce = jsonResponse.jsonObject["nonce"]?.jsonPrimitive?.content
            Response(nonce, null)
        }
    }

    private suspend fun postCommand(
        encryptedCommand: Bytes,
        accessCertificate: AccessCertificate,
    ): Response<Bytes> {
        val request = Request.Builder()
            .url("${baseUrl}/telematics_commands")
            .headers(baseHeaders)
            .post(
                requestBody(
                    mapOf(
                        "serial_number" to accessCertificate.gainerSerial.hex,
                        "issuer" to certificate.issuer.hex,
                        "data" to encryptedCommand.base64
                    )
                )
            )
            .build()

        printRequest(request)

        val call = client.newCall(request)
        val response = call.await()

        return tryParseResponse(response, HttpURLConnection.HTTP_OK) { body ->
            val jsonResponse = Json.parseToJsonElement(body) as JsonObject
            val encryptedResponseCommand =
                jsonResponse.jsonObject["response_data"]?.jsonPrimitive?.content
            Response(Bytes(encryptedResponseCommand), null)
        }
    }
}