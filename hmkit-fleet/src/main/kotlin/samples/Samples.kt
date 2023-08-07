package com.highmobility.hmkitfleet.samples

import com.highmobility.hmkitfleet.HMKitFleet
import com.highmobility.hmkitfleet.ServiceAccountApiConfiguration

fun createHmkitInMain() {
    val fleet = HMKitFleet(
        ServiceAccountApiConfiguration(
            "serviceAccountApiKey",
            "serviceAccountPrivateKey",
            "clientCertificate",
            "clientPrivateKey",
            "oauthClientId",
            "oauthClientSecret"
        ),
        HMKitFleet.Environment.SANDBOX
    )
}