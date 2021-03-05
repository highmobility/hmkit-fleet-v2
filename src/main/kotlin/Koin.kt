import com.highmobility.crypto.Crypto
import network.*
import network.AccessCertificateRequests
import network.AuthTokenRequests
import network.Cache
import network.ClearanceRequests
import network.Requests
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