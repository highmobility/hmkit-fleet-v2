import com.highmobility.autoapi.Command
import com.highmobility.value.Bytes
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import model.AuthToken
import model.Brand
import model.ControlMeasure
import model.VehicleAccess
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

    internal var authToken: AuthToken? = null

    /**
     * Set the Service Account Configuration before calling other methods.
     */
    lateinit var configuration: ServiceAccountApiConfiguration

    init {
        Koin.start()
    }

    /**
     * Start the data access clearance process for a fleet vehicle.
     *
     * @param vin The vehicle VIN number
     * @param brand The vehicle brand
     * @param controlMeasures Optional control measures for some vehicle brands.
     * @return The clearance status
     */
    fun requestClearance(
        vin: String,
        brand: Brand,
        controlMeasures: List<ControlMeasure>?
    ): CompletableFuture<Response<ClearanceStatus>> {
        logger.debug("HMKitFleet: requestClearance: $vin")

        return GlobalScope.future {
            get<ClearanceRequests>().requestClearance(vin)
        }
    }

    /**
     * Get the status of VINs that have previously been registered for data access clearance with
     * [requestClearance]. After VIN is Approved, [getAccessCertificate] and subsequent
     * [sendCommand] can be sent
     *
     * @return The clearance status
     */
    fun getClearanceStatuses(): CompletableFuture<Response<List<ClearanceStatus>>> {
        logger.debug("HMKitFleet: getClearanceStatuses:")

        return GlobalScope.future {
            get<ClearanceRequests>().getClearanceStatuses()
        }
    }

    /**
     * Get Vehicle Access object. This can be queried for vehicles with [getClearanceStatuses]
     * Approved. The returned object can be used with [revokeClearance] or [sendCommand].
     * The user should securely store this object for later use.
     *
     * @param vin The vehicle VIN number
     * @param brand The vehicle brand
     * @return The vehicle access object
     */
    fun getVehicleAccess(vin: String, brand: Brand):
            CompletableFuture<Response<VehicleAccess>> {

        return CompletableFuture()
    }

    /**
     * Send a telematics command to the vehicle.
     *
     * @param vehicleAccess The vehicle access object returned in [getVehicleAccess]
     * @param command The command that is sent to the vehicle.
     * @return The response command from the vehicle.
     */
    fun sendCommand(
        vehicleAccess: VehicleAccess,
        command: Bytes
    ): CompletableFuture<Response<Bytes>> {
        // TODO: 23/11/20 implement

        return GlobalScope.future {
            Response()
        }
    }

    /**
     * Revoke the vehicle clearance. After this, the [VehicleAccess] object is invalid.
     *
     * @param vehicleAccess The vehicle access object
     * @return Whether clearance was successful
     */
    fun revokeClearance(vehicleAccess: VehicleAccess): CompletableFuture<Boolean> {
        // TODO: 30/10/20 implement
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