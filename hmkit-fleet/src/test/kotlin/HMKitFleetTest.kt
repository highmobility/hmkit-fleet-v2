import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import model.Brand
import network.AccessCertificateRequests
import network.AccessTokenRequests
import network.Response
import network.genericError
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class HMKitFleetTest : BaseTest() {
    private val accessCertificateRequests = mockk<AccessCertificateRequests>()
    private val accessTokenRequests = mockk<AccessTokenRequests>()

    fun setUp() {
        HMKitFleet // call the init first, so koin can be reset
        Koin.koinApplication = koinApplication {
            modules(module {
                single { accessCertificateRequests }
                single { accessTokenRequests }
            })
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testGetVehicleAccessRequests() = runBlocking {
        coEvery { accessCertificateRequests.getAccessCertificate(any()) } returns Response(
            mockAccessCert,
            null
        )
        coEvery {
            accessTokenRequests.getAccessToken(
                any(),
                any()
            )
        } returns Response(newAccessToken, null)

        val access = HMKitFleet.getVehicleAccess("vin1", Brand.MERCEDES_BENZ).get()

        coVerify { accessCertificateRequests.getAccessCertificate(any()) }
        coVerify { accessTokenRequests.getAccessToken(any(), any()) }

        assertTrue(access.response!!.vin == "vin1")
        assertTrue(access.response!!.brand == Brand.MERCEDES_BENZ)
        assertTrue(access.response!!.accessCertificate.hex == mockAccessCert.hex)
        assertTrue(access.response!!.accessToken == newAccessToken)
    }

    @Test
    fun accessTokenErrorReturned() = runBlocking {
        coEvery {
            accessTokenRequests.getAccessToken(
                any(),
                any()
            )
        } returns Response(null, genericError("accessTokenError"))

        val access = HMKitFleet.getVehicleAccess("vin1", Brand.MERCEDES_BENZ).get()

        assertTrue(access.response == null)
        assertTrue(access.error!!.detail == "accessTokenError")
    }

    @Test
    fun accessCertErrorReturned() = runBlocking {
        coEvery {
            accessTokenRequests.getAccessToken(
                any(),
                any()
            )
        } returns Response(newAccessToken, null)

        coEvery { accessCertificateRequests.getAccessCertificate(any()) } returns Response(
            null,
            genericError("accessCertError")
        )

        val access = HMKitFleet.getVehicleAccess("vin1", Brand.MERCEDES_BENZ).get()
        assertTrue(access.response == null)
        assertTrue(access.error!!.detail == "accessCertError")
    }
}