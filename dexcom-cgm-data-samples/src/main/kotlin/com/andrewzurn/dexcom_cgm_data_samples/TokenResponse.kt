package com.andrewzurn.dexcom_cgm_data_samples

import com.fasterxml.jackson.annotation.JsonProperty

data class TokenResponse(@JsonProperty("access_token") val accessToken: String)
