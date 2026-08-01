package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocationHistoryEntity
import com.example.data.VehicleEntity

@Composable
fun MapViewCanvas(
    vehicle: VehicleEntity?,
    historyPoints: List<LocationHistoryEntity>,
    modifier: Modifier = Modifier
) {
    var isSatelliteMode by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 42f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val textMeasurer = rememberTextMeasurer()

    val mapBgColor = if (isSatelliteMode) Color(0xFF0F172A) else Color(0xFFF8F9FA)
    val roadWhite = if (isSatelliteMode) Color(0xFF1E293B) else Color.White
    val roadBorder = if (isSatelliteMode) Color(0xFF334155) else Color(0xFFD1D5DB)
    val highwayYellow = if (isSatelliteMode) Color(0xFFD97706) else Color(0xFFFBBF24)
    val highwayBorder = if (isSatelliteMode) Color(0xFFB45309) else Color(0xFFD97706)
    val parkColor = if (isSatelliteMode) Color(0xFF14532D) else Color(0xFFDCFCE7)
    val waterColor = if (isSatelliteMode) Color(0xFF1E3A8A) else Color(0xFFBAE6FD)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(mapBgColor)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.4f, 4.0f)
                        panOffset += pan
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val centerLat = vehicle?.currentLat ?: 13.7381
            val centerLng = vehicle?.currentLng ?: 100.6283

            val scalePx = 22000f * zoomScale

            fun latLngToOffset(lat: Double, lng: Double): Offset {
                val dx = (lng - centerLng) * scalePx
                val dy = -(lat - centerLat) * scalePx
                return Offset(
                    x = (canvasWidth / 2f) + dx.toFloat() + panOffset.x,
                    y = (canvasHeight / 2f) + dy.toFloat() + panOffset.y
                )
            }

            // 1. River Curves & Canals
            val riverPath = Path().apply {
                val p1 = latLngToOffset(13.820, 100.510)
                val p2 = latLngToOffset(13.770, 100.520)
                val p3 = latLngToOffset(13.730, 100.490)
                val p4 = latLngToOffset(13.680, 100.530)
                val p5 = latLngToOffset(13.630, 100.580)
                moveTo(p1.x, p1.y)
                quadraticTo(p2.x, p2.y, p3.x, p3.y)
                quadraticTo(p4.x, p4.y, p5.x, p5.y)
            }
            drawPath(
                path = riverPath,
                color = waterColor,
                style = Stroke(
                    width = 28.dp.toPx() * zoomScale,
                    cap = StrokeCap.Round
                )
            )

            // 2. Airport Grounds & Parks
            val airportCenter = latLngToOffset(13.6900, 100.7500)
            drawRoundRect(
                color = if (isSatelliteMode) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                topLeft = Offset(airportCenter.x - 45.dp.toPx() * zoomScale, airportCenter.y - 65.dp.toPx() * zoomScale),
                size = Size(90.dp.toPx() * zoomScale, 130.dp.toPx() * zoomScale),
                cornerRadius = CornerRadius(16f, 16f)
            )

            val parkOffset = latLngToOffset(13.6870, 100.6620)
            drawCircle(
                color = parkColor,
                radius = 50.dp.toPx() * zoomScale,
                center = parkOffset
            )

            // 3. Real Vector Road Network
            drawRealRoadNetwork(
                scalePx = scalePx,
                panOffset = panOffset,
                zoomScale = zoomScale,
                roadWhite = roadWhite,
                roadBorder = roadBorder,
                highwayYellow = highwayYellow,
                highwayBorder = highwayBorder,
                latLngToOffset = ::latLngToOffset
            )

            // 4. Landmarks & Road Badges
            val landmarks = listOf(
                Pair("7 มอเตอร์เวย์ กรุงเทพฯ-ชลบุรี", latLngToOffset(13.7380, 100.6782)),
                Pair("9 วงแหวนกาญจนาภิเษก", latLngToOffset(13.7292, 100.6880)),
                Pair("34 บางนา-ตราด", latLngToOffset(13.6500, 100.7000)),
                Pair("ถ.พระราม 9", latLngToOffset(13.7500, 100.6200)),
                Pair("ถ.ลาดกระบัง", latLngToOffset(13.7200, 100.7700)),
                Pair("✈️ สนามบินสุวรรณภูมิ", latLngToOffset(13.6900, 100.7500)),
                Pair("🏢 คลังสินค้า ICD", latLngToOffset(13.7320, 100.7100))
            )

            landmarks.forEach { (name, pos) ->
                val textLayout = textMeasurer.measure(
                    text = name,
                    style = TextStyle(
                        color = if (isSatelliteMode) Color.White else Color(0xFF334155),
                        fontSize = (11 * zoomScale.coerceIn(0.8f, 1.3f)).sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                drawRoundRect(
                    color = if (isSatelliteMode) Color(0xDD0F172A) else Color(0xEEFFFFFF),
                    topLeft = Offset(pos.x - textLayout.size.width / 2f - 8f, pos.y - 4f),
                    size = Size(textLayout.size.width + 16f, textLayout.size.height + 8f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(pos.x - textLayout.size.width / 2f, pos.y)
                )
            }

            // 5. GPS History Path
            if (historyPoints.size > 1) {
                val historyPath = Path()
                val hOffsets = historyPoints.map { latLngToOffset(it.latitude, it.longitude) }
                historyPath.moveTo(hOffsets[0].x, hOffsets[0].y)
                for (i in 1 until hOffsets.size) {
                    historyPath.lineTo(hOffsets[i].x, hOffsets[i].y)
                }
                drawPath(
                    path = historyPath,
                    color = Color(0xFF2563EB).copy(alpha = 0.7f),
                    style = Stroke(
                        width = 5.dp.toPx() * zoomScale,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 6. Vehicle Position & Red PostCarTrack Icon
            if (vehicle != null) {
                val carPos = latLngToOffset(vehicle.currentLat, vehicle.currentLng)

                drawCircle(
                    color = Color(0xFFEF4444).copy(alpha = pulseAlpha),
                    radius = pulseScale * zoomScale,
                    center = carPos
                )

                rotate(degrees = vehicle.headingBearing, pivot = carPos) {
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.35f),
                        topLeft = Offset(carPos.x - 14.dp.toPx(), carPos.y - 24.dp.toPx() + 3.dp.toPx()),
                        size = Size(28.dp.toPx(), 48.dp.toPx()),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0xFFEF4444),
                        topLeft = Offset(carPos.x - 14.dp.toPx(), carPos.y - 24.dp.toPx()),
                        size = Size(28.dp.toPx(), 48.dp.toPx()),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(carPos.x - 10.dp.toPx(), carPos.y - 14.dp.toPx()),
                        size = Size(20.dp.toPx(), 24.dp.toPx()),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(carPos.x - 9.dp.toPx(), carPos.y - 12.dp.toPx()),
                        size = Size(18.dp.toPx(), 7.dp.toPx()),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFFEF08A),
                        radius = 3.5.dp.toPx(),
                        center = Offset(carPos.x - 9.dp.toPx(), carPos.y - 22.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFFEF08A),
                        radius = 3.5.dp.toPx(),
                        center = Offset(carPos.x + 9.dp.toPx(), carPos.y - 22.dp.toPx())
                    )
                }

                val tagTextResult = textMeasurer.measure(
                    text = "${vehicle.name} • ${vehicle.licensePlate}",
                    style = TextStyle(
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                val tagX = carPos.x - tagTextResult.size.width / 2f
                val tagY = carPos.y - 50.dp.toPx()

                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(tagX - 10f, tagY - 6f),
                    size = Size(tagTextResult.size.width + 20f, tagTextResult.size.height + 12f),
                    cornerRadius = CornerRadius(16f, 16f)
                )
                drawRoundRect(
                    color = Color(0xFFEF4444),
                    topLeft = Offset(tagX - 10f, tagY - 6f),
                    size = Size(tagTextResult.size.width + 20f, tagTextResult.size.height + 12f),
                    cornerRadius = CornerRadius(16f, 16f),
                    style = Stroke(width = 2.dp.toPx())
                )

                drawText(
                    textLayoutResult = tagTextResult,
                    topLeft = Offset(tagX, tagY)
                )
            }
        }

        // Top Right Map Mode & Compass Buttons
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSatelliteMode) Color(0xEE1E293B) else Color.White,
                shadowElevation = 4.dp
            ) {
                IconButton(
                    onClick = { isSatelliteMode = !isSatelliteMode },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Toggle Layer",
                        tint = if (isSatelliteMode) Color(0xFFEF4444) else Color(0xFF5F6368),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = if (isSatelliteMode) Color(0xEE1E293B) else Color.White,
                shadowElevation = 4.dp
            ) {
                IconButton(
                    onClick = {
                        panOffset = Offset.Zero
                        zoomScale = 1.0f
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "North Compass",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Bottom Right Floating Controls (Recenter + Zoom with percentage display)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFEF4444),
                shadowElevation = 6.dp,
                contentColor = Color.White
            ) {
                IconButton(
                    onClick = {
                        panOffset = Offset.Zero
                        zoomScale = 1.0f
                    },
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "My Location",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSatelliteMode) Color(0xEE1E293B) else Color.White,
                shadowElevation = 4.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { zoomScale = (zoomScale * 1.25f).coerceAtMost(4.0f) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", tint = Color(0xFF334155))
                    }

                    Text(
                        text = "${(zoomScale * 100).toInt()}%",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    IconButton(
                        onClick = { zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.4f) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color(0xFF334155))
                    }
                }
            }
        }

        // Bottom Left Google Watermark Logo & Scale Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 120.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF4285F4), fontWeight = FontWeight.ExtraBold)) { append("G") }
                    withStyle(SpanStyle(color = Color(0xFFEA4335), fontWeight = FontWeight.ExtraBold)) { append("o") }
                    withStyle(SpanStyle(color = Color(0xFFFBBC05), fontWeight = FontWeight.ExtraBold)) { append("o") }
                    withStyle(SpanStyle(color = Color(0xFF4285F4), fontWeight = FontWeight.ExtraBold)) { append("g") }
                    withStyle(SpanStyle(color = Color(0xFF34A853), fontWeight = FontWeight.ExtraBold)) { append("l") }
                    withStyle(SpanStyle(color = Color(0xFFEA4335), fontWeight = FontWeight.ExtraBold)) { append("o") }
                },
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(if (isSatelliteMode) Color.White else Color(0xFF5F6368))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "100 ม. • Map data ©2026",
                    color = if (isSatelliteMode) Color.LightGray else Color(0xFF5F6368),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun DrawScope.drawRealRoadNetwork(
    scalePx: Float,
    panOffset: Offset,
    zoomScale: Float,
    roadWhite: Color,
    roadBorder: Color,
    highwayYellow: Color,
    highwayBorder: Color,
    latLngToOffset: (Double, Double) -> Offset
) {
    // 1. Grid of Local Streets
    val gridSpacing = 70.dp.toPx() * zoomScale
    var x = (panOffset.x % gridSpacing)
    while (x < size.width) {
        drawLine(
            color = roadBorder,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 5.dp.toPx()
        )
        drawLine(
            color = roadWhite,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 3.dp.toPx()
        )
        x += gridSpacing
    }

    var y = (panOffset.y % gridSpacing)
    while (y < size.height) {
        drawLine(
            color = roadBorder,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 5.dp.toPx()
        )
        drawLine(
            color = roadWhite,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 3.dp.toPx()
        )
        y += gridSpacing
    }

    // 2. Highway 7 (Motorway Bangkok - Chonburi)
    val hw7Points = listOf(
        latLngToOffset(13.7600, 100.5600),
        latLngToOffset(13.7420, 100.6000),
        latLngToOffset(13.7381, 100.6283),
        latLngToOffset(13.7292, 100.6782),
        latLngToOffset(13.7125, 100.7421),
        latLngToOffset(13.6821, 100.8251),
        latLngToOffset(13.5220, 100.9950)
    )
    val hw7Path = Path().apply {
        moveTo(hw7Points[0].x, hw7Points[0].y)
        for (i in 1 until hw7Points.size) {
            lineTo(hw7Points[i].x, hw7Points[i].y)
        }
    }
    drawPath(path = hw7Path, color = highwayBorder, style = Stroke(width = 14.dp.toPx() * zoomScale, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path = hw7Path, color = highwayYellow, style = Stroke(width = 10.dp.toPx() * zoomScale, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // 3. Highway 9 (Kanchanaphisek Ring Road)
    val hw9Points = listOf(
        latLngToOffset(13.9200, 100.6900),
        latLngToOffset(13.8200, 100.6900),
        latLngToOffset(13.7292, 100.6850),
        latLngToOffset(13.6300, 100.6800),
        latLngToOffset(13.5500, 100.6500)
    )
    val hw9Path = Path().apply {
        moveTo(hw9Points[0].x, hw9Points[0].y)
        for (i in 1 until hw9Points.size) {
            lineTo(hw9Points[i].x, hw9Points[i].y)
        }
    }
    drawPath(path = hw9Path, color = highwayBorder, style = Stroke(width = 12.dp.toPx() * zoomScale, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path = hw9Path, color = highwayYellow, style = Stroke(width = 8.dp.toPx() * zoomScale, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // 4. Highway 34 (Bang Na - Chonburi / Trad Expressway)
    val hw34Points = listOf(
        latLngToOffset(13.6680, 100.6000),
        latLngToOffset(13.6500, 100.6800),
        latLngToOffset(13.6200, 100.7800),
        latLngToOffset(13.5800, 100.9000)
    )
    val hw34Path = Path().apply {
        moveTo(hw34Points[0].x, hw34Points[0].y)
        for (i in 1 until hw34Points.size) {
            lineTo(hw34Points[i].x, hw34Points[i].y)
        }
    }
    drawPath(path = hw34Path, color = highwayBorder, style = Stroke(width = 12.dp.toPx() * zoomScale, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path = hw34Path, color = highwayYellow, style = Stroke(width = 8.dp.toPx() * zoomScale, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
