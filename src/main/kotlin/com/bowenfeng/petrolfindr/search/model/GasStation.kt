package com.bowenfeng.petrolfindr.search.model

import com.bowenfeng.petrolfindr.common.Coordinate

data class GasStation(
    val id: String,
    val name: String,
    val location: Coordinate,
    val address: String,
    val tradingHours: List<TradingHour>?,
    val icon: String,
    val prices: List<Price>,
)
