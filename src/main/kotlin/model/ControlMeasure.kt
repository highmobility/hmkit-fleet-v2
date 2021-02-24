package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ControlMeasure

@Serializable
@SerialName("odometer")
data class Odometer(val value: Long, val unit: Length) : ControlMeasure() {

    @Serializable
    enum class Length {
        @SerialName("kilometers")
        KILOMETERS,

        @SerialName("miles")
        MILES
    }
}