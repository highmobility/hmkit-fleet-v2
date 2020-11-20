import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import model.AuthToken
import network.AuthTokenRequests
import network.ClearanceRequests
import network.Response
import network.response.ClearanceStatus
import org.koin.core.component.get
import org.koin.core.component.inject
import org.slf4j.Logger
import java.util.concurrent.CompletableFuture

object HMKitFleet : Koin.FleetSdkKoinComponent {
    private val logger by inject<Logger>()
    var environment: Environment = Environment.PRODUCTION

    init {
        Koin.start()
    }

    /**
     * Get the auth token that is required for future requests.
     *
     * @param configuration The Service account API configuration
     * @return The auth token
     */
    fun getAuthToken(configuration: ServiceAccountApiConfiguration):
            CompletableFuture<Response<AuthToken>> {
        return GlobalScope.future {
            get<AuthTokenRequests>().getAuthToken(configuration)
        }
    }

    /**
     * Clear the fleet vehicle for future requests.
     *
     * @param authToken The auth token acquired in [getAuthToken]
     * @param vin The vehicle VIN
     * @return The clearance status.
     */
    fun requestClearance(
        authToken: AuthToken,
        vin: String
    ): CompletableFuture<Response<ClearanceStatus>> {
        logger.debug("HMKitFleet: requestClearance: $vin")

        return GlobalScope.future {
            get<ClearanceRequests>().requestClearance(authToken, vin)
        }
    }

    /**
     * Get the vehicle clearance status. Use this request to check the status after starting the clearance
     * process with {@link requestClearance}
     *
     * @param authToken The auth token acquired in [getAuthToken]
     * @param vin The vehicle VIN
     * @return The clearance status.
     */
    fun getClearanceStatus(
        authToken: AuthToken,
        vin: String
    ): CompletableFuture<Response<ClearanceStatus>> {
        logger.debug("HMKitFleet: requestClearance: $vin")

        return GlobalScope.future {
            get<ClearanceRequests>().requestClearance(authToken, vin)
        }
    }

    fun revokeClearance(authToken: AuthToken, vin: String): CompletableFuture<Boolean> {
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

    enum class Environment {
        PRODUCTION, SANDBOX, DEV, DEV_SANDBOX;

        val url: String
            get() {
                return when (this) {
                    PRODUCTION -> "https://api.high-mobility.com/v1"
                    SANDBOX -> "https://sandbox.api.high-mobility.com/v1"
                    DEV -> "https://api.develop.high-mobility.net/v1"
                    DEV_SANDBOX -> "https://sandbox.api.develop.high-mobility.net/v1"
                }
            }
    }
}