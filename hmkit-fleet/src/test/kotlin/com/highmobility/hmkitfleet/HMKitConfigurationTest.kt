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
package com.highmobility.hmkitfleet.com.highmobility.hmkitfleet

import com.highmobility.hmkitfleet.BaseTest
import com.highmobility.hmkitfleet.HMKitConfiguration
import com.highmobility.hmkitfleet.HMKitFleet
import io.mockk.every
import io.mockk.mockk
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HMKitConfigurationTest : BaseTest() {
  @Test
  fun customHttpClientTest() {
    val call = mockk<Call> {
      every {
        enqueue(any())
      } answers {
        firstArg<Callback>().onResponse(
          this@mockk,
          mockk(relaxed = true)
        )
      }
    }

    val client = mockk<OkHttpClient> {
      every { newCall(any()) } returns call
    }
    val hmkitConf = HMKitConfiguration.Builder()
      .client(client)
      .credentials(privateKeyConfiguration.credentials)
      .build()

    val hmkit = HMKitFleet(hmkitConf)

    // verify mock without throwing succeeds
    hmkit.getClearanceStatus("vin1").get()

    // now throw exception on new call and verify it is thrown
    every { client.newCall(any()) } throws Exception("test")
    assertThrows<Exception> {
      hmkit.getClearanceStatus("vin1").get()
    }
  }
}
