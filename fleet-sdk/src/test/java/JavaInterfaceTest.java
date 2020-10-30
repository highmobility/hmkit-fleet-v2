import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import network.WebService;

/**
 * These are to test public API in java, so it looks nice.
 * More meaningful tests in .kt files.
 */
class JavaInterfaceTest extends BaseTest {
    List<String> vins = List.of("1");

    public void requestClearance() throws ExecutionException, InterruptedException {
        CompletableFuture<WebService.RequestClearanceResponse>[] allRequests = new CompletableFuture[vins.size()];

        for (int i = 0; i < vins.size(); i++) {
            allRequests[i] = fleetSdk.requestClearance(vins.get(i));
        }

        CompletableFuture<Void> allTasks = CompletableFuture.allOf(allRequests);
        CompletableFuture cf = allTasks.thenRun(() -> System.out.println("All tasks finished: "));

        // start a blocking thread

/*        Executors.newCachedThreadPool().submit<Any?> {
            Thread.sleep(500)
            completableFuture.complete(RequestClearanceResult("http error"))
        }*/

        cf.get();
    }

    public void revokeClearance() {
        // TODO:
        CompletableFuture<Boolean> task = fleetSdk.revokeClearance("vin1");
    }
}