import com.highmobility.hmkit.HMKit
import network.WebService
import okhttp3.OkHttpClient
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.slf4j.LoggerFactory

object DI {
    val koinModules = module {
        single { LoggerFactory.getLogger(HMKitFleet::class.java) }
        single { OkHttpClient() }
        single { WebService(getProperty("apiKey"), get(), get()) }
        single { HMKit.getInstance() }
    }

    lateinit var koinApplication: KoinApplication

    fun start(apiKey: String) {
        koinApplication = koinApplication {
            properties(values = mapOf("apiKey" to apiKey))
            modules(koinModules)
        }
    }

    interface FleetSdkKoinComponent : KoinComponent {
        override fun getKoin(): Koin {
            return koinApplication.koin
        }
    }
}