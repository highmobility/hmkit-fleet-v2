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
        assertTrue(jsonBody["serial_number"]!!.jsonPrimitive.contentOrNull == mockSerial.hex)
        assertTrue(jsonBody["access_token"]!!.jsonPrimitive.contentOrNull == newAccessToken.accessToken)
        assertTrue(jsonBody["signature"]!!.jsonPrimitive.contentOrNull == mockSignature.base64)

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