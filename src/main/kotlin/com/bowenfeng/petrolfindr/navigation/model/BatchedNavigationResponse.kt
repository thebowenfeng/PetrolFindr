package com.bowenfeng.petrolfindr.navigation.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class BatchedNavigationResponse(
    val code: String,
    val distances: List<List<Double?>> = emptyList(),
    val message: String? = null,
    @JsonProperty("data_version")
    val dataVersion: String? = null,
)
