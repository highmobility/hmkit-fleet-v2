package network

import BaseTest
import ClientCertificate
import com.highmobility.crypto.Crypto
import com.highmobility.crypto.value.PrivateKey
import com.highmobility.value.Bytes
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import mockAccessCert
import mockSerial
import mockSignature
import newAccessToken
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection

internal class AccessCertificateRequestsTest : BaseTest() {
    val mockWebServer = MockWebServer()
    val client = OkHttpClient()

    val privateKey = mockk<PrivateKey>()
    val crypto = mockk<Crypto> {
        every { sign(any<Bytes>(), any()) } returns mockSignature
    }
    val clientCertificate = mockk<ClientCertificate> {
        every { serial } returns mockSerial
    }

    @BeforeEach
    fun setUp() {
        mockWebServer.start()
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun getAccessCertificate() {
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_CREATED)
            .setBody(
                """
                    {
                        "device_access_certificate": "${mockAccessCert.base64}",
                        "vehicle_access_certificate": null
                    }
                """.trimIndent()
            )

        mockWebServer.enqueue(mockResponse)
        val accessCertificateRequests = getRequests(mockWebServer.url("").toString())

        val response = runBlocking {
            accessCertificateRequests.getAccessCertificate(newAccessToken)
        }

        val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
        assertTrue(recordedRequest.path!!.endsWith("/access_certificates"))

        // verify request
        val jsonBody = Json.parseToJsonElement(recordedRequest.body.readUtf8()) as JsonObject
        assertTrue(jsonBody["serial_number"]!!.jsonPrimitive?.contentOrNull == mockSerial.hex)
        assertTrue(jsonBody["access_token"]!!.jsonPrimitive?.contentOrNull == newAccessToken.accessToken)
        assertTrue(jsonBody["signature"]!!.jsonPrimitive?.contentOrNull == mockSignature.base64)

        // verify response
        assertTrue(response.response?.hex == mockAccessCert.hex)
    }

    // error responses

    @Test
    fun getAccessCertificateErrorResponse() = runBlocking {
        testErrorResponseReturned(mockWebServer) { mockUrl ->
            getRequests(mockUrl).getAccessCertificate(newAccessToken)
        }
    }

    @Test
    fun getAccessCertificateUnknownResponse() = runBlocking {
        testForUnknownResponseGenericErrorReturned(mockWebServer) { mockUrl ->
            getRequests(mockUrl).getAccessCertificate(newAccessToken)
        }
    }

    fun getRequests(baseUrl: String): AccessCertificateRequests {
        return AccessCertificateRequests(
            client,
            mockLogger,
            baseUrl,
            privateKey,
            clientCertificate,
            crypto
        )
    }
}