package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
data class AuthToken(
    @SerialName("auth_token")
    val auth_token:String,
    @SerialName("valid_from")
    val valid_from:String,
    @SerialName("valid_until")
    val valid_until:String
)