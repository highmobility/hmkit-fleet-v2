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
