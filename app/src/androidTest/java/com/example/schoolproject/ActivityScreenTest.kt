package com.example.schoolproject

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.schoolproject.data.models.*
import com.example.schoolproject.ui.screens.ActivityContent
import com.example.schoolproject.ui.theme.SchoolProjectTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ActivityScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun incompleteTimerCannotBeSubmitted() {
        compose.setContent {
            SchoolProjectTheme {
                ActivityContent(BreathingSessionState(), false, {}, {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("Start breathing").assertExists()
        compose.onNodeWithText("Save session").performScrollTo().assertIsNotEnabled()
    }

    @Test fun completedSessionHasSaveConfirmationAndDoneAction() {
        var done = false
        compose.setContent {
            var state by remember { mutableStateOf(BreathingSessionState(secondsRemaining = 0)) }
            SchoolProjectTheme {
                ActivityContent(state, false, {}, {}, {}, {},
                    onSave = { state = state.copy(isSaved = true) }, onBack = { done = true })
            }
        }
        compose.onNodeWithText("Session complete — save it below").assertExists()
        compose.onNodeWithText("Save session").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText("Activity saved").performScrollTo().assertExists()
        val folder = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        File(folder, "activity-saved.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("Done").performScrollTo().performClick()
        assertTrue(done)
    }

    @Test fun gratitudeRequiresNoteThenAllowsSave() {
        compose.setContent {
            var state by remember { mutableStateOf(BreathingSessionState(activityType = ActivityType.GRATITUDE_JOURNAL)) }
            SchoolProjectTheme {
                ActivityContent(state, false, {}, {}, { state = state.copy(gratitudeNote = it) }, {},
                    { state = state.copy(isSaved = true) }, {})
            }
        }
        compose.onNodeWithText("Save journal entry").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Gratitude note").performScrollTo().performTextInput("My family")
        compose.onNodeWithText("Save journal entry").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText("Activity saved").assertExists()
        compose.onNodeWithText("My family").assertExists()
    }

    @Test fun resettingAnUnfinishedSessionAsksBeforeDiscarding() {
        var reset = false
        compose.setContent {
            SchoolProjectTheme {
                ActivityContent(BreathingSessionState(secondsRemaining = 60), false, {}, { reset = true }, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("Reset timer").performScrollTo().performClick()
        compose.onNodeWithText("Start over?").assertExists()
        compose.onNodeWithText("Start over").performClick()
        assertTrue(reset)
    }
}
