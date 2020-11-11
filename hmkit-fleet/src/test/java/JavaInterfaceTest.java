import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import network.ClearVehicleResponse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These are to test public API in java, so it looks nice.
 * More meaningful tests in .kt files.
 */
class JavaInterfaceTest {
    List<String> vins = List.of("1", "12", "189");
    HMKitFleet fleetSdk = HMKitFleet.INSTANCE;

    @Test public void throwsIfConfigurationNotSet() {
        assertThrows(IllegalStateException.class, () -> {
            HMKitFleet.INSTANCE.requestClearance("vin1");
        });

        // set it for next tests
        ServiceAccountApiConfiguration configuration = BaseTestKt.getConfiguration();
        fleetSdk.setConfiguration(configuration);
    }

    @Test
    public void requestClearance() throws InterruptedException {
        CompletableFuture<ClearVehicleResponse>[] allRequests = new CompletableFuture[vins.size()];

        for (int i = 0; i < vins.size(); i++) {
            allRequests[i] = fleetSdk.requestClearance(vins.get(i));

            allRequests[i].thenAcceptAsync(response -> {
                System.out.println("single response " + response.getVin() + " " + response.getStatus());
            });
        }

        CompletableFuture<Void> allTasks = CompletableFuture.allOf(allRequests);
        allTasks.thenAccept(responses -> {
                    System.out.println("All responses finished: " + "");
                    for (int i = 0; i < vins.size(); i++) {
                        try {
                            ClearVehicleResponse response = allRequests[i].get();
                            System.out.println(response.getStatus());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                        assertTrue(true);
                    }
                }
        );

        // wait for the responses
        Thread.sleep(3500);
    }

    @Test
    public void RequestClearanceWithStreams() throws InterruptedException, ExecutionException {
        // requests with streams(dont have to loop)
        List<CompletableFuture<ClearVehicleResponse>> requests =
                vins.stream().map(vin -> fleetSdk.requestClearance(vin)).collect(Collectors.toList());

        CompletableFuture<Void> allRequests = CompletableFuture.allOf(requests.toArray(new CompletableFuture[requests.size()]));

        // callback for single requests
        for (CompletableFuture<ClearVehicleResponse> request : requests) {
            request.thenAcceptAsync(response -> {
                System.out.println("request response for VIN: " + response.getVin() + " " + response.getStatus());
            });
        }

        // callback when all requests are finished
        allRequests.thenAcceptAsync(response -> {
            System.out.println(">> All requests finished <<");
        });

        // start blocking thread
        allRequests.get();
    }

    public void revokeClearance() {
        // TODO:
        CompletableFuture<Boolean> task = fleetSdk.revokeClearance("vin1");
    }
}