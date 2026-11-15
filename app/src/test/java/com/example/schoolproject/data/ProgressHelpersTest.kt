package com.example.schoolproject.data

import com.example.schoolproject.data.helpers.*
import com.example.schoolproject.data.models.BreathingSessionState
import com.example.schoolproject.data.remote.ActivityDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ProgressHelpersTest {
    private val today = LocalDate.of(2026, 11, 15)
    private fun stamp(day: LocalDate) = day.atTime(12, 0).atZone(ZoneId.systemDefault()).toOffsetDateTime().toString()

    @Test fun streakCountsDaysNotNumberOfEntries() {
        assertEquals(3, checkInStreak(listOf(stamp(today), stamp(today), stamp(today.minusDays(1)), stamp(today.minusDays(2))), today))
        assertEquals(2, checkInStreak(listOf(stamp(today.minusDays(1)), stamp(today.minusDays(2))), today))
        assertEquals(0, checkInStreak(listOf(stamp(today.minusDays(2)), null, "bad date"), today))
    }

    @Test fun slotsRejectElapsedOrInvalidTimes() {
        val now = LocalDateTime.of(2026, 11, 15, 15, 0)
        assertFalse(isFutureSlot("2026-11-15", "2:00 PM", now))
        assertTrue(isFutureSlot("2026-11-15", "4:00 PM", now))
        assertTrue(isFutureSlot("2026-11-16", "09:00", now))
        assertFalse(isFutureSlot("2026-11-16", "unknown", now))
    }

    @Test fun breathingPromptsFollowFourSevenEightCycle() {
        val state = BreathingSessionState(isPlaying = true)
        assertEquals("Breathe in · 4", state.breathingInstruction)
        assertEquals("Hold · 7", state.copy(secondsRemaining = 116).breathingInstruction)
        assertEquals("Breathe out · 8", state.copy(secondsRemaining = 109).breathingInstruction)
        assertEquals("Breathe in · 4", state.copy(secondsRemaining = 101).breathingInstruction)
        assertEquals("Session complete", state.copy(secondsRemaining = 0).breathingInstruction)
    }

    @Test fun unansweredStressIsExplicitNullInsteadOfDatabaseDefaultSixty() {
        val json = Json.encodeToString(ActivityDto("id", "user", "BREATHING_478", 120))
        assertTrue(json.contains("\"stress_decrease_percent\":null"))
    }
}
