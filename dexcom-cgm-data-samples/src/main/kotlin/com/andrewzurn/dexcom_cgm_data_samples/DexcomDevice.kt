package com.andrewzurn.dexcom_cgm_data_samples

data class DexcomDevice(
    val transmitterId: String,
    val transmitterGeneration: TransmitterGeneration,
    val displayDevice: DisplayDevice
)
