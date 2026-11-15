package com.example.schoolproject.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schoolproject.data.models.TimeOfDay
import com.example.schoolproject.ui.theme.CalmaAmber
import com.example.schoolproject.ui.theme.CalmaBackground
import com.example.schoolproject.ui.theme.CalmaChipUnselected
import com.example.schoolproject.ui.theme.CalmaGreen
import com.example.schoolproject.ui.theme.CalmaGreenDark
import com.example.schoolproject.ui.theme.CalmaTextDark
import com.example.schoolproject.ui.theme.CalmaTextMuted
import com.example.schoolproject.ui.viewmodel.CalmaViewModel
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: CalmaViewModel,
    onGetRecommendationClicked: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val busy by viewModel.busy.collectAsState()
    val dataReady by viewModel.dataReady.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val checkInState by viewModel.checkInState.collectAsState()
    val lastCheckIn by viewModel.lastCheckIn.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CalmaBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Top Header & Streak Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hello, ${userProfile.name.split(" ").firstOrNull()?.ifBlank { "there" } ?: "there"}",
                    fontSize = 13.sp,
                    color = CalmaTextMuted
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "How are you feeling today?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CalmaTextDark
                )
            }

            // Streak Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2DDD3), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(CalmaAmber)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${userProfile.streakDays}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CalmaTextDark
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CalmaGreen)) {
            Column(Modifier.padding(20.dp)) {
                Text("Latest saved check-in", color = Color.White, fontWeight = FontWeight.Bold)
                val saved = lastCheckIn
                if (saved == null) Text("Your saved sleep and stress check-in will appear here.", color = Color.White)
                else {
                    Text("Sleep: ${saved.sleep_hours} hours · Stress: ${saved.stress_level}/10", color = Color.White)
                    Text(com.example.schoolproject.data.helpers.displaySavedTime(saved.created_at), color = Color.White)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.OutlinedButton(onClick = onGetRecommendationClicked,
            enabled = !busy && dataReady, modifier = Modifier.fillMaxWidth()) { Text("Open breathing & journal") }
        Spacer(modifier = Modifier.height(16.dp))

        // Daily Check-In Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Daily Check-In",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = CalmaTextDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Sleep last night
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sleep last night (hours)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalmaTextDark
                    )
                    Text(
                        text = "${checkInState.sleepHours.roundToInt()} hrs",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CalmaTextDark
                    )
                }

                Slider(
                    value = checkInState.sleepHours,
                    onValueChange = { viewModel.updateSleepHours(it) },
                    valueRange = 0f..12f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = CalmaGreen,
                        inactiveTrackColor = Color(0xFFE4EDE7)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Stress level (1-10)
                Text(
                    text = "Stress level (1–10)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CalmaTextDark
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (1..10).forEach { level ->
                        val isSelected = checkInState.stressLevel == level
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) CalmaAmber else CalmaChipUnselected)
                                .clickable { viewModel.updateStressLevel(level) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$level",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else CalmaTextDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3. Time of day
                Text(
                    text = "Time of day",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CalmaTextDark
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TimeOfDay.values().forEach { tod ->
                        val isSelected = checkInState.timeOfDay == tod
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) CalmaGreen else CalmaChipUnselected)
                                .clickable { viewModel.updateTimeOfDay(tod) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tod.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else CalmaTextDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button
                Button(
                    enabled = !busy && dataReady,
                    onClick = {
                        viewModel.submitCheckIn {
                            android.widget.Toast.makeText(context, "Check-in saved", android.widget.Toast.LENGTH_SHORT).show()
                            onGetRecommendationClicked()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CalmaGreen)
                ) {
                    Text(
                        text = if (busy) "Saving check-in…" else "Save check-in & choose activity",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Milestones
        Text(
            text = "Milestones",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = CalmaTextDark
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Milestone 1
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE5EDE8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${userProfile.streakDays}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CalmaGreenDark
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Day Streak",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CalmaTextDark
                        )
                        Text(
                            text = "Consecutive days",
                            fontSize = 11.sp,
                            color = CalmaTextMuted
                        )
                    }
                }
            }

            // Milestone 2
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEDE7F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${userProfile.activitiesCount}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B588A)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Activities Done",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CalmaTextDark
                        )
                        Text(
                            text = "Saved so far",
                            fontSize = 11.sp,
                            color = CalmaTextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
