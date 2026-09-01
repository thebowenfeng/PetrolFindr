package com.bowenfeng.petrolfindr.search.model

enum class FuelType {
    E10,
    U91,
    DIESEL,
    PremDSL,
    U95,
    U98,
    LPG,
    TruckDSL,
    E85,
    BIODIESEL,
    AdBlue
}

data class Price(
    val type: FuelType,
    val updated: Long,
    val amount: Double,
)
