package com.example.schoolproject

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import com.example.schoolproject.ui.screens.AuthScreen
import com.example.schoolproject.ui.theme.SchoolProjectTheme
import com.example.schoolproject.ui.viewmodel.CalmaViewModel
import org.junit.Rule
import org.junit.Test

class AuthScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun signupValidatesEmptyFormWithoutTouchingRealSession() {
        val store = ViewModelStore()
        try {
            compose.setContent {
                val vm = androidx.compose.runtime.remember { CalmaViewModel(TestRepository()).also { store.put("test", it) } }
                SchoolProjectTheme { AuthScreen(vm) }
            }
            compose.onNodeWithText("Sign Up").performClick()
            compose.onNodeWithText("Full name").assertExists()
            compose.onNodeWithText("Create Account").performScrollTo().performClick()
            compose.onNodeWithText("Enter a valid email and password. For signup, add your name and use at least 6 password characters.").assertExists()
        } finally { compose.runOnIdle { store.clear() } }
    }
}
