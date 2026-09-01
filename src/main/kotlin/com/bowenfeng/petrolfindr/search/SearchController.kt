package com.bowenfeng.petrolfindr.search

import com.bowenfeng.petrolfindr.search.model.BoundingBox
import com.bowenfeng.petrolfindr.search.model.GasStation
import com.bowenfeng.petrolfindr.search.model.StationSearchRequest
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/search")
@CrossOrigin(origins = ["*"])
class SearchController(
    val gasStationStore: GasStationStore
) {
    @PostMapping("/stations")
    suspend fun searchGasStations(@RequestBody searchRequest: StationSearchRequest): List<GasStation> = 
        gasStationStore.getStations(
            BoundingBox(
                topRight = searchRequest.topRight,
                bottomLeft = searchRequest.bottomLeft,
            ),
            previous = null
        )
}
