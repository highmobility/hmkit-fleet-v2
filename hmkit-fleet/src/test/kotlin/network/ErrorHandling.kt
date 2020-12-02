package network

import io.mockk.coEvery
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertTrue
import java.net.HttpURLConnection

/**
 * Respond with an error for this [request] and assert the fields are parsed into the response
 */
internal inline fun <T> testErrorResponseReturned(
    mockWebServer: MockWebServer,
    request: (mockUrl: String) -> Response<T>
) {
    val mockResponse = MockResponse()
        .setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
        .setBody(
            "{\"errors\":" +
                    "[{\"detail\":\"Missing or invalid assertion. It must be a JWT signed with the service account key.\"," +
                    "\"source\":\"assertion\"," +
                    "\"title\":\"Not authorized\"}]}"
        )
    mockWebServer.enqueue(mockResponse)
    val mockUrl = mockWebServer.url("").toString()

    val response = request(mockUrl)

    assertTrue(response.error!!.title == "Not authorized")
    assertTrue(response.error!!.source == "assertion")
    assertTrue(response.error!!.detail == "Missing or invalid assertion. It must be a JWT signed with the service account key.")
}

/**
 * Respond with unknown response and assert generic error is returned
 */
internal inline fun <T> testForUnknownResponseGenericErrorReturned(
    mockWebServer: MockWebServer,
    request: (mockUrl: String) -> Response<T>
) {
    val mockResponse = MockResponse()
        .setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
        .setBody(
            "{\"invalidKey\":\"invalidValue\"}"
        )
    mockWebServer.enqueue(mockResponse)
    val mockUrl = mockWebServer.url("").toString()

    val response = request(mockUrl)

    val genericError = genericError("")
    assertTrue(response.error!!.title == genericError.title)
}

/**
 * Auth token request returns error
 */
internal inline fun <T> testAuthTokenErrorReturned(
    mockWebServer: MockWebServer,
    authTokenRequests: AuthTokenRequests,
    request: (mockUrl: String) -> Response<T>
) {
    coEvery { authTokenRequests.getAuthToken() } returns Response(
        null,
        genericError("auth token error")
    )

    val mockUrl = mockWebServer.url("").toString()

    val response = request(mockUrl)

    assertTrue(response.response == null)
    assertTrue(response.error!!.detail == "auth token error")
}