package network

data class Response<T>(
    val response: T? = null,
    val error: Error? = null
)