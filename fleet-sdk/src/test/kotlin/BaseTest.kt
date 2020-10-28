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

import com.highmobility.hmkit.HMKit
import io.mockk.*
import network.WebService
import org.junit.Rule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.koin.test.mock.MockProviderRule
import org.koin.test.mock.declareMock
import org.slf4j.Logger

val modules = module {
    single { WebService(getProperty("apiKey")) }
    single { HMKit.getInstance() }
    single { mockk<Logger>() }
}

val testApiKey = "apiKey"

open class BaseTest : KoinTest {
    val mockLogger by inject<Logger>()

    /*
        @get:Rule
        val mockProvider = MockProviderRule.create { clazz ->
            mockk(clazz.java.toString())
        }
    */
    companion object {
        lateinit var fleetSdk: FleetSdk

        @JvmStatic @BeforeAll
        fun beforeAll() {
            fleetSdk = FleetSdk.getInstance(testApiKey)
            loadKoinModules(modules)

            /*
            stopKoin()

            startKoin {
                properties(values = mapOf("apiKey" to "apiKey"))
                modules
            }
             */
        }
    }

    @BeforeEach
    fun before() {
        clearAllMocks()

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