package com.bowenfeng.petrolfindr.location

import com.bowenfeng.petrolfindr.common.Coordinate
import com.bowenfeng.petrolfindr.navigation.NavigationService
import com.bowenfeng.petrolfindr.navigation.model.NavigationResponse
import com.bowenfeng.petrolfindr.navigation.model.Route
import com.bowenfeng.petrolfindr.search.model.FuelType
import com.bowenfeng.petrolfindr.search.model.GasStation
import com.bowenfeng.petrolfindr.search.model.Price
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals

class LocationFilterServiceTests {
    private val navigationService = mock(NavigationService::class.java)
    private val service = LocationFilterService(navigationService)
    private val location = Coordinate(-37.8136, 144.9631)

    @Test
    fun `selects a cheaper station even when it is farther away and appears later`() = runBlocking {
        val expensive = station("expensive", 200.0, 144.964)
        val cheap = station("cheap", 180.0, 144.965)
        stubDistance(expensive, 500.0)
        stubDistance(cheap, 900.0)

        assertEquals(listOf(cheap), service.filterStationsByLocation(listOf(expensive, cheap), location, FuelType.U91))
        assertEquals(listOf(cheap), service.filterStationsByLocation(listOf(cheap, expensive), location, FuelType.U91))
    }

    @Test
    fun `selects the closer station when prices tie regardless of input order`() = runBlocking {
        val farther = station("farther", 180.0, 144.964)
        val closer = station("closer", 180.0, 144.965)
        stubDistance(farther, 900.0)
        stubDistance(closer, 500.0)

        assertEquals(listOf(closer), service.filterStationsByLocation(listOf(farther, closer), location, FuelType.U91))
        assertEquals(listOf(closer), service.filterStationsByLocation(listOf(closer, farther), location, FuelType.U91))
    }

    @Test
    fun `keeps the first station when both price and distance tie`() = runBlocking {
        val first = station("first", 180.0, 144.964)
        val second = station("second", 180.0, 144.965)
        stubDistance(first, 500.0)
        stubDistance(second, 500.0)

        assertEquals(listOf(first), service.filterStationsByLocation(listOf(first, second), location, FuelType.U91))
        assertEquals(listOf(second), service.filterStationsByLocation(listOf(second, first), location, FuelType.U91))
    }

    @Test
    fun `returns a station only once when it wins every bucket`() = runBlocking {
        val station = station("winner", 180.0, 144.964)
        stubDistance(station, 500.0)

        assertEquals(listOf(station), service.filterStationsByLocation(listOf(station), location, FuelType.U91))
    }

    @Test
    fun `returns distinct winners in ascending bucket order`() = runBlocking {
        val nearby = station("nearby", 200.0, 144.964)
        val middle = station("middle", 190.0, 144.965)
        val far = station("far", 180.0, 144.966)
        stubDistance(nearby, 800.0)
        stubDistance(middle, 1500.0)
        stubDistance(far, 3000.0)

        assertEquals(
            listOf(nearby, middle, far),
            service.filterStationsByLocation(listOf(far, nearby, middle), location, FuelType.U91),
        )
    }

    @Test
    fun `deduplicates by ID keeping the last bucket value in the first ID position`() = runBlocking {
        val nearby = station("same-id", 200.0, 144.964)
        val middle = station("other-id", 190.0, 144.965)
        val far = station("same-id", 180.0, 144.966)
        stubDistance(nearby, 800.0)
        stubDistance(middle, 1500.0)
        stubDistance(far, 3000.0)

        assertEquals(
            listOf(far, middle),
            service.filterStationsByLocation(listOf(nearby, middle, far), location, FuelType.U91),
        )
    }

    @Test
    fun `compares prices for the requested fuel type`() = runBlocking {
        val first = station("first", 180.0, 144.964).copy(
            prices = listOf(Price(FuelType.U91, 0, 180.0), Price(FuelType.U95, 0, 220.0)),
        )
        val second = station("second", 190.0, 144.965).copy(
            prices = listOf(Price(FuelType.U91, 0, 190.0), Price(FuelType.U95, 0, 210.0)),
        )
        stubDistance(first, 500.0)
        stubDistance(second, 900.0)

        assertEquals(listOf(second), service.filterStationsByLocation(listOf(first, second), location, FuelType.U95))
    }

    private fun station(id: String, price: Double, longitude: Double) = GasStation(
        id = id,
        name = id,
        location = Coordinate(location.latitude, longitude),
        address = "",
        tradingHours = null,
        icon = "",
        prices = listOf(Price(FuelType.U91, 0, price)),
    )

    private suspend fun stubDistance(station: GasStation, distance: Double) {
        `when`(navigationService.navigate(location, station.location)).thenReturn(
            NavigationResponse(
                code = "Ok",
                routes = listOf(
                    Route(
                        legs = emptyList(),
                        weightName = "routability",
                        geometry = "",
                        weight = 0.0,
                        duration = 0.0,
                        distance = distance,
                    ),
                ),
            ),
        )
    }
}
