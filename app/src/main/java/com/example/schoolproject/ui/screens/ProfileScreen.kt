package com.example.schoolproject.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.schoolproject.data.helpers.displaySavedTime
import com.example.schoolproject.notifications.ReminderScheduler
import com.example.schoolproject.ui.viewmodel.AuthUIState
import com.example.schoolproject.ui.viewmodel.CalmaViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ProfileScreen(viewModel: CalmaViewModel, onNavigateToBookings: () -> Unit, onLogOutClicked: () -> Unit, onNavigateToHistory: () -> Unit) {
    val profile by viewModel.userProfile.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val authState by viewModel.authUIState.collectAsState()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val latestProfile by rememberUpdatedState(profile)
    var editName by remember { mutableStateOf(false) }
    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var showPrivacy by remember { mutableStateOf(false) }
    var notificationsAllowed by remember { mutableStateOf(ReminderScheduler.isAllowed(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted && ReminderScheduler.isAllowed(context)
        if (granted) {
            if (!profile.notificationsEnabled) viewModel.toggleNotifications()
            else ReminderScheduler.sync(context, true, profile.reminderTime)
        } else Toast.makeText(context, "Notifications are not allowed on this device.", Toast.LENGTH_LONG).show()
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsAllowed = ReminderScheduler.isAllowed(context)
                ReminderScheduler.sync(context, latestProfile.notificationsEnabled, latestProfile.reminderTime)
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    if (editName) AlertDialog(onDismissRequest = { if (!busy) editName = false },
        title = { Text("Personal details") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it.take(80) }, label = { Text("Full name") }, enabled = !busy, singleLine = true)
            Text("Email: ${profile.email}")
        } },
        confirmButton = { TextButton(enabled = !busy && name.isNotBlank(), onClick = {
            viewModel.updateProfileName(name) { editName = false; Toast.makeText(context, "Name saved", Toast.LENGTH_SHORT).show() }
        }) { Text(if (busy) "Saving…" else "Save") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { editName = false }) { Text("Cancel") } })

    if (showPrivacy) AlertDialog(onDismissRequest = { showPrivacy = false },
        title = { Text("Privacy & data") },
        text = { Text("Your profile, check-ins, bookings, saved activities and support-chat messages are stored in Calma's Supabase database and linked to your account.\n\nSupport-chat replies are predefined automated messages.\n\nActivity timers and notes stay unsaved until you tap Save. Logging out clears your account's data from the app's memory and turns off this device's reminders.") },
        confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text("Close") } })

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(profile.name, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
        Text(profile.email)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                ProfileStat(profile.streakDays, "Day streak")
                ProfileStat(profile.checkInsCount, "Check-ins")
                ProfileStat(profile.activitiesCount, "Activities")
            }
        }
        OutlinedButton(onClick = { name = profile.name; editName = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Personal Details") }
        OutlinedButton(onClick = onNavigateToBookings, modifier = Modifier.fillMaxWidth()) { Text("My Bookings") }
        OutlinedButton(onClick = onNavigateToHistory, modifier = Modifier.fillMaxWidth()) { Text("Journals & Session History") }
        OutlinedButton(onClick = { showPrivacy = true }, modifier = Modifier.fillMaxWidth()) { Text("Privacy & Data") }

        Text("Daily check-in reminder", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (profile.notificationsEnabled && notificationsAllowed) "Enabled" else "Off on this device")
            Switch(checked = profile.notificationsEnabled && notificationsAllowed, enabled = !busy,
                onCheckedChange = { enabled ->
                    if (!enabled) viewModel.toggleNotifications()
                    else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                            Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else if (!ReminderScheduler.isAllowed(context)) {
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    } else if (!profile.notificationsEnabled) viewModel.toggleNotifications()
                })
        }
        OutlinedButton(enabled = !busy, onClick = {
            val date = try { SimpleDateFormat("h:mm a", Locale.ENGLISH).parse(profile.reminderTime) } catch (_: Exception) { null }
            val time = Calendar.getInstance().apply { if (date != null) this.time = date }
            TimePickerDialog(context, { _, hour, minute ->
                time.set(Calendar.HOUR_OF_DAY, hour); time.set(Calendar.MINUTE, minute)
                viewModel.updateReminderTime(SimpleDateFormat("h:mm a", Locale.ENGLISH).format(time.time))
            }, time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), false).show()
        }) { Text("Reminder time: ${profile.reminderTime}") }
        Text("Android delivers reminders around this time; battery settings can delay them.", style = MaterialTheme.typography.bodySmall)
        Text("Appearance: Light", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onLogOutClicked, enabled = authState != AuthUIState.Loading, modifier = Modifier.fillMaxWidth()) {
            Text(if (authState == AuthUIState.Loading) "Logging out…" else "Log Out")
        }
    }
}

@Composable
private fun ProfileStat(value: Int, label: String) {
    Column {
        Text(value.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
