package network

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertTrue
import java.net.HttpURLConnection

/**
 * Respond with an error for this [request] and assert the fields are parsed into the response
 */
// TODO: 20/11/20 the error spec will be updated. Update the tests then
internal inline fun <T> testErrorResponseHandled(
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
internal inline fun <T> testUnknownResponseHandled(
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
