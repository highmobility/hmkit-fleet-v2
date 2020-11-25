package network

import BaseTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.component.get
import java.net.HttpURLConnection

internal class AccessTokenRequestsTest : BaseTest() {
    val mockWebServer = MockWebServer()
    val client = OkHttpClient()

    @BeforeEach
    fun setUp() {
        mockWebServer.start()
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun createAccessToken() {
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_CREATED)
            .setBody(
                "{\n" +
                        "  \"token_type\": \"bearer\",\n" +
                        "  \"scope\": \"diagnostics.mileage door_locks.locks windows.windows_positions\",\n" +
                        "  \"refresh_token\": \"7f7a9be0-04c9-4202-a59f-35d55079b6ba\",\n" +
                        "  \"expires_in\": 600,\n" +
                        "  \"access_token\": \"a50e89e5-093c-4727-8101-4c6e81addabe\"\n" +
                        "}"
            )
        mockWebServer.enqueue(mockResponse)
        val baseUrl: HttpUrl = mockWebServer.url("")
        val webService = AccessTokenRequests(client, get(), baseUrl.toString())

        val status = runBlocking {
            webService.createAccessToken(mockk(), "", "")

        }
    }
}