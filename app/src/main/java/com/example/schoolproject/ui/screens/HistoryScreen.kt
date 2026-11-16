package com.example.schoolproject.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schoolproject.data.helpers.bookingStatusLabel
import com.example.schoolproject.data.helpers.displaySavedTime
import com.example.schoolproject.data.models.Booking
import com.example.schoolproject.data.remote.ActivityDto
import com.example.schoolproject.ui.viewmodel.CalmaViewModel

@Composable
fun HistoryScreen(viewModel: CalmaViewModel, onBack: () -> Unit) {
    val activities by viewModel.activities.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val ready by viewModel.dataReady.collectAsState()
    HistoryContent(activities, bookings, busy, ready, viewModel::reloadData, onBack)
}

@Composable
fun HistoryContent(activities: List<ActivityDto>, bookings: List<Booking>, busy: Boolean,
                   ready: Boolean, onRefresh: () -> Unit, onBack: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf("Journals") }
    val listState = rememberLazyListState()
    LaunchedEffect(tab) { listState.scrollToItem(0) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Back") }
            TextButton(onClick = onRefresh, enabled = !busy) { Text("Refresh") }
        }
        Text("Journals & sessions", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Revisit your saved writing, activities and counselling appointments.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Journals", "Activities", "Bookings").forEach { label ->
                FilterChip(selected = tab == label, onClick = { tab = label }, label = { Text(label) })
            }
        }
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (!ready && !busy) Text("History could not be refreshed. Tap Refresh to try again.", color = MaterialTheme.colorScheme.error)
        val visibleActivities = if (tab == "Journals")
            activities.filter { !it.gratitude_note.isNullOrBlank() } else activities
        LazyColumn(Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (tab == "Bookings") {
                if (bookings.isEmpty() && ready && !busy) item { Text("No appointments yet. Book a counsellor from Support.") }
                items(bookings, key = { it.id }) { booking ->
                    HistoryCard {
                        Text(booking.counsellor.name, fontWeight = FontWeight.Bold)
                        Text("${booking.dateSlot} · ${booking.timeSlot}")
                        Text(bookingStatusLabel(booking), color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                if (visibleActivities.isEmpty() && ready && !busy) item {
                    Text(if (tab == "Journals") "No saved journals yet. Save a gratitude entry or a breathing reflection to see it here."
                    else "No saved activities yet. Finish an activity and tap Save.")
                }
                items(visibleActivities, key = { it.id }) { activity ->
                    HistoryCard {
                        Text(if (activity.activity_type == "GRATITUDE_JOURNAL") "Gratitude journal" else "Breathing session", fontWeight = FontWeight.Bold)
                        Text(displaySavedTime(activity.created_at), style = MaterialTheme.typography.bodySmall)
                        if (activity.duration_seconds > 0) Text("Duration: ${activity.duration_seconds / 60} min")
                        activity.gratitude_note?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        activity.stress_decrease_percent?.let { Text("Stress decrease you reported: $it%") }
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun HistoryCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
