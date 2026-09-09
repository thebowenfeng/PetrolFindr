package com.bowenfeng.petrolfindr.search.model

import com.bowenfeng.petrolfindr.common.Coordinate

data class StationSearchByLocationRequest (
    val allStations: List<GasStation>,
    val location: Coordinate,
    val fuelType: FuelType?,
)