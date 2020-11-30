package model

sealed class ControlMeasure {
    data class Odometer(val length: Long, val unit: Length) : ControlMeasure() {
        enum class Length {
            KILOMETERS, MILES
        }
    }
}