import com.highmobility.hmkit.HMKit
import network.WebService
import okhttp3.OkHttpClient
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.slf4j.LoggerFactory

object DI {
    val logger: org.slf4j.Logger = LoggerFactory.getLogger(FleetSdk::class.java)
    val koinModules = module {
        single { OkHttpClient() }
        single { WebService(getProperty("apiKey"), get()) }
        single { HMKit.getInstance() }
        single { logger }
    }

    fun start(apiKey: String) {
        startKoin {
            properties(values = mapOf("apiKey" to apiKey))
            koinModules
        }
    }
}