/*
 * The MIT License
 *
 * Copyright (c) 2023- High-Mobility GmbH (https://high-mobility.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.highmobility.hmkitfleet.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RequestClearanceResponse(
    val vin: String,
    val status: ClearanceStatus.Status,
    /**
     * Filled when status is ERROR
     */
    val description: String? = null
)

@Serializable
data class ClearanceStatus(
    val vin: String,
    val status: Status,
    val brand: Brand? = null,
    val changelog: List<ChangeLogItem> = emptyList()
) {
    @Serializable
    enum class Status {
        @SerialName("approved")
        APPROVED,

        @SerialName("pending")
        PENDING,

        // Error can only happen during requestClearance
        @SerialName("error")
        ERROR,

        @SerialName("revoking")
        REVOKING,

        @SerialName("revoked")
        REVOKED,

        @SerialName("rejected")
        REJECTED,

        @SerialName("canceling")
        CANCELING,

        @SerialName("canceled")
        CANCELED
    }
}

@Serializable
data class ChangeLogItem(val status: ClearanceStatus.Status, val timestamp: String)
