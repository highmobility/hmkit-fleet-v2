package network

data class Response<T>(
    val response: T? = null,
    val error: network.response.Error? = null
)