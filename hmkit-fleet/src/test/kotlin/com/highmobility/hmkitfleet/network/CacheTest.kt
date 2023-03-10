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

import com.highmobility.hmkitfleet.model.AuthToken
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock.systemUTC
import java.time.LocalDateTime

internal class CacheTest {
    private val notExpiredToken = AuthToken(
        "",
        LocalDateTime.now(systemUTC()).minusMinutes(30),
        LocalDateTime.now(systemUTC()).plusMinutes(30)
    )

    private val expiredToken = AuthToken(
        "",
        LocalDateTime.now(systemUTC()).minusMinutes(120),
        LocalDateTime.now(systemUTC()).minusMinutes(60)
    )

    @Test
    fun testAuthTokenExpiration() {
        assertTrue(notExpiredToken.isExpired() == false)
        assertTrue(expiredToken.isExpired() == true)
    }

    @Test
    fun returnsNullIfAuthTokenExpired() {
        val cache = Cache()
        cache.authToken = expiredToken

        assertTrue(cache.authToken == null)

        cache.authToken = notExpiredToken
        assertTrue(cache.authToken == notExpiredToken)
    }
}