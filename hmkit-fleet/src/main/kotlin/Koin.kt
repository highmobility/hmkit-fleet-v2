import com.highmobility.hmkit.HMKit
import model.Database
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
        single { Database() }
        single { OkHttpClient() }
        single { WebService(get(), get(), get(), get(), get()) }
        single { HMKit.getInstance() }
    }

    lateinit var koinApplication: KoinApplication
    fun start(apiConfiguration: ServiceAccountApiConfiguration) {
        koinApplication = koinApplication {
            modules(
                module { single { apiConfiguration } },
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