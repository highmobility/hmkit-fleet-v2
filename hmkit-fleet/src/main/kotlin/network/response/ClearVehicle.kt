package network.response

class ClearVehicle(val vin: String, val status: Status) {
    enum class Status { PENDING, APPROVED, FAILED }
}