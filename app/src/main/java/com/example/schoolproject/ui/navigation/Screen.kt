package com.example.schoolproject.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object MainContainer : Screen("main_container")
    object PersonalisedActivity : Screen("activity")
    object Bookings : Screen("bookings")
    object BookingConfirmation : Screen("booking_confirmation")
}

enum class BottomTab(val route: String, val label: String) {
    HOME("home", "Home"),
    SUPPORT("support", "Support"),
    PROFILE("profile", "Profile")
}
