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

package com.highmobility.hmkitfleet

import com.highmobility.hmkitfleet.model.ClearanceStatus
import com.highmobility.hmkitfleet.model.RequestClearanceResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import com.highmobility.hmkitfleet.network.AccessCertificateRequests
import com.highmobility.hmkitfleet.network.AccessTokenRequests
import com.highmobility.hmkitfleet.network.ClearanceRequests
import com.highmobility.hmkitfleet.network.Response
import com.highmobility.hmkitfleet.network.genericError
import io.mockk.clearAllMocks
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.slf4j.Logger

class HMKitFleetTest : BaseTest() {
    private val accessCertificateRequests = mockk<AccessCertificateRequests>()
    private val accessTokenRequests = mockk<AccessTokenRequests>()
    private val clearanceRequests = mockk<ClearanceRequests>()

    @BeforeEach
    fun setUp() {
        HMKitFleet // call the init first, so koin can be reset
        Koin.koinApplication = koinApplication {
            modules(module {
                single { accessCertificateRequests }
                single { accessTokenRequests }
                single { clearanceRequests }
                single { mockk<Logger>(relaxed = true) }
            })
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
        clearAllMocks()
    }

    @Test
    fun testGetVehicleAccessRequests() = runBlocking {
        coEvery { accessCertificateRequests.getAccessCertificate(any()) } returns Response(
            mockAccessCert,
            null
        )
        coEvery {
            accessTokenRequests.getAccessToken(
                any()
            )
        } returns Response(newAccessToken, null)

        val access = HMKitFleet.getVehicleAccess("vin1").get()

        coVerify { accessCertificateRequests.getAccessCertificate(any()) }
        coVerify { accessTokenRequests.getAccessToken(any()) }

        assertTrue(access.response!!.vin == "vin1")
        assertTrue(access.response!!.accessCertificate.hex == mockAccessCert.hex)
        assertTrue(access.response!!.accessToken == newAccessToken)
    }

    @Test
    fun accessTokenErrorReturned() = runBlocking {
        coEvery {
            accessTokenRequests.getAccessToken(
                any()
            )
        } returns Response(null, genericError("accessTokenError"))

        val access = HMKitFleet.getVehicleAccess("vin1").get()

        assertTrue(access.response == null)
        assertTrue(access.error!!.detail == "accessTokenError")
    }

    @Test
    fun accessCertErrorReturned() = runBlocking {
        coEvery {
            accessTokenRequests.getAccessToken(
                any()
            )
        } returns Response(newAccessToken, null)

        coEvery { accessCertificateRequests.getAccessCertificate(any()) } returns Response(
            null,
            genericError("accessCertError")
        )

        val access = HMKitFleet.getVehicleAccess("vin1").get()
        assertTrue(access.response == null)
        assertTrue(access.error!!.detail == "accessCertError")
    }

    @Test
    fun revokeClearance() = runBlocking {
        coEvery {
            accessTokenRequests.deleteAccessToken(
                any()
            )
        } returns Response(true, null)

        val access = HMKitFleet.revokeClearance(newVehicleAccess).get()
        assertTrue(access.response == true)
    }

    @Test
    fun deleteClearance() = runBlocking {
        coEvery {
            clearanceRequests.deleteClearance(any())
        } returns Response(RequestClearanceResponse("vin1", ClearanceStatus.Status.REVOKING), null)

        val delete = HMKitFleet.deleteClearance("vin1").get()
        assertTrue(delete.response?.vin == "vin1")
        assertTrue(delete.response?.status == ClearanceStatus.Status.REVOKING)
    }

    @Test
    fun canSetCustomWebUrl() {
        HMKitFleet.environment = HMKitFleet.Environment.PRODUCTION
        HMKitFleet.Environment.webUrl = "asd"
        assert(HMKitFleet.environment.url == "asd")
    }
}