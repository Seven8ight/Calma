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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.schoolproject.ui.navigation.BottomTab
import com.example.schoolproject.ui.theme.CalmaBackground
import com.example.schoolproject.ui.theme.CalmaGreen
import com.example.schoolproject.ui.theme.CalmaTextDark
import com.example.schoolproject.ui.theme.CalmaTextMuted
import com.example.schoolproject.ui.viewmodel.CalmaViewModel

@Composable
fun MainContainerScreen(
    viewModel: CalmaViewModel,
    onNavigateToActivity: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToLogin: () -> Unit,
    initialTab: BottomTab = BottomTab.HOME
) {
    val busy by viewModel.busy.collectAsState()
    val dataReady by viewModel.dataReady.collectAsState()
    var selectedTab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initialTab) }

    Scaffold(
        containerColor = CalmaBackground,
        bottomBar = {
            CalmaBottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!dataReady) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(if (busy) "Loading your account…" else "Your account data has not loaded.")
                    if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    else TextButton(onClick = { viewModel.reloadData() }) { Text("Retry") }
                    TextButton(onClick = onNavigateToLogin) { Text("Log out") }
                }
            } else when (selectedTab) {
                BottomTab.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onGetRecommendationClicked = onNavigateToActivity
                )
                BottomTab.SUPPORT -> SupportScreen(
                    viewModel = viewModel,
                    onBookCounsellorClicked = onNavigateToBookings
                )
                BottomTab.PROFILE -> ProfileScreen(
                    viewModel = viewModel,
                    onNavigateToBookings = onNavigateToBookings,
                    onLogOutClicked = onNavigateToLogin
                )
            }
        }
    }
}

@Composable
fun CalmaBottomNavigationBar(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CalmaGreen)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text(
                        text = tab.label,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) CalmaTextDark else CalmaTextMuted
                    )
                }
            }
        }
    }
}
