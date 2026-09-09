package com.bowenfeng.petrolfindr.navigation

import com.bowenfeng.petrolfindr.common.Coordinate
import com.bowenfeng.petrolfindr.navigation.model.BatchedNavigationResponse
import com.bowenfeng.petrolfindr.navigation.model.NavigationResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson3.jackson
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.milliseconds

@Service
class NavigationService {
    private companion object {
        val API_ROOT = "https://router.project-osrm.org/route/v1/driving"
        val TABLE_API_ROOT = "https://router.project-osrm.org/table/v1/driving"

        const val MIN_REQUEST_INTERVAL_NANOS = 100_000_000L
        const val NANOS_PER_MILLISECOND = 1_000_000L

        val CHROME_MAJOR_VERSION_RANGE = 143..152
    }

    private val rateLimitMutex = Mutex()
    private var lastRequestStartedAtNanos: Long? = null

    val client = HttpClient(Java) {
        install(ContentEncoding) {
            gzip()
        }

        install(ContentNegotiation) {
            jackson()
        }
    }

    suspend fun navigate(start: Coordinate, destination: Coordinate): NavigationResponse {
        waitForRequestSlot()

        return client.get(
            "$API_ROOT/${start.longitude},${start.latitude};${destination.longitude},${destination.latitude}"
        ) {
            parameter("alternatives", true)
            setBrowserHeaders(this)
        }.body()
    }

    suspend fun batchedNavigate(start: Coordinate, destinations: List<Coordinate>): BatchedNavigationResponse {
        if (destinations.isEmpty()) {
            return BatchedNavigationResponse(code = "Ok", distances = listOf(emptyList()))
        }

        val coordinates = (listOf(start) + destinations).joinToString(";") {
            "${it.longitude},${it.latitude}"
        }

        waitForRequestSlot()

        return client.get("$TABLE_API_ROOT/$coordinates") {
            parameter("sources", 0)
            parameter("destinations", destinations.indices.joinToString(";") { (it + 1).toString() })
            parameter("annotations", "distance")
            parameter("skip_waypoints", true)
            setBrowserHeaders(this)
        }.body()
    }

    private suspend fun waitForRequestSlot() {
        rateLimitMutex.withLock {
            val previousRequestStartedAtNanos = lastRequestStartedAtNanos

            if (previousRequestStartedAtNanos != null) {
                val elapsedNanos = System.nanoTime() - previousRequestStartedAtNanos
                val remainingNanos = MIN_REQUEST_INTERVAL_NANOS - elapsedNanos

                if (remainingNanos > 0) {
                    val delayMillis =
                        (remainingNanos + NANOS_PER_MILLISECOND - 1) / NANOS_PER_MILLISECOND
                    delay(delayMillis.milliseconds)
                }
            }

            this.lastRequestStartedAtNanos = System.nanoTime()
        }
    }

    private fun setBrowserHeaders(builder: HttpRequestBuilder) {
        val chromeMajorVersion = CHROME_MAJOR_VERSION_RANGE.random()
        val (platform, platformToken) = when ((0..2).random()) {
            0 -> {
                val macVersion = "${(11..15).random()}_${(0..6).random()}_${(0..9).random()}"
                "macOS" to "Macintosh; Intel Mac OS X $macVersion"
            }
            1 -> "Windows" to "Windows NT 10.0; Win64; x64"
            else -> "Linux" to "X11; Linux x86_64"
        }
        val webKitVersion = "${(537..605).random()}.${(1..50).random()}"
        val userAgent =
            "Mozilla/5.0 ($platformToken) AppleWebKit/$webKitVersion (KHTML, like Gecko) " +
                "Chrome/$chromeMajorVersion.0.0.0 Safari/$webKitVersion"

        builder.header(HttpHeaders.Accept, "*/*")
        builder.header(HttpHeaders.AcceptLanguage, "en,zh;q=0.9,en-GB;q=0.8,en-US;q=0.7")
        builder.header(HttpHeaders.CacheControl, "no-cache")
        builder.header(HttpHeaders.Pragma, "no-cache")
        builder.header(HttpHeaders.UserAgent, userAgent)
        builder.header("Priority", "u=1, i")
        builder.header(
            "Sec-CH-UA",
            "\"Chromium\";v=\"$chromeMajorVersion\", \"Not_A Brand\";v=\"24\", \"Google Chrome\";v=\"$chromeMajorVersion\""
        )
        builder.header("Sec-CH-UA-Mobile", "?0")
        builder.header("Sec-CH-UA-Platform", "\"$platform\"")
        builder.header("Sec-Fetch-Dest", "empty")
        builder.header("Sec-Fetch-Mode", "cors")
        builder.header("Sec-Fetch-Site", "cross-site")
        builder.header(HttpHeaders.Referrer, "https://maplibre.org/")
    }
}
