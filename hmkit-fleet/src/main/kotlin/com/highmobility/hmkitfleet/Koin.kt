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

import com.highmobility.hmkitfleet.network.AccessTokenRequests
import com.highmobility.hmkitfleet.network.Cache
import com.highmobility.hmkitfleet.network.ClearanceRequests
import com.highmobility.hmkitfleet.network.Requests
import com.highmobility.hmkitfleet.network.UtilityRequests
import com.highmobility.hmkitfleet.network.VehicleDataRequests
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.slf4j.LoggerFactory

internal class Koin(
  hmKitConfiguration: HMKitConfiguration
) {
  private val koinModules = module {
    val environment = hmKitConfiguration.environment

    single { LoggerFactory.getLogger(HMKitFleet::class.java) }
    single { hmKitConfiguration.client }
    single { environment }
    single { Requests(get(), get(), environment.url) }
    single { Cache() }
    single {
      AccessTokenRequests(
        get(),
        get(),
        environment.url,
        hmKitConfiguration.credentials,
        get()
      )
    }
    single { ClearanceRequests(get(), get(), environment.url, get()) }

    single {
      UtilityRequests(
        get(),
        get(),
        environment.url,
        get()
      )
    }
    single {
      VehicleDataRequests(
        get(),
        get(),
        environment.url,
        get()
      )
    }

    single { CoroutineScope(Dispatchers.IO) }
  }

  private lateinit var koinApplication: KoinApplication

  fun start(): Koin {
    koinApplication = koinApplication {
      modules(koinModules)
    }

    return koinApplication.koin
  }
}
