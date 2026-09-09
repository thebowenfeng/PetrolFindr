package com.bowenfeng.petrolfindr.location

import com.bowenfeng.petrolfindr.common.Coordinate
import com.bowenfeng.petrolfindr.navigation.NavigationService
import com.bowenfeng.petrolfindr.search.model.FuelType
import com.bowenfeng.petrolfindr.search.model.GasStation
import org.springframework.stereotype.Service
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Service
class LocationFilterService(private val navigationService: NavigationService) {
    private companion object {
        val DISTANCE_BUCKET = listOf(1000.0, 2000.0, 4000.0, 8000.0, 16000.0)
        const val EARTH_RADIUS_METRES = 6_371_008.8
    }

    private data class StationCandidate(val station: GasStation, val distance: Double, val price: Double)

    suspend fun filterStationsByLocation(stations: List<GasStation>, location: Coordinate, fuelType: FuelType): List<GasStation> {
        val filteredStations = stations.filter { it.prices.any { price -> price.type == fuelType } }
            .filter { calculateStraightLineDistance(it.location, location) < DISTANCE_BUCKET[DISTANCE_BUCKET.size - 1] }
        val distances = navigationService.batchedNavigate(location, filteredStations.map { it.location }).distances[0]

        val distanceMap = mutableMapOf<Double, StationCandidate?>()
        DISTANCE_BUCKET.forEach { distanceMap[it] = null }

        filteredStations.forEachIndexed { index, station ->
            val distance = distances[index] ?: return@forEachIndexed
            val price = station.prices.find { price -> price.type == fuelType }?.amount ?: 9999.0
            val candidate = StationCandidate(station, distance, price)
            distanceMap.forEach { (distanceBucket, current) ->
                if (distance <= distanceBucket && (
                    current == null ||
                        price < current.price ||
                        (price == current.price && distance < current.distance)
                    )) {
                    distanceMap[distanceBucket] = candidate
                }
            }
        }

        return distanceMap.values.mapNotNull { it?.station }.associateBy { it.id }.values.toList()
    }

    /** Returns the great-circle distance between two coordinates, in metres. */
    private fun calculateStraightLineDistance(start: Coordinate, destination: Coordinate): Double {
        val startLatitude = Math.toRadians(start.latitude)
        val destinationLatitude = Math.toRadians(destination.latitude)
        val latitudeDifference = destinationLatitude - startLatitude
        val longitudeDifference = Math.toRadians(destination.longitude - start.longitude)

        val haversine =
            sin(latitudeDifference / 2).let { it * it } +
                cos(startLatitude) * cos(destinationLatitude) *
                sin(longitudeDifference / 2).let { it * it }
        val boundedHaversine = haversine.coerceIn(0.0, 1.0)
        val angularDistance = 2 * atan2(sqrt(boundedHaversine), sqrt(1 - boundedHaversine))

        return EARTH_RADIUS_METRES * angularDistance
    }
}
