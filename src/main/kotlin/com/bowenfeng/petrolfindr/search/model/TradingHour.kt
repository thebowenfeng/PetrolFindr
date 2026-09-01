package com.bowenfeng.petrolfindr.search.model

data class TradingHour(
    val startMinute: Int,
    val endMinute: Int,
) {
    companion object {
        fun minutesSinceStartOfDay(time: String): Int {
            val (hours, minutes) = time.split(":").map(String::toInt)

            require(hours in 0..23 && minutes in 0..59) {
                "Invalid time: $time"
            }

            return hours * 60 + minutes
        }
    }
}
