package com.kaos.evcryptoscanner.data.api.dto

import com.google.gson.annotations.SerializedName

data class ScanResponseDto(
    @SerializedName("state") val state: String?,
    @SerializedName("coin") val coin: String?,
    @SerializedName("readiness") val readiness: Int?,
    @SerializedName("formatted_output") val formattedOutput: String?,
    @SerializedName("entry") val entry: Double?,
    @SerializedName("stop") val stop: Double?,
    @SerializedName("tp1") val tp1: Double?,
    @SerializedName("tp2") val tp2: Double?,
    @SerializedName("btc_regime") val btcRegime: String?,
    @SerializedName("timestamp") val timestamp: Long?
)

data class HealthResponseDto(
    @SerializedName("status") val status: String?,
    @SerializedName("timestamp") val timestamp: Long?
)

data class DeviceTokenDto(
    @SerializedName("token") val token: String
)

data class ScanTriggerResponseDto(
    @SerializedName("message") val message: String?,
    @SerializedName("success") val success: Boolean?
)
