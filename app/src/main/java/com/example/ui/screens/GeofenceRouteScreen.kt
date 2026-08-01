package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RouteGeofenceEntity
import com.example.ui.TrackingViewModel
import com.example.ui.theme.CyberCyanPrimary
import com.example.ui.theme.EmeraldSafe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofenceRouteScreen(
    viewModel: TrackingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val routes by viewModel.allRoutes.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0F172A),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = CyberCyanPrimary,
                contentColor = Color.Black
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Route")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "เพิ่มเส้นทางใหม่", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "📍 ระบบกำหนดเส้นทางและขอบเขต Geofence",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "กำหนดแนวเส้นทางปลอดภัย (Corridor) และระยะเบี่ยงเบนที่อนุญาต ระบบจะแจ้งเตือนทันทีเมื่อรถออกนอกกรอบ",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (routes.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AltRoute,
                            contentDescription = "No route",
                            tint = CyberCyanPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "ยังไม่มีเส้นทางควบคุมที่เปิดใช้งาน", color = Color.White)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(routes) { route ->
                        RouteCardItem(route = route)
                    }
                }
            }
        }

        if (showAddDialog) {
            AddRouteDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, type, start, end, tol, speed ->
                    viewModel.addNewRoute(name, type, start, end, tol, speed)
                    showAddDialog = false
                    Toast.makeText(context, "บันทึกเส้นทางควบคุมใหม่แล้ว", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun RouteCardItem(route: RouteGeofenceEntity) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CyberCyanPrimary.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            imageVector = if (route.type == "CIRCLE_ZONE") Icons.Default.RadioButtonChecked else Icons.Default.AltRoute,
                            contentDescription = "Route Type",
                            tint = CyberCyanPrimary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = route.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (route.type == "CIRCLE_ZONE") "ขอบเขตทรงกลม Geofence Zone" else "แนวระเบียงเส้นทาง Corridor",
                            color = CyberCyanPrimary,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = EmeraldSafe.copy(alpha = 0.2f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = EmeraldSafe,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "เปิดใช้งาน",
                            color = EmeraldSafe,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Start - End Points
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Start",
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${route.startLocationName} ➔ ${route.endLocationName}",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tolerance & Speed Rules Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "ระยะเบี่ยงเบนที่ยอมรับ", color = Color.Gray, fontSize = 10.sp)
                        Text(
                            text = "${route.toleranceMeters} เมตร",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "จำกัดความเร็วสูงสุด", color = Color.Gray, fontSize = 10.sp)
                        Text(
                            text = "${route.maxAllowedSpeed} กม./ชม.",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddRouteDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, type: String, start: String, end: String, tol: Int, maxSpeed: Int) -> Unit
) {
    var name by remember { mutableStateOf("เส้นทางใหม่: ถนนสุขุมวิท") }
    var startName by remember { mutableStateOf("แยกบางนา") }
    var endName by remember { mutableStateOf("เมืองพัทยา") }
    var toleranceMeters by remember { mutableFloatStateOf(200f) }
    var maxSpeed by remember { mutableFloatStateOf(100f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = "➕ เพิ่มเส้นทางควบคุมใหม่",
                color = Color(0xFF1D1B20),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ชื่อเส้นทาง / Geofence") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startName,
                        onValueChange = { startName = it },
                        label = { Text("จุดเริ่มต้น") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = endName,
                        onValueChange = { endName = it },
                        label = { Text("จุดปลายทาง") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ระยะเบี่ยงเบนที่อนุญาต: ${toleranceMeters.toInt()} เมตร",
                    color = Color(0xFF1D1B20),
                    fontSize = 12.sp
                )
                Slider(
                    value = toleranceMeters,
                    onValueChange = { toleranceMeters = it },
                    valueRange = 50f..1000f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF6750A4), activeTrackColor = Color(0xFF6750A4))
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "จำกัดความเร็วสูงสุด: ${maxSpeed.toInt()} กม./ชม.",
                    color = Color(0xFF1D1B20),
                    fontSize = 12.sp
                )
                Slider(
                    value = maxSpeed,
                    onValueChange = { maxSpeed = it },
                    valueRange = 40f..160f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF6750A4), activeTrackColor = Color(0xFF6750A4))
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(name, "ROUTE_CORRIDOR", startName, endName, toleranceMeters.toInt(), maxSpeed.toInt())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4), contentColor = Color.White),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("บันทึกเส้นทาง", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก", color = Color(0xFF49454F))
            }
        }
    )
}
