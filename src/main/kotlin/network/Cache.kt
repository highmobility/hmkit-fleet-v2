package network

import model.AuthToken

internal class Cache {
    var authToken: AuthToken? = null
        get() {
            if (field?.isExpired() == true) return null
            return field
        }
}