package com.example.schoolproject

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.schoolproject.data.models.*
import com.example.schoolproject.data.remote.ActivityDto
import com.example.schoolproject.ui.screens.HistoryContent
import com.example.schoolproject.ui.screens.CounsellorCard
import com.example.schoolproject.ui.theme.SchoolProjectTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {
    @get:Rule val compose = createComposeRule()
    private val counsellor = Counsellor("c1", "Demo counsellor", "Counsellor", "Study stress", "Available", true, 0xFFE8F5E9)

    @Test fun journalsBeyondTwentyRemainReadableAndTabsShowBookings() {
        val journals = (1..25).map {
            ActivityDto("journal-$it", "user", "GRATITUDE_JOURNAL", 0, "Full journal entry $it")
        }
        val booking = Booking("b1", counsellor, "2020-01-01", "9:00 AM", BookingStatus.UPCOMING)
        compose.setContent {
            SchoolProjectTheme { HistoryContent(journals, listOf(booking), false, true, {}, {}) }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Full journal entry 25"))
        compose.onNodeWithText("Full journal entry 25").assertIsDisplayed()
        compose.onNodeWithText("Bookings").performClick()
        compose.onNodeWithText("Demo counsellor").assertIsDisplayed()
        compose.onNodeWithText("Past appointment · awaiting status update").assertIsDisplayed()
    }

    @Test fun breathingWithoutReflectionAppearsInActivitiesButNotJournals() {
        val breathing = ActivityDto("session", "user", "BREATHING_478", 120)
        compose.setContent {
            SchoolProjectTheme { HistoryContent(listOf(breathing), emptyList(), false, true, {}, {}) }
        }
        compose.onNodeWithText("No saved journals yet. Save a gratitude entry or a breathing reflection to see it here.").assertExists()
        compose.onNodeWithText("Activities").performClick()
        compose.onNodeWithText("Breathing session").assertIsDisplayed()
        compose.onNodeWithText("Duration: 2 min").assertIsDisplayed()
    }

    @Test fun counsellorWithNoSlotsCanStillBeSelectedAndExplainsAvailability() {
        var selected = false
        compose.setContent {
            SchoolProjectTheme { CounsellorCard(counsellor, 0) { selected = true } }
        }
        compose.onNodeWithText("No times published yet").assertExists()
        compose.onNodeWithText("Select").assertIsEnabled().performClick()
        assertTrue(selected)
    }
}
