package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.TrackingViewModel
import com.example.ui.screens.AdminUserManagementScreen
import com.example.ui.screens.AlertLogScreen
import com.example.ui.screens.GeofenceRouteScreen
import com.example.ui.screens.LiveTrackingScreen
import com.example.ui.screens.PlaybackScreen
import com.example.ui.theme.CyberCyanPrimary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AutoGuardApp()
            }
        }
    }
}

@Composable
fun AutoGuardApp() {
    val viewModel: TrackingViewModel = viewModel()
    var selectedScreen by remember { mutableStateOf("LIVE_MAP") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFF3EDF7),
                contentColor = Color(0xFF1D1B20),
                tonalElevation = 3.dp
            ) {
                NavigationBarItem(
                    selected = selectedScreen == "LIVE_MAP",
                    onClick = { selectedScreen = "LIVE_MAP" },
                    icon = { Icon(imageVector = Icons.Default.Map, contentDescription = "Live GPS") },
                    label = { Text("แผนที่สด", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF6750A4),
                        indicatorColor = Color(0xFF6750A4),
                        unselectedIconColor = Color(0xFF49454F),
                        unselectedTextColor = Color(0xFF49454F)
                    )
                )

                NavigationBarItem(
                    selected = selectedScreen == "ALERTS",
                    onClick = { selectedScreen = "ALERTS" },
                    icon = { Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = "Alerts") },
                    label = { Text("เตือนความเร็ว", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF6750A4),
                        indicatorColor = Color(0xFF6750A4),
                        unselectedIconColor = Color(0xFF49454F),
                        unselectedTextColor = Color(0xFF49454F)
                    )
                )

                NavigationBarItem(
                    selected = selectedScreen == "PLAYBACK",
                    onClick = { selectedScreen = "PLAYBACK" },
                    icon = { Icon(imageVector = Icons.Default.History, contentDescription = "Playback") },
                    label = { Text("ย้อนหลัง", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF6750A4),
                        indicatorColor = Color(0xFF6750A4),
                        unselectedIconColor = Color(0xFF49454F),
                        unselectedTextColor = Color(0xFF49454F)
                    )
                )

                NavigationBarItem(
                    selected = selectedScreen == "ADMIN",
                    onClick = { selectedScreen = "ADMIN" },
                    icon = { Icon(imageVector = Icons.Default.ManageAccounts, contentDescription = "Admin") },
                    label = { Text("จัดการข้อมูล", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF6750A4),
                        indicatorColor = Color(0xFF6750A4),
                        unselectedIconColor = Color(0xFF49454F),
                        unselectedTextColor = Color(0xFF49454F)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedScreen) {
                "LIVE_MAP" -> LiveTrackingScreen(viewModel = viewModel)
                "ALERTS" -> AlertLogScreen(viewModel = viewModel)
                "PLAYBACK" -> PlaybackScreen(viewModel = viewModel)
                "ADMIN" -> AdminUserManagementScreen(viewModel = viewModel)
            }
        }
    }
}

