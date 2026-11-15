package com.example.schoolproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.schoolproject.ui.navigation.CalmaNavGraph
import com.example.schoolproject.ui.theme.SchoolProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SchoolProjectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CalmaNavGraph()
                }
            }
        }
    }
}
