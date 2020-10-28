import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * These are to test public API in java, so it looks nice.
 * More meaningful tests in .kt files.
 */
class JavaInterfaceTest extends BaseTest {
    FleetSdk sdk = FleetSdk.getInstance("apiKey");
    List<String> vins = List.of("1");

    @Test public void debugLogEmitOnInit() {
        debugLogExpected(1, () -> sdk.toString());
    }

    @Test public void throwsClearVehiclesNoApiKey() {
        FleetSdk.FleetSdkResult result = new FleetSdk.FleetSdkResult() {
            @Override public void onVehiclesCleared() {

            }

            @Override public void onFailure() {

            }
        };

        assertThrows(IllegalStateException.class, () -> sdk.clearVehicles(vins, result));
    }

    @Test public void throwsDeleteClearVehiclesNoApiKey() {
        FleetSdk.FleetSdkResult result = new FleetSdk.FleetSdkResult() {
            @Override public void onVehiclesCleared() {

            }

            @Override public void onFailure() {

            }
        };

        assertThrows(IllegalStateException.class, () -> sdk.deleteVehicleClearance("vin", result));
    }
}