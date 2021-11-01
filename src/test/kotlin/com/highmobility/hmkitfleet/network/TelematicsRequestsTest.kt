/*
 * The MIT License
 *
 * Copyright (c) 2014- High-Mobility GmbH (https://high-mobility.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.highmobility.hmkitfleet.network

import com.highmobility.hmkitfleet.BaseTest

import com.highmobility.autoapi.Diagnostics
import com.highmobility.autoapi.property.Property
import com.highmobility.autoapi.value.measurement.Length
import com.highmobility.crypto.Crypto
import com.highmobility.crypto.value.DeviceSerial
import com.highmobility.crypto.value.Issuer
import com.highmobility.crypto.value.PrivateKey
import com.highmobility.hmkitfleet.ClientCertificate
import com.highmobility.value.Bytes
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import com.highmobility.hmkitfleet.mockAccessCert
import com.highmobility.hmkitfleet.mockSignature
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

    private val crypto = mockk<Crypto> {
        // random encryption ok here
        every {
            createTelematicsContainer(
                Diagnostics.GetState(),
                privateKey,
                certificate.serial,
                mockAccessCert,
                nonce
            )
        } returns encryptedSentCommand

        every {
            getPayloadFromTelematicsContainer(
                Bytes(encryptedReceivedCommand),
                privateKey,
                mockAccessCert
            )
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
            TelematicsRequests(
                client,
                mockLogger,
                baseUrl.toString(),
                privateKey,
                certificate,
                crypto
            )

        val response = runBlocking {
            telematicsRequests.sendCommand(Diagnostics.GetState(), mockAccessCert)
        }

        verify {
            crypto.createTelematicsContainer(
                Diagnostics.GetState(),
                privateKey,
                certificate.serial,
                mockAccessCert,
                nonce
            )
        }

        // first request is nonce
        val nonceRequest: RecordedRequest = mockWebServer.takeRequest()
        assert(nonceRequest.path!!.endsWith("/nonces"))

        // verify request
        val nonceRequestBody = Json.parseToJsonElement(nonceRequest.body.readUtf8()) as JsonObject
        assert(nonceRequestBody["serial_number"]!!.jsonPrimitive.contentOrNull == certificate.serial.hex)

        // second request is telematics command
        val commandRequest: RecordedRequest = mockWebServer.takeRequest()
        assert(commandRequest.path!!.endsWith("/telematics_commands"))

        // verify request
        val jsonBody = Json.parseToJsonElement(commandRequest.body.readUtf8()) as JsonObject
        assert(jsonBody["serial_number"]!!.jsonPrimitive.contentOrNull == certificate.serial.hex)
        assert(jsonBody["issuer"]!!.jsonPrimitive.contentOrNull == certificate.issuer.hex)
        assert(jsonBody["data"]!!.jsonPrimitive.contentOrNull == encryptedSentCommand.base64)

        // verify command decrypted
        verify {
            crypto.getPayloadFromTelematicsContainer(
                encryptedReceivedCommand,
                privateKey,
                mockAccessCert
            )
        }

        // verify final telematics command response
        assert(response.response!! == decryptedReceivedCommand)
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
            .setResponseCode(HttpURLConnection.HTTP_CREATED)
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
                TelematicsRequests(client, mockLogger, mockUrl, privateKey, certificate, crypto)
            webService.sendCommand(Diagnostics.GetState(), mockAccessCert)
        }
    }

    @Test
    fun nonceUnknownResponse() = runBlocking {
        testForUnknownResponseGenericErrorReturned(mockWebServer) { mockUrl ->
            val webService =
                TelematicsRequests(client, mockLogger, mockUrl, privateKey, certificate, crypto)
            webService.sendCommand(Diagnostics.GetState(), mockAccessCert)
        }
    }

    @Test
    fun telematicsCommandErrorResponse() = runBlocking {
        mockNonceResponse()
        testErrorResponseReturned(mockWebServer) { mockUrl ->
            val webService =
                TelematicsRequests(client, mockLogger, mockUrl, privateKey, certificate, crypto)
            webService.sendCommand(Diagnostics.GetState(), mockAccessCert)
        }
    }

    @Test
    fun telematicsCommandUnknownResponse() = runBlocking {
        mockNonceResponse()
        testForUnknownResponseGenericErrorReturned(mockWebServer) { mockUrl ->
            val webService =
                TelematicsRequests(client, mockLogger, mockUrl, privateKey, certificate, crypto)
            webService.sendCommand(Diagnostics.GetState(), mockAccessCert)
        }
    }
}