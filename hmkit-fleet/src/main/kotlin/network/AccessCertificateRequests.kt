package network

import ClientCertificate
import com.highmobility.crypto.AccessCertificate
import com.highmobility.crypto.Crypto
import com.highmobility.crypto.value.PrivateKey
import com.highmobility.value.Bytes
import kotlinx.serialization.json.*
import model.AccessToken
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.Logger
import ru.gildor.coroutines.okhttp.await
import java.net.HttpURLConnection

internal class AccessCertificateRequests(
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
    val apiDeviceCertKey = "device_access_certificate"

    suspend fun getAccessCertificate(
        accessToken: AccessToken,
    ): Response<AccessCertificate> {
        val request = Request.Builder()
            .url("${baseUrl}/access_certificates")
            .header("Content-Type", "application/json")
            .post(requestBody(accessToken, certificate, privateKey, crypto))
            .build()

        printRequest(request)

        val call = client.newCall(request)
        val response = call.await()

        return tryParseResponse(response, HttpURLConnection.HTTP_OK) { body ->
            val jsonResponse = Json.parseToJsonElement(body) as JsonObject
            val certBytes = jsonResponse.jsonObject[apiDeviceCertKey]?.jsonPrimitive?.content
            val cert = AccessCertificate(certBytes)
            Response(cert, null)
        }
    }

    private fun requestBody(
        accessToken: AccessToken,
        certificate: ClientCertificate,
        privateKey: PrivateKey,
        crypto: Crypto
    ): RequestBody {
        val accessTokenBytes = Bytes(accessToken.accessToken.encodeToByteArray())
        val signature = crypto.sign(accessTokenBytes, privateKey).base64

        val completeBody = buildJsonObject {
            put("serial_number", certificate.serial.hex)
            put("access_token", accessToken.accessToken)
            put("signature", signature)
        }

        return completeBody.toString().toRequestBody(mediaType)
    }
}