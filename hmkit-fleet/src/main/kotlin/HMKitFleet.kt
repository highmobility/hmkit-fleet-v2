import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import network.ClearVehicleResponse
import network.WebService
import org.koin.core.component.inject
import org.slf4j.Logger
import java.util.concurrent.CompletableFuture

object HMKitFleet : Koin.FleetSdkKoinComponent {
    private val logger by inject<Logger>()
    private val webService by inject<WebService>()

    var configuration: ServiceAccountApiConfiguration? = null
        set(value) {
            if (value == null) return
            field = value
            Koin.start(value)
            logger.info("HMKit Fleet initialised")
        }

    fun requestClearance(vin: String): CompletableFuture<ClearVehicleResponse> {
        throwIfConfigurationNotSet()
        logger.debug("HMKitFleet: requestClearance: $vin")

        return GlobalScope.future {
            webService.clearVehicle(vin)
        }
    }

    fun revokeClearance(vin: String): CompletableFuture<Boolean> {
        throwIfConfigurationNotSet()
        // TODO: 30/10/20 implement
        /*
        Although the OAuth2 API is not used to get an access token, it is used to refresh the access
        token and to revoke access. Revoking access means that the VIN is removed from the clearance list.

        By using the POST /access_tokens endpoint, the data customer passes in the refresh token and
         receives a new access token

        By using the DELETE /access_tokens endpoint, the data customer passes in the access token
        and the vehicle is deleted from the clearance list
         */

        return CompletableFuture()
    }

    private fun throwIfConfigurationNotSet() {
        if (configuration == null) throw IllegalStateException("HMKitFleet setConfiguration() not called")
    }
}