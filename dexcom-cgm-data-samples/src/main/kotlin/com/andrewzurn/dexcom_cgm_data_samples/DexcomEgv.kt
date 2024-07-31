package com.andrewzurn.dexcom_cgm_data_samples

import com.fasterxml.jackson.annotation.JsonValue
import java.time.OffsetDateTime

/*
 * Classes created from publicly documented information on developer.dexcom.com (particularlly the EGV documentation
 * that can be found here: https://developer.dexcom.com/docs/dexcomv3/operation/getEstimatedGlucoseValuesV3).
 */
data class DexcomEgvWrapper(
    val recordType: String,
    val recordVersion: String,
    val userId: String,
    val records: List<DexcomEgv>
)

data class DexcomEgv(
    val recordId: String,
    val systemTime: OffsetDateTime,
    val displayTime: OffsetDateTime,
    val value: Int,
    val status: StatusType?,
    val trend: TrendRate,
    val trendRate: Double,
    val displayDevice: DisplayDevice,
    val transmitterGeneration: TransmitterGeneration,
    val transmitterId: String,
    val transmitterTicks: String
) {
    val device = DexcomDevice(transmitterId, transmitterGeneration, displayDevice)
}

enum class StatusType(@JsonValue val value: String) {
    UNKNOWN("unknown"),
    HIGH("high"),
    OW("low"),
    OK("ok")
}

enum class TrendRate(@JsonValue val value: String) {
    NONE("none"),
    UNKNOWN("unknown"),
    DOUBLEUP("doubleUp"),
    SINGLEUP("singleUp"),
    FORTYFIVEUP("fortyFiveUp"),
    FLAT("flat"),
    FORTYFIVEDOWN("fortyFiveDown"),
    SINGLEDOWN("singleDown"),
    DOUBLEDOWN("doubleDown"),
    NOTCOMPUTABLE("notComputable"),
    RATEOUTOFRANGE("rateOutOfRange")
}

enum class DisplayDevice(@JsonValue val value: String) {
    UNKNOWN("unknown"),
    IOS("iOS"),
    ANDROID("android"),
    RECEIVER("receiver")
}

enum class TransmitterGeneration(@JsonValue val value: String) {
    UNKNOWN("unknown"),
    G4("g4"),
    G5("g5"),
    G6("g6"),
    G7("g7")
}
