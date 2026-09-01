package com.bowenfeng.petrolfindr.search.model

import com.bowenfeng.petrolfindr.common.Coordinate

data class BoundingBox(
    val topRight: Coordinate,
    val bottomLeft: Coordinate,
)
