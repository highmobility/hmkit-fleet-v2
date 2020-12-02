package network

import kotlinx.serialization.Serializable

@Serializable
data class Error(
    val title: String,
    val detail: String? = null,
    val source: String? = null
)