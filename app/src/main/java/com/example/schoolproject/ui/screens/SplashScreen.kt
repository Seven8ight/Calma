package com.example.schoolproject.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schoolproject.ui.theme.CalmaBackground
import com.example.schoolproject.ui.theme.CalmaGreen
import com.example.schoolproject.ui.theme.CalmaTextDark
import com.example.schoolproject.ui.theme.CalmaTextMuted
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1800)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CalmaBackground)
    ) {
        // Top-right background curve accent
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .padding(top = 0.dp, end = 0.dp)
                .clip(CircleShape)
                .background(Color(0xFFE5EDE8).copy(alpha = 0.7f))
        )

        // Bottom-left background curve accent
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomStart)
                .padding(bottom = 0.dp, start = 0.dp)
                .clip(CircleShape)
                .background(Color(0xFFEDE7F0).copy(alpha = 0.6f))
        )

        // Center Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Squircle Logo Icon with wave
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(CalmaGreen),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(48.dp)) {
                    val path = Path().apply {
                        moveTo(0f, size.height * 0.5f)
                        cubicTo(
                            size.width * 0.25f, size.height * 0.1f,
                            size.width * 0.45f, size.height * 0.9f,
                            size.width * 0.75f, size.height * 0.3f
                        )
                        quadraticTo(
                            size.width * 0.9f, size.height * 0.1f,
                            size.width, size.height * 0.2f
                        )
                    }
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Calma",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = CalmaTextDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your daily companion for calm",
                fontSize = 15.sp,
                color = CalmaTextMuted
            )
        }

        // Bottom Progress Section
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .width(120.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = CalmaGreen,
                trackColor = Color(0xFFD6E2DB)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Preparing your data...",
                fontSize = 13.sp,
                color = CalmaTextMuted
            )
        }
    }
}
