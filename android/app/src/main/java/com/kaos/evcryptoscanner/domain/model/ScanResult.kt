package com.kaos.evcryptoscanner.domain.model

data class ScanResult(
    val state: TradeState,
    val coin: String?,
    val readiness: Int?,
    val formattedOutput: String,
    val entry: Double?,
    val stop: Double?,
    val tp1: Double?,
    val tp2: Double?,
    val btcRegime: String?,
    val timestamp: Long
)

enum class TradeState {
    BUY,
    WAIT,
    NO_TRADE,
    UNKNOWN;

    companion object {
        fun fromString(value: String?): TradeState {
            return when (value?.uppercase()) {
                "BUY" -> BUY
                "SETUP FORMING — WAIT", "WAIT" -> WAIT
                "NO TRADE" -> NO_TRADE
                else -> UNKNOWN
            }
        }
    }

    fun displayName(): String = when (this) {
        BUY -> "BUY"
        WAIT -> "SETUP FORMING — WAIT"
        NO_TRADE -> "NO TRADE"
        UNKNOWN -> "UNKNOWN"
    }
}

data class HealthResponse(
    val status: String,
    val timestamp: Long
)
