package com.example.schoolproject.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schoolproject.data.models.Counsellor
import com.example.schoolproject.data.models.MessageSender
import com.example.schoolproject.ui.theme.CalmaAmber
import com.example.schoolproject.ui.theme.CalmaBackground
import com.example.schoolproject.ui.theme.CalmaChipUnselected
import com.example.schoolproject.ui.theme.CalmaGreen
import com.example.schoolproject.ui.theme.CalmaGreenDark
import com.example.schoolproject.ui.theme.CalmaTextDark
import com.example.schoolproject.ui.theme.CalmaTextMuted
import com.example.schoolproject.ui.viewmodel.CalmaViewModel

@Composable
fun SupportScreen(
    viewModel: CalmaViewModel,
    onBookCounsellorClicked: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = AI Chatbot, 1 = Counselling

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CalmaBackground)
    ) {
        // Top Header Title
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Support",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CalmaTextDark
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Segmented Switcher: AI Chatbot vs Counselling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(Color(0xFFEFECE5))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selectedTab == 0) CalmaGreen else Color.Transparent)
                        .clickable { selectedTab = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Support Chat",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 0) Color.White else CalmaTextMuted,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selectedTab == 1) CalmaGreen else Color.Transparent)
                        .clickable { selectedTab = 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Counselling",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 1) Color.White else CalmaTextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            ChatbotTabContent(viewModel = viewModel)
        } else {
            CounsellingTabContent(
                viewModel = viewModel,
                onBookCounsellorClicked = onBookCounsellorClicked
            )
        }
    }
}

@Composable
fun ChatbotTabContent(viewModel: CalmaViewModel) {
    val busy by viewModel.busy.collectAsState()
    val dataReady by viewModel.dataReady.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()
    var inputText by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }

    val quickPills = listOf("I feel anxious", "Talk to a counsellor", "Suggest activity")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Chat Header Avatar
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CalmaGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Calmly Companion",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CalmaTextDark
                )
                Text(
                    text = "Automated replies · Not a live counsellor",
                    fontSize = 11.sp,
                    color = CalmaGreenDark
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (messages.isEmpty()) Text("Start a conversation below. Your messages will be saved to your account.", color = CalmaTextMuted)
        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.sender == MessageSender.USER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (isUser) 18.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 18.dp
                                )
                            )
                            .background(if (isUser) CalmaGreen else Color.White)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = msg.text,
                            fontSize = 14.sp,
                            color = if (isUser) Color.White else CalmaTextDark
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (busy) Text("Saving conversation…", color = CalmaTextMuted)
        // Quick Suggestion Pills
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPills.forEach { pill ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .clickable(enabled = !busy && dataReady) { viewModel.sendMessage(pill) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = pill,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = CalmaTextDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Type a message...", fontSize = 13.sp, color = CalmaTextMuted) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = CalmaGreen,
                    unfocusedBorderColor = Color(0xFFE2DDD3)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(CalmaGreen)
                    .clickable(enabled = !busy && dataReady) {
                        if (inputText.isNotBlank()) {
                            val submitted = inputText
                            viewModel.sendMessage(submitted) { if (inputText == submitted) inputText = "" }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CounsellingTabContent(
    viewModel: CalmaViewModel,
    onBookCounsellorClicked: () -> Unit
) {
    val counsellors by viewModel.counsellors.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Licensed professionals available for 1-on-1 sessions",
                fontSize = 13.sp,
                color = CalmaTextMuted
            )
        }

        if (counsellors.isEmpty()) item { Text("No counsellors are listed yet. Please check again later.") }
        items(counsellors) { counsellor ->
            CounsellorCard(
                counsellor = counsellor,
                onBookClicked = {
                    viewModel.selectCounsellorForBooking(counsellor)
                    onBookCounsellorClicked()
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Crisis Helpline Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDCE8E2))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "In crisis right now?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CalmaGreenDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Contact your campus counselling office or local emergency services if you need urgent help.",
                        fontSize = 13.sp,
                        color = CalmaTextDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun CounsellorCard(
    counsellor: Counsellor,
    onBookClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(counsellor.avatarColorHex)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = counsellor.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CalmaGreenDark
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = counsellor.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CalmaTextDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = counsellor.title,
                        fontSize = 12.sp,
                        color = CalmaTextMuted
                    )
                    Text(
                        text = counsellor.specialties,
                        fontSize = 11.sp,
                        color = CalmaTextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = counsellor.availabilityStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (counsellor.isAvailableToday) CalmaGreenDark else CalmaAmber
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onBookClicked,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CalmaGreen),
                modifier = Modifier.height(38.dp)
            ) {
                Text(
                    text = "Book",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
