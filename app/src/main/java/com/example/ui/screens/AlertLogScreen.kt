package com.example.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AlertEntity
import com.example.ui.TrackingViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberCyanPrimary
import com.example.ui.theme.EmeraldSafe
import com.example.util.GeoUtils

@Composable
fun AlertLogScreen(
    viewModel: TrackingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val alerts by viewModel.allAlerts.collectAsState()
    val speedLimitKmh by viewModel.speedLimitKmh.collectAsState()
    val isOverspeeding by viewModel.isOverspeeding.collectAsState()
    val activeVehicle by viewModel.activeVehicle.collectAsState()

    val speedAlerts = alerts.filter { it.alertType == "SPEEDING" || it.title.contains("ความเร็ว") }
    val unackCount = speedAlerts.count { !it.isAcknowledged }
    val maxSpeedRecord = speedAlerts.maxOfOrNull {
        // extract speed from title or description if possible
        try {
            val numStr = it.description.replace(Regex("[^0-9]"), " ").trim().split("\\s+".toRegex()).firstOrNull { n -> n.toIntOrNull() != null && n.toInt() > 30 }
            numStr?.toInt() ?: 0
        } catch (e: Exception) { 0 }
    } ?: activeVehicle?.speedKmh ?: 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFEF7FF))
            .padding(16.dp)
    ) {
        // Screen Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "🚨 แจ้งเตือนรถขับเกินความเร็ว",
                    color = Color(0xFF1D1B20),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ระบบตรวจจับและบันทึกเหตุการณ์ขับเร็วเกินกำหนด",
                    color = Color(0xFF49454F),
                    fontSize = 12.sp
                )
            }

            Surface(
                shape = CircleShape,
                color = if (unackCount > 0) CrimsonAlert.copy(alpha = 0.15f) else EmeraldSafe.copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (unackCount > 0) "$unackCount รายการใหม่" else "ปกติ",
                    color = if (unackCount > 0) CrimsonAlert else EmeraldSafe,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Fixed Speed Limit Control Panel Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Speed Limit",
                        tint = CrimsonAlert,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ขีดจำกัดความเร็วคงที่ 90 กม./ชม.",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ตามกฎหมายกำหนด (ตรวจจับและแจ้งเตือนอัตโนมัติ)",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CrimsonAlert
                ) {
                    Text(
                        text = "90 กม./ชม.",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Summary Stats Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "ความเร็วสูงสุดที่เคยขับ", color = Color.Gray, fontSize = 10.sp)
                    Text(
                        text = "$maxSpeedRecord กม./ชม.",
                        color = if (maxSpeedRecord > speedLimitKmh) CrimsonAlert else Color(0xFF1D1B20),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "จำนวนครั้งที่ขับเร็วเกิน", color = Color.Gray, fontSize = 10.sp)
                    Text(
                        text = "${speedAlerts.size} ครั้ง",
                        color = CrimsonAlert,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "สถานะปัจจุบัน", color = Color.Gray, fontSize = 10.sp)
                    Text(
                        text = if (isOverspeeding) "⚠️ ขับเกิน!" else "🟢 ปลอดภัย",
                        color = if (isOverspeeding) CrimsonAlert else EmeraldSafe,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Overspeed Logs Section
        Text(
            text = "📋 ประวัติบันทึกการขับรถเกินความเร็ว",
            color = Color(0xFF1D1B20),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (speedAlerts.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Safe",
                        tint = EmeraldSafe,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "ไม่พบประวัติขับเกินความเร็ว", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold)
                    Text(text = "ผู้ขับขี่ควบคุมความเร็วอยู่ในเกณฑ์ปลอดภัย ($speedLimitKmh กม./ชม.)", color = Color(0xFF49454F), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(speedAlerts) { alert ->
                    OverspeedAlertCardItem(
                        alert = alert,
                        speedLimitKmh = speedLimitKmh,
                        onAcknowledge = { viewModel.acknowledgeAlert(alert.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun OverspeedAlertCardItem(
    alert: AlertEntity,
    speedLimitKmh: Int,
    onAcknowledge: () -> Unit
) {
    val formattedTime = DateFormat.format("HH:mm:ss • dd/MM/yyyy", alert.timestamp).toString()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!alert.isAcknowledged) Color(0xFFFFF0F3) else Color.White
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CrimsonAlert.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = alert.title,
                            tint = CrimsonAlert,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = alert.title,
                            color = Color(0xFF1D1B20),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${alert.vehicleName} (${alert.licensePlate})",
                            color = Color(0xFF6750A4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = formattedTime,
                    color = Color(0xFF49454F),
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = alert.description,
                color = Color(0xFF1D1B20),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CrimsonAlert.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "ขีดจำกัดความเร็ว: $speedLimitKmh กม./ชม.",
                        color = CrimsonAlert,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                if (alert.isAcknowledged) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Acknowledged",
                            tint = EmeraldSafe,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "รับทราบแล้ว",
                            color = EmeraldSafe,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Button(
                        onClick = onAcknowledge,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6750A4),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Ack", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "ยืนยันรับทราบ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
