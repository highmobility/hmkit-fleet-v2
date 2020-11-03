package network

class ClearVehicleResponse(val vin: String, val status: Status, val error: Error? = null) {
    enum class Status { PENDING, APPROVED, FAILED }
    class Error(val title: String?, val description: String, val source: String?)
}