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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import com.example.schoolproject.ui.viewmodel.AuthUIState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schoolproject.ui.theme.CalmaBackground
import com.example.schoolproject.ui.theme.CalmaBorder
import com.example.schoolproject.ui.theme.CalmaGreen
import com.example.schoolproject.ui.theme.CalmaTextDark
import com.example.schoolproject.ui.theme.CalmaTextMuted

@Composable
fun AuthScreen(
    viewModel: com.example.schoolproject.ui.viewmodel.CalmaViewModel
) {
    val authState by viewModel.authUIState.collectAsState()
    val loading = authState == AuthUIState.Loading
    var name by remember { mutableStateOf("") }
    var isLoginTab by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CalmaBackground)
    ) {
        // Top right background decorative accent
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(Color(0xFFE5EDE8).copy(alpha = 0.7f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Logo Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CalmaGreen),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(32.dp)) {
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
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isLoginTab) "Welcome to Calma" else "Join Calma Today",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = CalmaTextDark
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Sign in to continue your journey",
                fontSize = 14.sp,
                color = CalmaTextMuted
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Tab Switcher: Log In / Sign Up
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFEFECE5))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isLoginTab) CalmaGreen else Color.Transparent)
                        .clickable(enabled = !loading) { isLoginTab = true; viewModel.clearAuthMessage() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Log In",
                        fontWeight = if (isLoginTab) FontWeight.Bold else FontWeight.Medium,
                        color = if (isLoginTab) Color.White else CalmaTextMuted,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (!isLoginTab) CalmaGreen else Color.Transparent)
                        .clickable(enabled = !loading) { isLoginTab = false; viewModel.clearAuthMessage() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign Up",
                        fontWeight = if (!isLoginTab) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isLoginTab) Color.White else CalmaTextMuted,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isLoginTab) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Full name") }, enabled = !loading,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(14.dp))
                Spacer(modifier = Modifier.height(16.dp))
            }
            // Form Inputs
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Email",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CalmaTextDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    enabled = !loading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    onValueChange = { email = it },
                    placeholder = { Text("you@example.com", color = CalmaTextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = CalmaGreen,
                        unfocusedBorderColor = Color(0xFFE0DDD5)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Password",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CalmaTextDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = password,
                    enabled = !loading,
                    onValueChange = { password = it },
                    placeholder = { Text("••••••••", color = CalmaTextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle password",
                                tint = CalmaTextMuted
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = CalmaGreen,
                        unfocusedBorderColor = Color(0xFFE0DDD5)
                    ),
                    singleLine = true
                )


            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Action Button
            Button(
                onClick = {
                    if (isLoginTab) viewModel.loginWithSupabase(email, password)
                    else viewModel.signUpWithSupabase(email, password, name)
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CalmaGreen)
            ) {
                Text(
                    text = if (loading) "Please wait…" else if (isLoginTab) "Log In" else "Create Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = authState) {
                is AuthUIState.Error -> Text(state.message, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                is AuthUIState.EmailConfirmationRequired -> Text(state.message, color = CalmaGreen)
                else -> Unit
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Footer Link
            Row(
                modifier = Modifier.clickable(enabled = !loading) { isLoginTab = !isLoginTab; viewModel.clearAuthMessage() }
            ) {
                Text(
                    text = if (isLoginTab) "Don't have an account? " else "Already have an account? ",
                    fontSize = 13.sp,
                    color = CalmaTextMuted
                )
                Text(
                    text = if (isLoginTab) "Create one — it's free" else "Log In",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CalmaGreen
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
