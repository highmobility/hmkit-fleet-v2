package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClearanceStatus(val vin: String, val status: Status) {
    @Serializable
    enum class Status {
        @SerialName("pending")
        PENDING,

        @SerialName("approved")
        APPROVED,

        @SerialName("failed")
        FAILED
    }
}