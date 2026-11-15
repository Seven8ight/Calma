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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schoolproject.ui.theme.CalmaBackground
import com.example.schoolproject.ui.theme.CalmaChipUnselected
import com.example.schoolproject.ui.theme.CalmaGreen
import com.example.schoolproject.ui.theme.CalmaGreenDark
import com.example.schoolproject.ui.theme.CalmaTextDark
import com.example.schoolproject.ui.theme.CalmaTextMuted
import com.example.schoolproject.ui.viewmodel.CalmaViewModel

@Composable
fun BookingConfirmationScreen(
    viewModel: CalmaViewModel,
    onBackToBookingsClicked: () -> Unit
) {
    val savedBooking by viewModel.lastBooking.collectAsState()
    val booking = savedBooking ?: return
    val selectedCounsellor = booking.counsellor
    val selectedTimeSlot = booking.timeSlot

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CalmaBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Bar
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
                    .clickable { onBackToBookingsClicked() },
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

        Spacer(modifier = Modifier.height(48.dp))

        // Center Success Check Icon Badge
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFD6ECE0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = CalmaGreen,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "You're successfully booked",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = CalmaTextDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your session is confirmed.\nWe look forward to seeing you.",
            fontSize = 13.sp,
            color = CalmaTextMuted,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Session Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${booking.dateSlot} · $selectedTimeSlot",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CalmaTextDark
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CalmaChipUnselected)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Confirmed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CalmaGreenDark
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onBackToBookingsClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CalmaGreen)
        ) {
            Text(
                text = "Back to Bookings",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
