package com.bowenfeng.petrolfindr.search

import com.bowenfeng.petrolfindr.common.Coordinate
import com.bowenfeng.petrolfindr.common.RandomUtils
import com.bowenfeng.petrolfindr.search.model.BoundingBox
import com.bowenfeng.petrolfindr.search.model.FuelType
import com.bowenfeng.petrolfindr.search.model.GasStation
import com.bowenfeng.petrolfindr.search.model.Price
import com.bowenfeng.petrolfindr.search.model.TradingHour
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.jackson3.jackson
import org.springframework.stereotype.Repository
import tools.jackson.databind.JsonNode

@Repository
class GasStationStore {
    val API_ROOT = "https://petrolspy.com.au/webservice-1"
    val DEVICE_NAME_HEADER = "X-ps-fp"
    val PHYSICAL_DEVICE_FLAG_HEADER = "X-ps-dv"
    val INSTALLATION_ID_HEADER = "X-ps-did"
    val DEVICE_MAP = mapOf(
        "Pixel 9" to "${RandomUtils.generateAndroidIdLike()}-${System.currentTimeMillis() - (10000..100000).random()}",
        "Pixel 8" to "${RandomUtils.generateAndroidIdLike()}-${System.currentTimeMillis() - (10000..100000).random()}",
        "iPhone" to "${RandomUtils.generateAndroidIdLike()}-${System.currentTimeMillis() - (10000..100000).random()}",
        "SM-S928B" to "${RandomUtils.generateAndroidIdLike()}-${System.currentTimeMillis() - (10000..100000).random()}"
    )

    val client = HttpClient(CIO) {
        install(ContentEncoding) {
            gzip()
        }

        install(ContentNegotiation) {
            jackson()
        }
    }

    fun buildInstallationId(deviceName: String): String {
        val now = System.currentTimeMillis()
        return "android-${DEVICE_MAP[deviceName]}-$now"
    }

    fun setHeaders(builder: HttpRequestBuilder) {
        val device = DEVICE_MAP.entries.random().key
        builder.header(DEVICE_NAME_HEADER, device)
        builder.header(PHYSICAL_DEVICE_FLAG_HEADER, true)
        builder.header(INSTALLATION_ID_HEADER, buildInstallationId(device))
    }

    suspend fun getStations(current: BoundingBox, previous: BoundingBox?): List<GasStation> {
        val response: JsonNode = client.get("$API_ROOT/station/box") {
            parameter("ts", System.currentTimeMillis())
            parameter("neLat", current.topRight.latitude)
            parameter("neLng", current.topRight.longitude)
            parameter("swLat", current.bottomLeft.latitude)
            parameter("swLng", current.bottomLeft.longitude)
            if (previous != null) {
                parameter("neLatOld", previous.topRight.latitude)
                parameter("neLngOld", previous.topRight.longitude)
                parameter("swLatOld", previous.bottomLeft.latitude)
                parameter("swLngOld", previous.bottomLeft.longitude)
            }
            setHeaders(this)
        }.body()

        return response.path("message").path("list").asArray().values().map {
            GasStation(
                id = it.path("id").asString(),
                name = it.path("name").asString(),
                location = Coordinate(
                    latitude = it.path("location").path("y").asDouble(),
                    longitude = it.path("location").path("x").asDouble()
                ),
                address = it.path("address").asString(),
                tradingHours = if (!it.path("tradingHours").isMissingNode) it.path("tradingHours").asArray().values().mapNotNull { it2 ->
                    if (!it2.isArray || it2.size() != 2) {
                        return@mapNotNull null
                    }

                    TradingHour(
                        startMinute = TradingHour.minutesSinceStartOfDay(it2.asArray().get(0).asString()),
                        endMinute = TradingHour.minutesSinceStartOfDay(it2.asArray().get(1).asString()),
                    )
                } else null,
                icon = it.path("brandIcon").asString().ifEmpty { "other.png" },
                prices = it.path("prices").properties().map { (fieldName, fieldValue) ->
                    Price(
                        type = FuelType.valueOf(fieldName),
                        updated = fieldValue.path("updated").asLong(),
                        amount = fieldValue.path("amount").asDouble(),
                    )
                }
            )
        }
    }
}
