package network

import io.mockk.mockk
import kotlinx.serialization.json.*
import org.junit.Test

internal class RequestsTest {
    @Test
    fun parsesErrorFormat1() {
        val error = buildJsonArray {
            addJsonObject {
                put("title", "title")
                put("source", "source")
                put("detail", "detail")
            }
        }

        val requests = Requests(mockk(), mockk(), "mockk()")
        val body = error.toString()
        val response = requests.parseError<Any>(body)

        assert(response.error?.title == "title")
        assert(response.error?.source == "source")
        assert(response.error?.detail == "detail")
    }

    @Test
    fun parsesErrorFormat2() {
        val error = buildJsonObject {
            put("errors", buildJsonArray {
                addJsonObject {
                    put("title", "title")
                    put("source", "source")
                    put("detail", "detail")
                }
            })
        }

        val requests = Requests(mockk(), mockk(), "mockk()")
        val body = error.toString()
        val response = requests.parseError<Any>(body)

        assert(response.error?.title == "title")
        assert(response.error?.source == "source")
        assert(response.error?.detail == "detail")
    }

    @Test
    fun parsesErrorFormat3() {
        val error = buildJsonObject {
            put("error", "title")
            put("error_description", "detail")
        }

        val requests = Requests(mockk(), mockk(), "mockk()")
        val body = error.toString()
        val response = requests.parseError<Any>(body)

        assert(response.error?.title == "title")
        assert(response.error?.source == null)
        assert(response.error?.detail == "detail")
    }
}
