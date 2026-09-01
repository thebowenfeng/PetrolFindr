package com.bowenfeng.petrolfindr.search.model

import com.bowenfeng.petrolfindr.common.Coordinate

data class StationSearchRequest(
    val topRight: Coordinate,
    val bottomLeft: Coordinate,
)
