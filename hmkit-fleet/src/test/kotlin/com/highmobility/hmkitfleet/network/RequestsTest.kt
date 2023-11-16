/*
 * The MIT License
 *
 * Copyright (c) 2023- High-Mobility GmbH (https://high-mobility.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.highmobility.hmkitfleet.network

import io.mockk.mockk
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test

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
      put(
        "errors",
        buildJsonArray {
          addJsonObject {
            put("title", "title")
            put("source", "source")
            put("detail", "detail")
          }
        }
      )
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

  @Test
  fun parsesUnknownResponse() {
    val requests = Requests(mockk(), mockk(), "mockk()")
    val body = "unknown"
    val response = requests.parseError<Any>(body)

    assert(response.error?.title == "Unknown server response")
  }
}
