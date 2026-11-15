package com.example.schoolproject.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.schoolproject.data.models.ActivityType
import com.example.schoolproject.data.models.BreathingSessionState
import com.example.schoolproject.ui.viewmodel.CalmaViewModel
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PersonalisedActivityScreen(viewModel: CalmaViewModel, onBackClicked: () -> Unit) {
    val state by viewModel.breathingState.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val ready by viewModel.dataReady.collectAsState()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.pauseBreathing()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); viewModel.pauseBreathing() }
    }
    ActivityContent(state, busy || !ready, viewModel::toggleBreathingPlayPause,
        viewModel::startNewActivity, viewModel::updateGratitudeNote,
        viewModel::updateStressDecrease, viewModel::saveActivity, onBackClicked)
}

// UI only: the ViewModel handles the timer and database save.
@Composable
fun ActivityContent(
    state: BreathingSessionState,
    busy: Boolean,
    onPlayPause: () -> Unit,
    onNewActivity: (ActivityType) -> Unit,
    onNoteChanged: (String) -> Unit,
    onStressChanged: (Float) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    var replacement by remember { mutableStateOf<ActivityType?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }
    fun leave() {
        if (state.isSaving) return
        if (state.isPlaying) onPlayPause()
        if (state.hasDraft) confirmLeave = true else onBack()
    }
    fun choose(type: ActivityType) {
        if (state.hasDraft) replacement = type else onNewActivity(type)
    }
    BackHandler { leave() }
    if (replacement != null) {
        AlertDialog(onDismissRequest = { replacement = null },
            title = { Text("Start over?") },
            text = { Text("This clears your unfinished timer and note. Saved activities stay in your history.") },
            confirmButton = { TextButton(onClick = { onNewActivity(replacement!!); replacement = null }) { Text("Start over") } },
            dismissButton = { TextButton(onClick = { replacement = null }) { Text("Keep session") } })
    }
    if (confirmLeave) {
        AlertDialog(onDismissRequest = { confirmLeave = false },
            title = { Text("This activity is not saved") },
            text = { Text("Leaving pauses the timer. You can return while the app stays open, but closing the app may lose this unfinished session.") },
            confirmButton = { TextButton(onClick = { confirmLeave = false; onBack() }) { Text("Leave for now") } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Keep working") } })
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = { leave() }, enabled = !state.isSaving) { Text("Back") }
        Text("Take a moment for yourself", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActivityType.entries.forEach { type ->
                FilterChip(selected = state.activityType == type, onClick = { if (state.activityType != type) choose(type) },
                    enabled = !busy, label = { Text(type.title) })
            }
        }
        if (state.activityType == ActivityType.BREATHING_478) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("4-7-8 breathing", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Follow the prompts for two minutes, at a pace that feels comfortable.")
                    Text(String.format(Locale.ENGLISH, "%02d:%02d", state.secondsRemaining / 60, state.secondsRemaining % 60),
                        fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(progress = { 1f - state.secondsRemaining.toFloat() / state.totalSeconds },
                        modifier = Modifier.fillMaxWidth())
                    Text(state.breathingInstruction, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    if (!state.isComplete) {
                        Button(onClick = onPlayPause, enabled = !busy) {
                            Text(if (state.isPlaying) "Pause" else if (state.secondsRemaining == state.totalSeconds) "Start breathing" else "Resume")
                        }
                    }
                    TextButton(onClick = { choose(state.activityType) }, enabled = !busy) {
                        Text(if (state.isSaved) "Start another session" else "Reset timer")
                    }
                }
            }
        }

        if (state.isSaved) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Activity saved", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(if (state.activityType == ActivityType.BREATHING_478)
                        "Your 2-minute breathing session is recorded in your activity history."
                    else "Your gratitude journal is recorded in your activity history.")
                    if (state.gratitudeNote.isNotBlank()) Text(state.gratitudeNote)
                    state.stressDecreasePercent?.let { Text("Stress decrease you reported: ${it.roundToInt()}%") }
                }
            }
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            if (state.activityType == ActivityType.GRATITUDE_JOURNAL) {
                OutlinedButton(onClick = { choose(state.activityType) }, modifier = Modifier.fillMaxWidth()) { Text("Write another entry") }
            }
        } else {
            Text(if (state.activityType == ActivityType.BREATHING_478 && state.isComplete)
                "Session complete — save it below" else if (state.activityType == ActivityType.GRATITUDE_JOURNAL)
                "What are you grateful for today?" else "Your session is not recorded until you save it.",
                fontWeight = FontWeight.SemiBold)
            OutlinedTextField(value = state.gratitudeNote, onValueChange = onNoteChanged,
                label = { Text(if (state.activityType == ActivityType.BREATHING_478) "Reflection (optional)" else "Gratitude note") },
                modifier = Modifier.fillMaxWidth(), minLines = 3, enabled = !state.isSaving,
                supportingText = { Text("${state.gratitudeNote.length}/2000 characters") })
            if (state.activityType == ActivityType.GRATITUDE_JOURNAL || state.isComplete) {
                Text("How much did your stress decrease? (Optional)")
                Text(state.stressDecreasePercent?.let { "${it.roundToInt()}% — your own estimate" } ?: "Not answered")
                Slider(value = state.stressDecreasePercent ?: 0f, onValueChange = onStressChanged,
                    valueRange = 0f..100f, steps = 19, enabled = !state.isSaving)
                TextButton(onClick = { onStressChanged(0f) }, enabled = !state.isSaving) { Text("No decrease (0%)") }
            }
            state.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = onSave, enabled = state.canSave && !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.isSaving) "Saving…" else if (state.activityType == ActivityType.BREATHING_478) "Save session" else "Save journal entry")
            }
            if (state.activityType == ActivityType.BREATHING_478 && !state.isComplete)
                Text("Finish the timer to save a breathing session. You can pause and resume.")
        }
    }
}
