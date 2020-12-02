package network

import model.AuthToken
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class CacheTest {
    private val notExpiredToken = AuthToken(
        "",
        LocalDateTime.now().minusMinutes(30),
        LocalDateTime.now().plusMinutes(30)
    )

    private val expiredToken = AuthToken(
        "",
        LocalDateTime.now().minusMinutes(120),
        LocalDateTime.now().minusMinutes(60)
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