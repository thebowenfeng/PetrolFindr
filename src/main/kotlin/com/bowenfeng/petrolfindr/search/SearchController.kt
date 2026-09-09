package com.bowenfeng.petrolfindr.search

import com.bowenfeng.petrolfindr.common.Coordinate
import com.bowenfeng.petrolfindr.location.LocationFilterService
import com.bowenfeng.petrolfindr.search.model.BoundingBox
import com.bowenfeng.petrolfindr.search.model.FuelType
import com.bowenfeng.petrolfindr.search.model.GasStation
import com.bowenfeng.petrolfindr.search.model.StationSearchByLocationRequest
import com.bowenfeng.petrolfindr.search.model.StationSearchRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import kotlin.time.Duration.Companion.milliseconds

@RestController
@RequestMapping("/search")
@CrossOrigin(origins = ["*"])
class SearchController(
    val gasStationStore: GasStationStore,
    val locationFilterService: LocationFilterService
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

    @PostMapping("/stations/location")
    suspend fun searchGasStationsByLocation(@RequestBody searchRequest: StationSearchByLocationRequest): List<GasStation> =
        locationFilterService.filterStationsByLocation(searchRequest.allStations, searchRequest.location, searchRequest.fuelType ?: FuelType.U91)

    @GetMapping("/stations/location/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamGasStationsByLocation(
        @RequestParam topRightLat: Double,
        @RequestParam topRightLng: Double,
        @RequestParam bottomLeftLat: Double,
        @RequestParam bottomLeftLng: Double,
        @RequestParam locationLat: Double,
        @RequestParam locationLng: Double,
        @RequestParam fuelType: FuelType?
    ): Flow<ServerSentEvent<List<GasStation>>> = flow {
        val allStations = gasStationStore.getStations(
            BoundingBox(
                topRight = Coordinate(topRightLat, topRightLng),
                bottomLeft = Coordinate(bottomLeftLat, bottomLeftLng),
            ),
            previous = null
        )
        emit(ServerSentEvent.builder<List<GasStation>>().event("all-stations").data(allStations).build())

        emit(ServerSentEvent.builder<List<GasStation>>().event("filtered-stations").data(
            locationFilterService.filterStationsByLocation(allStations, Coordinate(locationLat, locationLng), fuelType ?: FuelType.U91)
        ).build())
    }
}
