package com.example.schoolproject.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.schoolproject.data.repository.CalmaSession
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.schoolproject.ui.screens.AuthScreen
import com.example.schoolproject.ui.screens.BookingConfirmationScreen
import com.example.schoolproject.ui.screens.BookingsScreen
import com.example.schoolproject.ui.screens.MainContainerScreen
import com.example.schoolproject.ui.screens.PersonalisedActivityScreen
import com.example.schoolproject.ui.screens.SplashScreen
import com.example.schoolproject.ui.viewmodel.CalmaViewModel

@Composable
fun CalmaNavGraph(
    navController: NavHostController = rememberNavController(),
    viewModel: CalmaViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val profile by viewModel.userProfile.collectAsState()
    val session by viewModel.session.collectAsState()
    val error by viewModel.error.collectAsState()
    val dataReady by viewModel.dataReady.collectAsState()
    val busy by viewModel.busy.collectAsState()
    LaunchedEffect(session, dataReady, profile.notificationsEnabled, profile.reminderTime) {
        if (session is CalmaSession.SignedOut) {
            com.example.schoolproject.notifications.ReminderScheduler.sync(context, false, profile.reminderTime)
        } else if (session is CalmaSession.SignedIn && dataReady) {
            com.example.schoolproject.notifications.ReminderScheduler.sync(context, profile.notificationsEnabled, profile.reminderTime)
        }
    }
    val lastBooking by viewModel.lastBooking.collectAsState()
    var splashFinished by rememberSaveable { mutableStateOf(false) }
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    LaunchedEffect(session, splashFinished, route) {
        if (splashFinished && session != CalmaSession.Initializing && route != null) {
            val signedIn = session is CalmaSession.SignedIn
            if (!signedIn && route != Screen.Auth.route || signedIn && (route == Screen.Auth.route || route == Screen.Splash.route)) {
                navController.navigate(if (signedIn) Screen.MainContainer.route else Screen.Auth.route) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
    if (error != null) {
        AlertDialog(onDismissRequest = viewModel::dismissError,
            title = { Text("Something went wrong") }, text = { Text(error!!) },
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text("OK") } },
            dismissButton = { if (!dataReady && !busy) TextButton(onClick = { viewModel.dismissError(); viewModel.reloadData() }) { Text("Retry loading") } })
    }
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onSplashFinished = { splashFinished = true })
        }

        composable(Screen.Auth.route) {
            AuthScreen(viewModel = viewModel)
        }

        composable(Screen.MainContainer.route) {
            MainContainerScreen(
                viewModel = viewModel,
                onNavigateToActivity = {
                    navController.navigate(Screen.PersonalisedActivity.route)
                },
                onNavigateToBookings = {
                    navController.navigate(Screen.Bookings.route)
                },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToLogin = { viewModel.logout() }
            )
        }

        composable(Screen.PersonalisedActivity.route) {
            PersonalisedActivityScreen(
                viewModel = viewModel,
                onBackClicked = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.History.route) {
            com.example.schoolproject.ui.screens.HistoryScreen(viewModel, onBack = { navController.popBackStack() })
        }

        composable(Screen.Bookings.route) {
            BookingsScreen(
                viewModel = viewModel,
                onBackClicked = {
                    navController.popBackStack()
                },
                onConfirmBookingSuccess = {
                    navController.navigate(Screen.BookingConfirmation.route)
                }
            )
        }

        composable(Screen.BookingConfirmation.route) {
            LaunchedEffect(lastBooking) {
                if (lastBooking == null) navController.popBackStack(Screen.Bookings.route, inclusive = false)
            }
            BookingConfirmationScreen(
                viewModel = viewModel,
                onBackToBookingsClicked = {
                    navController.popBackStack(Screen.Bookings.route, inclusive = false)
                }
            )
        }
    }
}
