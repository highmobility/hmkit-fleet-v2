/*
 * The MIT License
 *
 * Copyright (c) 2023- High-Mobility GmbH (https://high-mobility.com)
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
package com.highmobility.hmkitfleet

import com.highmobility.hmkitfleet.model.Brand
import com.highmobility.hmkitfleet.model.ClearanceStatus
import com.highmobility.hmkitfleet.model.RequestClearanceResponse
import com.highmobility.hmkitfleet.network.ClearanceRequests
import com.highmobility.hmkitfleet.network.Response
import com.highmobility.hmkitfleet.network.VehicleDataRequests
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.slf4j.Logger
import java.net.HttpURLConnection

class HMKitFleetTest : BaseTest() {
    private val clearanceRequests = mockk<ClearanceRequests>()
    private val vehicleDataRequests = mockk<VehicleDataRequests>()

    @BeforeEach
    fun setUp() {
        val koin = koinApplication {
            modules(
                module {
                    single { clearanceRequests }
                    single { vehicleDataRequests }
                    single { mockk<Logger>(relaxed = true) }
                    single { CoroutineScope(Dispatchers.IO) }
                }
            )
        }

        mockkConstructor(Koin::class)
        every { anyConstructed<Koin>().start() } returns koin.koin
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
        unmockkConstructor(Koin::class)
        clearAllMocks()
    }

    @Test
    fun deleteClearance() = runBlocking {
        coEvery {
            clearanceRequests.deleteClearance(any())
        } returns Response(RequestClearanceResponse("vin1", ClearanceStatus.Status.REVOKING), null)

        val hmkit = HMKitFleet(configuration.toJsonString())
        val delete = hmkit.deleteClearance("vin1").get()
        assertTrue(delete.response?.vin == "vin1")
        assertTrue(delete.response?.status == ClearanceStatus.Status.REVOKING)
    }

    @Test
    fun getClearance() = runBlocking {
        coEvery {
            clearanceRequests.getClearanceStatus("vin1")
        } returns Response(ClearanceStatus("vin1", ClearanceStatus.Status.REVOKING), null)

        val hmkit = HMKitFleet(configuration.toJsonString())
        val clearance = hmkit.getClearanceStatus("vin1").get()
        assertTrue(clearance.response?.vin == "vin1")
        assertTrue(clearance.response?.status == ClearanceStatus.Status.REVOKING)
    }

    @Test
    fun getVehicleStatus() = runTest {
        coEvery {
            vehicleDataRequests.getVehicleStatus("vin1")
        } returns Response(
            "{\"brand\":\"emulator\",\"diagnostics\":{\"odometer\":{\"data\":{\"unit\":\"kilometers\",\"value\":2050.0},\"failure\":null,\"timestamp\":\"2023-08-11T05:31:09.385Z\"}},\"vin\":\"vin1\"}",
            null
        )

        val hmkit = HMKitFleet(configuration.toJsonString())
        val vehicleStatus = hmkit.getVehicleState("vin1").get()
        val json = Json.decodeFromString<JsonObject>(vehicleStatus.response ?: "")
        assertTrue(json["vin"]?.jsonPrimitive?.content == "vin1")
    }

    @Test
    fun canSetCustomWebUrl() {
        stopKoin()
        clearAllMocks()

        val mockWebServer = MockWebServer()
        mockWebServer.start()
        mockWebServer.enqueue(
            MockResponse()
                .setBody("")
                .setResponseCode(HttpURLConnection.HTTP_CREATED)
        )

        val fakeUrl = mockWebServer.url("canSetCustomWebUrl").toString()

        HMKitFleet.Environment.webUrl = fakeUrl
        assertTrue(HMKitFleet.Environment.webUrl == fakeUrl)

        val hmkit = HMKitFleet(configuration.toJsonString())
        hmkit.getEligibility("vin1", Brand.SANDBOX).get()

        val recordedRequest = mockWebServer.takeRequest()
        assertTrue(recordedRequest.requestUrl.toString().contains(fakeUrl))

        mockWebServer.shutdown()
    }
}
