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

import com.highmobility.crypto.Crypto
import com.highmobility.hmkitfleet.network.*
import com.highmobility.hmkitfleet.network.AccessCertificateRequests
import com.highmobility.hmkitfleet.network.AuthTokenRequests
import com.highmobility.hmkitfleet.network.Cache
import com.highmobility.hmkitfleet.network.ClearanceRequests
import com.highmobility.hmkitfleet.network.Requests
import okhttp3.OkHttpClient
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.slf4j.LoggerFactory

internal object Koin {
    val koinModules = module {
        single { LoggerFactory.getLogger(HMKitFleet::class.java) }
        single { OkHttpClient() }
        single { HMKitFleet.environment }
        single { Crypto() }
        single { Requests(get(), get(), HMKitFleet.environment.url) }
        single { Cache() }
        single {
            AuthTokenRequests(
                get(),
                get(),
                get(),
                HMKitFleet.environment.url,
                HMKitFleet.configuration,
                get()
            )
        }
        single {
            AccessTokenRequests(
                get(),
                get(),
                HMKitFleet.environment.url,
                get(),
                HMKitFleet.configuration
            )
        }
        single { ClearanceRequests(get(), get(), HMKitFleet.environment.url, get()) }
        single {
            AccessCertificateRequests(
                get(),
                get(),
                HMKitFleet.environment.url,
                HMKitFleet.configuration.getClientPrivateKey(),
                HMKitFleet.configuration.clientCertificate,
                get()
            )
        }
        single {
            TelematicsRequests(
                get(),
                get(),
                HMKitFleet.environment.url,
                HMKitFleet.configuration.getClientPrivateKey(),
                HMKitFleet.configuration.clientCertificate,
                get()
            )
        }
        single {
            UtilityRequests(
                get(),
                get(),
                HMKitFleet.environment.url,
                get()
            )
        }
    }

    lateinit var koinApplication: KoinApplication

    fun start() {
        koinApplication = koinApplication {
            modules(koinModules)
        }
    }

    interface FleetSdkKoinComponent : KoinComponent {
        override fun getKoin(): Koin {
            return koinApplication.koin
        }
    }
}