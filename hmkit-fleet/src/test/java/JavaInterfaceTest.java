import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import model.Brand;
import model.ControlMeasure;
import model.Odometer;
import network.Response;
import model.ClearanceStatus;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These are to test public API in java, so it looks nice.
 * More meaningful tests in .kt files.
 */
class JavaInterfaceTest {
    List<String> vins = List.of("1", "12", "189");
    HMKitFleet fleetSdk = HMKitFleet.INSTANCE;

    @Test
    public void requestClearance() throws InterruptedException {
        CompletableFuture<Response<ClearanceStatus>>[] allRequests = new CompletableFuture[vins.size()];
        ControlMeasure measure = new Odometer(110000, Odometer.Length.KILOMETERS);

        for (int i = 0; i < vins.size(); i++) {
            allRequests[i] = fleetSdk.requestClearance(
                    vins.get(i), Brand.DAIMLER_FLEET, List.of(measure));

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
                            Response<ClearanceStatus> response = allRequests[i].get();
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
        ControlMeasure measure = new Odometer(110000, Odometer.Length.KILOMETERS);
        List<CompletableFuture<Response<ClearanceStatus>>> requests =
                vins.stream().map(vin -> fleetSdk
                        .requestClearance(vin, Brand.DAIMLER_FLEET, List.of(measure)))
                        .collect(Collectors.toList());

        CompletableFuture<Void> allRequests = CompletableFuture.allOf(
                requests.toArray(new CompletableFuture[requests.size()]));

        // callback for single requests
        for (CompletableFuture<Response<ClearanceStatus>> request : requests) {
            request.thenAcceptAsync(response -> {
                System.out.println("request response for VIN: "
                        + response.getResponse().getVin()
                        + " "
                        + response.getResponse().getStatus());
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
        ControlMeasure measure = new Odometer(110000, Odometer.Length.KILOMETERS);
//        CompletableFuture<Boolean> task = fleetSdk.revokeClearance("vin1");
    }
}