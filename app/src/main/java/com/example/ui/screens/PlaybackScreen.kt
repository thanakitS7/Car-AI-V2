package com.example.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocationHistoryEntity
import com.example.ui.TrackingViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberCyanPrimary
import com.example.ui.theme.EmeraldSafe
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlaybackScreen(
    viewModel: TrackingViewModel,
    modifier: Modifier = Modifier
) {
    val vehicle by viewModel.activeVehicle.collectAsState()
    val historyPoints by viewModel.activeHistoryPoints.collectAsState()
    val tripDistanceMeters by viewModel.tripDistanceMeters.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    val currentPointIndex = (sliderPosition * (historyPoints.size - 1).coerceAtLeast(0)).toInt()
    val currentPoint = historyPoints.getOrNull(currentPointIndex)

    val todayStr = remember {
        SimpleDateFormat("dd MMMM yyyy", Locale("th", "TH")).format(Date())
    }

    // Calculate today statistics
    val maxSpeedToday = historyPoints.maxOfOrNull { it.speedKmh } ?: 0
    val avgSpeedToday = if (historyPoints.isNotEmpty()) {
        (historyPoints.sumOf { it.speedKmh } / historyPoints.size)
    } else 0
    val totalDeviations = historyPoints.count { it.isDeviationPoint }
    val tripDistanceKm = (tripDistanceMeters / 1000.0)

    val startTimeStr = historyPoints.firstOrNull()?.timestamp?.let {
        DateFormat.format("HH:mm", it).toString()
    } ?: "--:--"

    val endTimeStr = historyPoints.lastOrNull()?.timestamp?.let {
        DateFormat.format("HH:mm", it).toString()
    } ?: "--:--"

    LaunchedEffect(isPlaying, historyPoints) {
        if (isPlaying && historyPoints.isNotEmpty()) {
            while (isPlaying) {
                delay(800)
                if (sliderPosition < 1.0f) {
                    sliderPosition = (sliderPosition + (1f / historyPoints.size)).coerceAtMost(1f)
                } else {
                    isPlaying = false
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEF4444).copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Today Route",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "📅 ประวัติเส้นทางที่วิ่งของวันปัจจุบัน",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ประจำวันที่ $todayStr • รถคันติดตาม: ${vehicle?.name ?: "คันหลัก"}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Vehicle & Today's Summary Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${vehicle?.name ?: "ยานพาหนะ"} (${vehicle?.licensePlate ?: "ทะเบียน -"})",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "เวลาวิ่ง: $startTimeStr - $endTimeStr น.",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4 Grid Stats Cards
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Distance Today
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "ระยะทางวันนี้", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            Text(
                                text = "${String.format(Locale.US, "%.1f", tripDistanceKm)} กม.",
                                color = Color(0xFF38BDF8),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Max Speed
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "ความเร็วสูงสุด", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            Text(
                                text = "$maxSpeedToday กม./ชม.",
                                color = Color(0xFFF59E0B),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Total Points
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "บันทึกพิกัด", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            Text(
                                text = "${historyPoints.size} จุด",
                                color = Color(0xFFA855F7),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Deviations
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "ออกนอกเส้นทาง", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            Text(
                                text = "$totalDeviations จุด",
                                color = if (totalDeviations > 0) CrimsonAlert else EmeraldSafe,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // View Mode Tab Switcher
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF1E293B),
            contentColor = Color(0xFFEF4444),
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🎬 เล่นย้อนหลัง (Playback)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                selectedContentColor = Color(0xFFEF4444),
                unselectedContentColor = Color(0xFF94A3B8)
            )

            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📋 บันทึกไทม์ไลน์วันนี้", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                selectedContentColor = Color(0xFFEF4444),
                unselectedContentColor = Color(0xFF94A3B8)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            // TAB 0: Playback Player & Speed Profile Graph
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "📊 กราฟความเร็วตลอดการเดินทางวันนี้",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        if (historyPoints.size > 1) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val maxSpd = (historyPoints.maxOfOrNull { it.speedKmh } ?: 120).toFloat().coerceAtLeast(10f)

                                val path = Path()
                                historyPoints.forEachIndexed { i, p ->
                                    val x = (i.toFloat() / (historyPoints.size - 1)) * w
                                    val y = h - ((p.speedKmh.toFloat() / maxSpd) * h)
                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)

                                    if (p.isDeviationPoint) {
                                        drawCircle(color = CrimsonAlert, radius = 6f, center = Offset(x, y))
                                    }
                                }

                                drawPath(
                                    path = path,
                                    color = Color(0xFF38BDF8),
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // Current playback marker line
                                val currentX = sliderPosition * w
                                drawLine(
                                    color = Color(0xFFEF4444),
                                    start = Offset(currentX, 0f),
                                    end = Offset(currentX, h),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                        } else {
                            Text(
                                text = "ไม่มีข้อมูลประวัติการเดินทางเพียงพอของวันนี้",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Current Point Details
                if (currentPoint != null) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = "เวลาบันทึก: ${DateFormat.format("HH:mm:ss", currentPoint.timestamp)} น.",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "พิกัด: ${String.format(Locale.US, "%.5f", currentPoint.latitude)}, ${String.format(Locale.US, "%.5f", currentPoint.longitude)}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (currentPoint.isDeviationPoint) CrimsonAlert else Color(0xFF0284C7)
                            ) {
                                Text(
                                    text = "${currentPoint.speedKmh} km/h",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Timeline Controller
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Slider(
                            value = sliderPosition,
                            onValueChange = {
                                sliderPosition = it
                                isPlaying = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFEF4444),
                                activeTrackColor = Color(0xFFEF4444),
                                inactiveTrackColor = Color(0xFF334155)
                            )
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = {
                                sliderPosition = 0f
                                isPlaying = false
                            }) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Start", tint = Color.White)
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEF4444)
                            ) {
                                IconButton(onClick = { isPlaying = !isPlaying }) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White
                                    )
                                }
                            }

                            IconButton(onClick = {
                                sliderPosition = 1f
                                isPlaying = false
                            }) {
                                Icon(Icons.Default.SkipNext, contentDescription = "End", tint = Color.White)
                            }
                        }
                    }
                }
            }
        } else {
            // TAB 1: Today's Timeline Logs List
            if (historyPoints.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("ไม่มีประวัติการบันทึกพิกัดของวันนี้", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(historyPoints.reversed()) { index, item ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (item.speedKmh > 3) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (item.speedKmh > 3) Icons.Default.DirectionsCar else Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = if (item.speedKmh > 3) Color(0xFF10B981) else Color(0xFFF59E0B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = "เวลา: ${DateFormat.format("HH:mm:ss", item.timestamp)} น.",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Lat: ${String.format(Locale.US, "%.4f", item.latitude)}, Lng: ${String.format(Locale.US, "%.5f", item.longitude)}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${item.speedKmh} กม./ชม.",
                                        color = if (item.speedKmh > 3) Color(0xFF38BDF8) else Color.LightGray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (item.isDeviationPoint) {
                                        Text(
                                            text = "⚠️ ออกนอกเส้นทาง",
                                            color = CrimsonAlert,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = if (item.speedKmh > 3) "MOVING" else "IDLE",
                                            color = if (item.speedKmh > 3) Color(0xFF10B981) else Color(0xFFF59E0B),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
