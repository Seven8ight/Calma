package com.example.schoolproject.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schoolproject.data.models.BookingStatus
import com.example.schoolproject.ui.theme.CalmaBackground
import com.example.schoolproject.ui.theme.CalmaChipUnselected
import com.example.schoolproject.ui.theme.CalmaGreen
import com.example.schoolproject.ui.theme.CalmaGreenDark
import com.example.schoolproject.ui.theme.CalmaTextDark
import com.example.schoolproject.ui.theme.CalmaTextMuted
import com.example.schoolproject.ui.viewmodel.CalmaViewModel

@Composable
fun BookingsScreen(
    viewModel: CalmaViewModel,
    onBackClicked: () -> Unit,
    onConfirmBookingSuccess: () -> Unit
) {
    val counsellors by viewModel.counsellors.collectAsState()
    var showCounsellors by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val bookings by viewModel.bookings.collectAsState()
    val selectedCounsellor by viewModel.selectedCounsellorForBooking.collectAsState()
    val selectedSlot by viewModel.selectedSlot.collectAsState()
    val allSlots by viewModel.slots.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val dataReady by viewModel.dataReady.collectAsState()
    var cancelTarget by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.schoolproject.data.models.Booking?>(null) }
    val showCancelDialog by viewModel.showCancelDialog.collectAsState()
    val cancellationConfirmed by viewModel.cancellationConfirmed.collectAsState()

    val upcomingBookings = bookings.filter { it.status == BookingStatus.UPCOMING }
    val historyBookings = bookings.filter { it.status != BookingStatus.UPCOMING }

    val slots = allSlots.filter { it.counsellor_id == selectedCounsellor.id }

    if (showCancelDialog && cancelTarget != null) {
        CancelBookingDialog(
            counsellorName = cancelTarget!!.counsellor.name,
            dateTime = "${cancelTarget!!.dateSlot} · ${cancelTarget!!.timeSlot}",
            onDismiss = { viewModel.closeCancelDialog() },
            onConfirm = { viewModel.confirmCancelBooking() }
        )
    }

    if (showCounsellors) {
        AlertDialog(onDismissRequest = { showCounsellors = false }, title = { Text("Choose a counsellor") },
            text = { Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                if (counsellors.isEmpty()) Text("No counsellors are available yet.")
                counsellors.forEach { counsellor ->
                    androidx.compose.material3.TextButton(onClick = { viewModel.selectCounsellorForBooking(counsellor); showCounsellors = false }) {
                        Text(counsellor.name)
                    }
                }
            } }, confirmButton = { androidx.compose.material3.TextButton(onClick = { showCounsellors = false }) { Text("Close") } })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CalmaBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2DDD3), CircleShape)
                    .clickable { onBackClicked() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CalmaTextDark
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Bookings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CalmaTextDark
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Upcoming Session Section
        Text(
            text = "Upcoming Session",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = CalmaTextDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (upcomingBookings.isNotEmpty()) {
            upcomingBookings.forEach { upcomingBooking ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CalmaGreen)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = upcomingBooking.counsellor.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = upcomingBooking.counsellor.title,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${upcomingBooking.dateSlot} · ${upcomingBooking.timeSlot}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        enabled = !busy && dataReady,
                        onClick = { cancelTarget = upcomingBooking; viewModel.openCancelDialog(upcomingBooking.id) },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CalmaTextMuted
                        )
                    }
                }
            }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "No upcoming sessions",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CalmaTextDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Book a new session when you're ready.",
                        fontSize = 12.sp,
                        color = CalmaTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Book a New Session Section
        Text(
            text = "Book a New Session",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = CalmaTextDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(selectedCounsellor.avatarColorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = selectedCounsellor.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CalmaGreenDark
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = selectedCounsellor.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CalmaTextDark
                        )
                        Text(
                            text = selectedCounsellor.title,
                            fontSize = 11.sp,
                            color = CalmaTextMuted
                        )
                    }
                }

                androidx.compose.material3.TextButton(onClick = { showCounsellors = true }, enabled = !busy) {
                    Text("Change")
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Available slots
        Text(
            text = if (slots.isEmpty()) "No available slots for this counsellor" else "Available sessions",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = CalmaTextDark
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            slots.forEach { slot ->
                val isSelected = slot.id == selectedSlot?.id
                OutlinedButton(onClick = { viewModel.selectSlot(slot) }, enabled = !busy && dataReady,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("${if (isSelected) "✓ " else ""}${slot.slot_date} · ${slot.slot_time}")
                }
            }
            OutlinedButton(onClick = { viewModel.reloadData() }, enabled = !busy) { Text("Refresh availability") }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            enabled = !busy && dataReady && selectedSlot != null,
            onClick = {
                viewModel.createBooking(onConfirmBookingSuccess)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CalmaGreen)
        ) {
            Text(
                text = if (busy) "Please wait…" else "Confirm Booking",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // History
        Text(
            text = "History",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = CalmaTextDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (historyBookings.isEmpty()) Text("Completed and cancelled sessions will appear here.", color = CalmaTextMuted)
        historyBookings.forEach { history ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = history.counsellor.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CalmaTextDark
                        )
                        Text(
                            text = "${history.dateSlot} · ${history.timeSlot}",
                            fontSize = 11.sp,
                            color = CalmaTextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CalmaChipUnselected)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (history.status == BookingStatus.CANCELLED) "Cancelled" else "Completed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = CalmaTextMuted
                        )
                    }
                }
            }
        }

        if (cancellationConfirmed) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(CalmaGreen),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cancellation confirmed",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun CancelBookingDialog(
    counsellorName: String,
    dateTime: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cancel this booking?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CalmaTextDark
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to cancel your upcoming session?",
                    fontSize = 13.sp,
                    color = CalmaTextMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF2EFE8))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = counsellorName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CalmaTextDark
                        )
                        Text(
                            text = dateTime,
                            fontSize = 12.sp,
                            color = CalmaTextMuted
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CalmaGreen),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Confirm", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cancel", color = CalmaTextDark)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}
