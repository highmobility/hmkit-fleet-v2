package network

import BaseTest
import ClientCertificate
import com.highmobility.autoapi.Diagnostics
import com.highmobility.autoapi.property.Property
import com.highmobility.autoapi.value.measurement.Length
import com.highmobility.crypto.value.DeviceSerial
import com.highmobility.crypto.value.Issuer
import com.highmobility.crypto.value.PrivateKey
import com.highmobility.hmkit.HMKit
import com.highmobility.value.Bytes
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import mockAccessCert
import mockSerial
import mockSignature
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection

internal class TelematicsRequestsTest : BaseTest() {
    private val mockWebServer = MockWebServer()
    private val client = OkHttpClient()

    private val privateKey = mockk<PrivateKey> {
        every { base64 } returns mockSignature.base64
    }
    private val nonce = Bytes("AAAAAAAAAAAAAAAAAA")
    private val encryptedSentCommand = nonce
    private val encryptedReceivedCommand = nonce

    private val decryptedReceivedCommand = Diagnostics.State.Builder()
        .setOdometer(Property(Length(11000.0, Length.Unit.KILOMETERS))).build()

    private val certificate = mockk<ClientCertificate> {
        every { serial } returns DeviceSerial(nonce)
        every { issuer } returns Issuer("00000000")
    }

    private val oem = mockk<HMKit> {
        // random encryption ok here
        every {
            encrypt(
                privateKey,
                mockAccessCert,
                nonce,
                certificate.serial,
                Diagnostics.GetState()
            )
        } returns encryptedSentCommand

        every {
            decrypt(privateKey, mockAccessCert, Bytes(encryptedReceivedCommand))
        } returns decryptedReceivedCommand
    }

    @BeforeEach
    fun setUp() {
        mockWebServer.start()
        every { certificate.serial.hex } returns "AAAAAAAAAAAAAAAAAA" // not sure why this needs to be defined for a real object
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun createAccessToken() {
        mockNonceResponse()
        mockTelematicsResponse()

        val baseUrl: HttpUrl = mockWebServer.url("")

        val telematicsRequests =
            TelematicsRequests(client, mockLogger, baseUrl.toString(), privateKey, certificate, oem)

        val response = runBlocking {
            telematicsRequests.sendCommand(Diagnostics.GetState(), mockAccessCert)
        }

        verify {
            oem.encrypt(
                privateKey,
                mockAccessCert,
                nonce,
                certificate.serial,
                Diagnostics.GetState()
            )
        }

        // first request is nonce
        val nonceRequest: RecordedRequest = mockWebServer.takeRequest()
        assertTrue(nonceRequest.path!!.endsWith("/nonces"))

        // verify request
        val nonceRequestBody = Json.parseToJsonElement(nonceRequest.body.readUtf8()) as JsonObject
        assertTrue(nonceRequestBody["serial_number"]!!.jsonPrimitive?.contentOrNull == certificate.serial.hex)

        // second request is telematics command
        val commandRequest: RecordedRequest = mockWebServer.takeRequest()
        assertTrue(commandRequest.path!!.endsWith("/telematics_commands"))

        // verify request
        val jsonBody = Json.parseToJsonElement(commandRequest.body.readUtf8()) as JsonObject
        assertTrue(jsonBody["serial_number"]!!.jsonPrimitive?.contentOrNull == certificate.serial.hex)
        assertTrue(jsonBody["issuer"]!!.jsonPrimitive?.contentOrNull == certificate.issuer.hex)
        assertTrue(jsonBody["data"]!!.jsonPrimitive?.contentOrNull == encryptedSentCommand.base64)

        // verify command decrypted
        verify { oem.decrypt(privateKey, mockAccessCert, encryptedReceivedCommand) }

        // verify final telematics command response
        assertTrue(response.response!! == decryptedReceivedCommand)
    }

    private fun mockTelematicsResponse() {
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(
                """
                    {
                      "status": "ok",
                      "response_data": "${encryptedReceivedCommand.base64}",
                      "message": "Response received"
                    }
                """.trimIndent()
            )
        mockWebServer.enqueue(mockResponse)
    }

    private fun mockNonceResponse() {
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(
                """
                    {
                        "nonce": "${nonce.base64}"
                    }
                """.trimIndent()
            )
        mockWebServer.enqueue(mockResponse)
    }

    // error responses

    @Test
    fun nonceErrorResponse() = runBlocking {
        testErrorResponseReturned(mockWebServer) { mockUrl ->
            val webService =
                TelematicsRequests(client, mockLogger, mockUrl, privateKey, certificate, oem)
            webService.sendCommand(Diagnostics.GetState(), mockAccessCert)
        }
    }

    @Test
    fun nonceUnknownResponse() = runBlocking {
        testForUnknownResponseGenericErrorReturned(mockWebServer) { mockUrl ->
            val webService =
                TelematicsRequests(client, mockLogger, mockUrl, privateKey, certificate, oem)
            webService.sendCommand(Diagnostics.GetState(), mockAccessCert)
        }
    }

    @Test
    fun telematicsCommandErrorResponse() = runBlocking {
        mockNonceResponse()
        testErrorResponseReturned(mockWebServer) { mockUrl ->
            val webService =
                TelematicsRequests(client, mockLogger, mockUrl, privateKey, certificate, oem)
            webService.sendCommand(Diagnostics.GetState(), mockAccessCert)
        }
    }

    @Test
    fun telematicsCommandUnknownResponse() = runBlocking {
        mockNonceResponse()
        testForUnknownResponseGenericErrorReturned(mockWebServer) { mockUrl ->
            val webService =
                TelematicsRequests(client, mockLogger, mockUrl, privateKey, certificate, oem)
            webService.sendCommand(Diagnostics.GetState(), mockAccessCert)
        }
    }
}