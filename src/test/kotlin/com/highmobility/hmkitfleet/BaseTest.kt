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

import com.charleskorn.kaml.Yaml
import com.highmobility.crypto.AccessCertificate
import com.highmobility.crypto.value.DeviceSerial
import com.highmobility.crypto.value.Signature
import io.mockk.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.highmobility.hmkitfleet.model.AccessToken
import com.highmobility.hmkitfleet.model.AuthToken
import com.highmobility.hmkitfleet.model.VehicleAccess
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.koin.test.KoinTest
import org.slf4j.Logger
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime

const val testApiKey = "apiKey"
const val testVin = "C0NNECT0000000001"

internal fun notExpiredAuthToken(): AuthToken {
    return AuthToken(
        "e903cb43-27b1-4e47-8922-c04ecd5d2019",
        LocalDateTime.now(),
        LocalDateTime.now().plusHours(1)
    )
}

internal fun expiredAuthToken(): AuthToken {
    return AuthToken(
        "e903cb43-27b1-4e47-8922-c04ecd5d2019",
        LocalDateTime.parse("2020-11-17T04:50:16"),
        LocalDateTime.parse("2020-11-17T05:50:16")
    )
}

internal val mockSignature = mockk<Signature> {
    every { base64UrlSafe } returns "qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqg=="
    every { base64 } returns "qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqg=="
    every { hex } returns "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
}

internal val mockSerial = mockk<DeviceSerial> {
    every { hex } returns "AAAAAAAAAAAAAAAAAA"
    every { base64 } returns "qqqqqqqqqqqq"
}

internal val newAccessToken: AccessToken = Json.decodeFromString(
    "{\n" +
            "  \"token_type\": \"bearer\",\n" +
            "  \"scope\": \"diagnostics.mileage door_locks.locks windows.windows_positions\",\n" +
            "  \"refresh_token\": \"7f7a9be0-04c9-4202-a59f-35d55079b6ba\",\n" +
            "  \"expires_in\": 600,\n" +
            "  \"access_token\": \"a50e89e5-093c-4727-8101-4c6e81addabe\"\n" +
            "}"
)

internal val mockAccessCert = mockk<AccessCertificate> {
    every { hex } returns "01030000030400000000000000040500000000000000050600000000000000000600000000000006000000000000000006000000000000060000000000000000060000000000000600000000000000000600000000000607010203070804050609090A0A0A0A0A0A0A0A0A0B00000000000000000600000000000006000000000000000006000000000000060000000000000000060000000000000600000000000000000600000000000B"
    every { base64 } returns "AQMAAAMEAAAAAAAAAAQFAAAAAAAAAAUGAAAAAAAAAAAGAAAAAAAABgAAAAAAAAAABgAAAAAAAAYAAAAAAAAAAAYAAAAAAAAGAAAAAAAAAAAGAAAAAAAGBwECAwcIBAUGCQkKCgoKCgoKCgoLAAAAAAAAAAAGAAAAAAAABgAAAAAAAAAABgAAAAAAAAYAAAAAAAAAAAYAAAAAAAAGAAAAAAAAAAAGAAAAAAAL"
    every { gainerSerial} returns mockSerial
}

internal val newVehicleAccess = VehicleAccess(
    testVin,
    newAccessToken,
    mockAccessCert
)

open class BaseTest : KoinTest {
    val configuration = readConfigurationFromFile()

    private fun readConfigurationFromFile(): ServiceAccountApiConfiguration {
        val homeDir = System.getProperty("user.home")

        val credentialsFilePath =
            Paths.get("$homeDir/.config/high-mobility/fleet-sdk/credentials.yaml")
        val credentialsDirectory = credentialsFilePath.parent

        if (Files.exists(credentialsDirectory) == false) {
            Files.createDirectories(credentialsDirectory)
            Files.createFile(credentialsFilePath)
            throw InstantiationException("Please add Service account credentials to $credentialsFilePath")
        }

        val credentialsContent = Files.readString(credentialsFilePath)

        val configuration =
            spyk(
                Yaml.default.decodeFromString(
                    ServiceAccountApiConfiguration.serializer(),
                    credentialsContent
                )
            )
        return configuration
    }

    val mockLogger = mockk<Logger>()

    @BeforeEach
    fun before() {
        // mockk the logs
        every { mockLogger.warn(allAny()) } just Runs

        val capturedDebugLog = CapturingSlot<String>()
        every { mockLogger.debug(capture(capturedDebugLog)) } answers {
            println(capturedDebugLog.captured)
        }

        every { mockLogger.info(capture(capturedDebugLog)) } answers {
            println(capturedDebugLog.captured)
        }

        every { mockLogger.error(allAny()) } just Runs
        every { mockLogger.error(any(), any<Throwable>()) } just Runs
    }

    @AfterEach
    fun after() {

    }

    fun errorLogExpected(runnable: Runnable) {
        errorLogExpected(1, runnable)
    }

    fun errorLogExpected(count: Int, runnable: Runnable) {
        runnable.run()
        verify(exactly = count) { mockLogger.error(allAny()) }
    }

    fun warningLogExpected(runnable: Runnable) {
        warningLogExpected(1, runnable)
    }

    fun warningLogExpected(count: Int, runnable: Runnable) {
        runnable.run()
        verify(exactly = count) { mockLogger.warn(allAny()) }
    }

    fun debugLogExpected(runnable: Runnable) {
        debugLogExpected(1, runnable)
    }

    fun debugLogExpected(count: Int, runnable: Runnable) {
        runnable.run()
        verify(exactly = count) { mockLogger.debug(allAny()) }
    }
}