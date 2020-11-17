import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import model.AuthToken;
import network.Response;
import network.response.ClearVehicle;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These are to test public API in java, so it looks nice.
 * More meaningful tests in .kt files.
 */
class JavaInterfaceTest {
    List<String> vins = List.of("1", "12", "189");
    HMKitFleet fleetSdk = HMKitFleet.INSTANCE;
    AuthToken token = new AuthToken("token", LocalDateTime.now(), LocalDateTime.now());

    @Test public void getToken() {
        // set it for next tests
        ServiceAccountApiConfiguration configuration = BaseTestKt.getConfiguration();
        CompletableFuture<Response<AuthToken>> tokenFuture = fleetSdk.getAuthToken(configuration);
    }

    @Test
    public void requestClearance() throws InterruptedException {

        CompletableFuture<Response<ClearVehicle>>[] allRequests = new CompletableFuture[vins.size()];

        for (int i = 0; i < vins.size(); i++) {
            allRequests[i] = fleetSdk.requestClearance(token, vins.get(i));

            allRequests[i].thenAcceptAsync(response -> {
                System.out.println("single response "
                        + response.getResponse().getVin()
                        + " "
                        + response.getResponse().getStatus());
            });
        }

        CompletableFuture<Void> allTasks = CompletableFuture.allOf(allRequests);
        allTasks.thenAccept(responses -> {
                    System.out.println("All responses finished: " + "");
                    for (int i = 0; i < vins.size(); i++) {
                        try {
                            Response<ClearVehicle> response = allRequests[i].get();
                            System.out.println(response.getResponse().getStatus());
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
        List<CompletableFuture<Response<ClearVehicle>>> requests =
                vins.stream().map(vin -> fleetSdk.requestClearance(token, vin)).collect(Collectors.toList());

        CompletableFuture<Void> allRequests = CompletableFuture.allOf(requests.toArray(new CompletableFuture[requests.size()]));

        // callback for single requests
        for (CompletableFuture<Response<ClearVehicle>> request : requests) {
            request.thenAcceptAsync(response -> {
                System.out.println("request response for VIN: " + response.getResponse().getVin() + " " + response.getResponse().getStatus());
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
        CompletableFuture<Boolean> task = fleetSdk.revokeClearance(token, "vin1");
    }
}