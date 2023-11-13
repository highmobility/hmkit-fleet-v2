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

import com.highmobility.crypto.value.Signature
import com.highmobility.hmkitfleet.model.AccessToken
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.koin.test.KoinTest
import org.slf4j.Logger
import java.nio.file.Files
import java.nio.file.Paths

internal fun notExpiredAccessToken(): AccessToken {
  return AccessToken(
    "e903cb43-27b1-4e47-8922-c04ecd5d2019",
    360
  )
}

open class BaseTest : KoinTest {
  val privateKeyConfiguration = readPrivateKeyConfiguration()

  internal fun readPrivateKeyJsonString(): String {
    val homeDir = System.getProperty("user.home")

    val credentialsFilePath =
      Paths.get("$homeDir/.config/high-mobility/fleet-sdk/credentialsPrivateKey.json")
    val credentialsDirectory = credentialsFilePath.parent

    if (Files.exists(credentialsDirectory) == false) {
      Files.createDirectories(credentialsDirectory)
      Files.createFile(credentialsFilePath)
      throw InstantiationException("Please add oauth credentials to $credentialsFilePath")
    }

    val credentialsContent = String(Files.readAllBytes(credentialsFilePath))
    return credentialsContent
  }

  private fun readPrivateKeyConfiguration(): HMKitConfiguration {

    val credentialsContent = readPrivateKeyJsonString()

    val configuration = spyk(
      HMKitConfiguration.Builder()
        .credentials(
          HMKitOAuthCredentials("client_id", credentialsContent)
        )
        .build()
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
