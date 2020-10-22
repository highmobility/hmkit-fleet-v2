import org.junit.jupiter.api.Test;

class FleetSdkTest extends BaseTest {

    @Test public void debugLogEmittedOnInit() {
        FleetSdk sdk = FleetSdk.INSTANCE;
        debugLogExpected(2, () -> sdk.init("apiKey"));
    }

    @Test public void testInitRequestSent() {

    }
}
