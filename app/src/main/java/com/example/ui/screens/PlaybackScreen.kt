package com.example.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberCyanPrimary
import com.example.ui.theme.EmeraldSafe
import kotlinx.coroutines.delay

@Composable
fun PlaybackScreen(
    viewModel: TrackingViewModel,
    modifier: Modifier = Modifier
) {
    val vehicle by viewModel.activeVehicle.collectAsState()
    val historyPoints by viewModel.activeHistoryPoints.collectAsState()

    var isPlaying by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    val currentPointIndex = (sliderPosition * (historyPoints.size - 1).coerceAtLeast(0)).toInt()
    val currentPoint = historyPoints.getOrNull(currentPointIndex)

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
            .background(Color(0xFFFEF7FF))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "History",
                tint = Color(0xFF6750A4),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "⏮️ เล่นย้อนหลังประวัติเส้นทาง (Trip Playback)",
                    color = Color(0xFF1D1B20),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ตรวจสอบการเดินทางและจุดที่ตรวจพบการออกนอกเส้นทางย้อนหลัง",
                    color = Color(0xFF49454F),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Replay Stats Overview
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "จุดบันทึกตำแหน่ง", color = Color(0xFF49454F), fontSize = 10.sp)
                    Text(
                        text = "${historyPoints.size} จุด",
                        color = Color(0xFF1D1B20),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "ความเร็วสูงสุด", color = Color(0xFF49454F), fontSize = 10.sp)
                    Text(
                        text = "${historyPoints.maxOfOrNull { it.speedKmh } ?: 0} กม./ชม.",
                        color = Color(0xFF6750A4),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "เบี่ยงออกนอกเส้นทาง", color = Color(0xFF49454F), fontSize = 10.sp)
                    Text(
                        text = "${historyPoints.count { it.isDeviationPoint }} จุด",
                        color = CrimsonAlert,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Speed Profile Canvas Graph
        Text(
            text = "📊 กราฟความเร็วตลอดการเดินทาง",
            color = Color(0xFF1D1B20),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
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
                            color = Color(0xFF6750A4),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Current playback marker line
                        val currentX = sliderPosition * w
                        drawLine(
                            color = Color(0xFF1D1B20),
                            start = Offset(currentX, 0f),
                            end = Offset(currentX, h),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                } else {
                    Text(
                        text = "ไม่มีข้อมูลประวัติการเดินทางเพียงพอ",
                        color = Color(0xFF49454F),
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Current Playback Frame Details
        if (currentPoint != null) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "เวลา: ${DateFormat.format("HH:mm:ss • dd/MM/yyyy", currentPoint.timestamp)}",
                            color = Color(0xFF1D1B20),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "พิกัด: ${String.format("%.4f", currentPoint.latitude)}, ${String.format("%.4f", currentPoint.longitude)}",
                            color = Color(0xFF49454F),
                            fontSize = 11.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (currentPoint.isDeviationPoint) CrimsonAlert else Color(0xFF6750A4)
                    ) {
                        Text(
                            text = "${currentPoint.speedKmh} km/h",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Timeline Slider & Control Player
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        sliderPosition = it
                        isPlaying = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF6750A4),
                        activeTrackColor = Color(0xFF6750A4)
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
                        Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Start", tint = Color(0xFF1D1B20))
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF6750A4)
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
                        Icon(imageVector = Icons.Default.SkipNext, contentDescription = "End", tint = Color(0xFF1D1B20))
                    }
                }
            }
        }
    }
}
