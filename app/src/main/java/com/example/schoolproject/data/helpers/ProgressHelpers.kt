package com.example.schoolproject.data.helpers

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

fun savedDate(timestamp: String?): LocalDate? = try {
    OffsetDateTime.parse(timestamp).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
} catch (_: Exception) { null }

// Multiple check-ins on one day count as one streak day. Yesterday's streak is
// still active until the user has had the chance to check in today.
fun checkInStreak(timestamps: List<String?>, today: LocalDate = LocalDate.now()): Int {
    val dates = timestamps.mapNotNull { savedDate(it) }.toSet()
    var day = if (today in dates) today else today.minusDays(1)
    var count = 0
    while (day in dates) {
        count++
        day = day.minusDays(1)
    }
    return count
}

fun displaySavedTime(timestamp: String?): String = try {
    OffsetDateTime.parse(timestamp).atZoneSameInstant(ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", java.util.Locale.ENGLISH))
} catch (_: Exception) { "Saved session" }

fun isFutureSlot(date: String, time: String, now: java.time.LocalDateTime = java.time.LocalDateTime.now()): Boolean = try {
    val clock = try {
        java.time.LocalTime.parse(time.uppercase(java.util.Locale.ENGLISH),
            java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH))
    } catch (_: Exception) { java.time.LocalTime.parse(time) }
    java.time.LocalDateTime.of(LocalDate.parse(date), clock).isAfter(now)
} catch (_: Exception) { false }
