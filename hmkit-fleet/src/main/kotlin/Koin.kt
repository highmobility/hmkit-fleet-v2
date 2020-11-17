import com.highmobility.hmkit.HMKit
import network.WebService
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
        single { WebService(get(), get(), get(), get<HMKitFleet.Environment>().url) }
        single { HMKit.getInstance() }
    }

    lateinit var koinApplication: KoinApplication

    fun start() {
        koinApplication = koinApplication {
            modules(
                koinModules
            )
        }
    }

    interface FleetSdkKoinComponent : KoinComponent {
        override fun getKoin(): Koin {
            return koinApplication.koin
        }
    }
}