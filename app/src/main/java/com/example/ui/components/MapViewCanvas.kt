package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.PathEffect
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
import com.example.ui.theme.CyberCyanPrimary
import com.example.ui.theme.EmeraldSafe
import com.example.util.LatLng

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

    val mapBgColor = if (isSatelliteMode) Color(0xFF0F172A) else Color(0xFFF1F3F4)
    val roadWhite = if (isSatelliteMode) Color(0xFF334155) else Color.White
    val roadBorder = if (isSatelliteMode) Color(0xFF1E293B) else Color(0xFFCFD8DC)
    val highwayYellow = if (isSatelliteMode) Color(0xFFD97706) else Color(0xFFFDE293)
    val highwayBorder = if (isSatelliteMode) Color(0xFFB45309) else Color(0xFFF59E0B)
    val parkColor = if (isSatelliteMode) Color(0xFF14532D) else Color(0xFFC8E6C9)
    val waterColor = if (isSatelliteMode) Color(0xFF1E3A8A) else Color(0xFFA5C9FF)

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
                        zoomScale = (zoomScale * zoom).coerceIn(0.5f, 4.0f)
                        panOffset += pan
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val centerLat = vehicle?.currentLat ?: 13.7381
            val centerLng = vehicle?.currentLng ?: 100.6283

            val scalePx = 28000f * zoomScale

            fun latLngToOffset(lat: Double, lng: Double): Offset {
                val dx = (lng - centerLng) * scalePx
                val dy = -(lat - centerLat) * scalePx
                return Offset(
                    x = (canvasWidth / 2f) + dx.toFloat() + panOffset.x,
                    y = (canvasHeight / 2f) + dy.toFloat() + panOffset.y
                )
            }

            // 1. Draw Google Maps Urban Grid & Parcels
            drawGoogleMapsLandParcels(scalePx, panOffset, isSatelliteMode)

            // 2. Draw Green Parks & Waterways
            val parkOffset = latLngToOffset(13.7420, 100.6400)
            drawCircle(
                color = parkColor,
                radius = 80.dp.toPx() * zoomScale,
                center = parkOffset
            )

            val waterPath = Path().apply {
                val start = latLngToOffset(13.7600, 100.5800)
                val mid = latLngToOffset(13.7300, 100.6500)
                val end = latLngToOffset(13.7000, 100.7500)
                moveTo(start.x, start.y)
                quadraticTo(mid.x, mid.y, end.x, end.y)
            }
            drawPath(
                path = waterPath,
                color = waterColor,
                style = Stroke(
                    width = 24.dp.toPx() * zoomScale,
                    cap = StrokeCap.Round
                )
            )

            // 3. Draw Street & Highway Grid (Google Maps Style)
            drawGoogleMapsRoads(
                scalePx = scalePx,
                panOffset = panOffset,
                zoomScale = zoomScale,
                roadWhite = roadWhite,
                roadBorder = roadBorder,
                highwayYellow = highwayYellow,
                highwayBorder = highwayBorder,
                latLngToOffset = ::latLngToOffset
            )

            // 4. Draw Road Labels & Major Landmarks
            val landmarks = listOf(
                Pair("ถ.มอเตอร์เวย์ 7", latLngToOffset(13.7292, 100.6782)),
                Pair("ถ.พระราม 9", latLngToOffset(13.7500, 100.6200)),
                Pair("เขตลาดกระบัง", latLngToOffset(13.7200, 100.7700)),
                Pair("✈️ สนามบินสุวรรณภูมิ", latLngToOffset(13.6900, 100.7500))
            )

            landmarks.forEach { (name, pos) ->
                val textLayout = textMeasurer.measure(
                    text = name,
                    style = TextStyle(
                        color = if (isSatelliteMode) Color.LightGray else Color(0xFF4A5568),
                        fontSize = (11 * zoomScale.coerceIn(0.8f, 1.3f)).sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(pos.x - textLayout.size.width / 2f, pos.y)
                )
            }

            // 5. Draw Location Breadcrumbs (History Path)
            if (historyPoints.size > 1) {
                val historyPath = Path()
                val hOffsets = historyPoints.map { latLngToOffset(it.latitude, it.longitude) }
                historyPath.moveTo(hOffsets[0].x, hOffsets[0].y)
                for (i in 1 until hOffsets.size) {
                    historyPath.lineTo(hOffsets[i].x, hOffsets[i].y)
                }
                drawPath(
                    path = historyPath,
                    color = Color(0xFF1A73E8).copy(alpha = 0.6f),
                    style = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 6. Draw Vehicle Position (Car Icon & Blue Pulse)
            if (vehicle != null) {
                val carPos = latLngToOffset(vehicle.currentLat, vehicle.currentLng)

                // Google Maps Blue Location Pulse
                drawCircle(
                    color = Color(0xFF1A73E8).copy(alpha = pulseAlpha),
                    radius = pulseScale * zoomScale,
                    center = carPos
                )

                // Top-Down Rotating Car Icon
                rotate(degrees = vehicle.headingBearing, pivot = carPos) {
                    // Car Shadow
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.35f),
                        topLeft = Offset(carPos.x - 14.dp.toPx(), carPos.y - 24.dp.toPx() + 3.dp.toPx()),
                        size = Size(28.dp.toPx(), 48.dp.toPx()),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                    // Main Car Body (Sporty Cyan)
                    drawRoundRect(
                        color = Color(0xFF0284C7),
                        topLeft = Offset(carPos.x - 14.dp.toPx(), carPos.y - 24.dp.toPx()),
                        size = Size(28.dp.toPx(), 48.dp.toPx()),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                    // Roof Dark Glass
                    drawRoundRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(carPos.x - 10.dp.toPx(), carPos.y - 14.dp.toPx()),
                        size = Size(20.dp.toPx(), 24.dp.toPx()),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                    // Front Windshield
                    drawRoundRect(
                        color = Color(0xFF38BDF8),
                        topLeft = Offset(carPos.x - 9.dp.toPx(), carPos.y - 12.dp.toPx()),
                        size = Size(18.dp.toPx(), 7.dp.toPx()),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                    // Rear Glass
                    drawRoundRect(
                        color = Color(0xFF38BDF8),
                        topLeft = Offset(carPos.x - 8.dp.toPx(), carPos.y + 4.dp.toPx()),
                        size = Size(16.dp.toPx(), 4.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    // Headlights (Yellow Glow)
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
                    // Taillights (Red)
                    drawRoundRect(
                        color = Color(0xFFEF4444),
                        topLeft = Offset(carPos.x - 11.dp.toPx(), carPos.y + 21.dp.toPx()),
                        size = Size(6.dp.toPx(), 3.dp.toPx()),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0xFFEF4444),
                        topLeft = Offset(carPos.x + 5.dp.toPx(), carPos.y + 21.dp.toPx()),
                        size = Size(6.dp.toPx(), 3.dp.toPx()),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )
                }

                // Vehicle License Plate Tag above Car Icon
                val labelText = "🚘 ${vehicle.licensePlate} (${vehicle.speedKmh} กม./ชม.)"
                val tagTextResult = textMeasurer.measure(
                    text = labelText,
                    style = TextStyle(color = Color(0xFF202124), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )

                val tagX = carPos.x - (tagTextResult.size.width / 2f)
                val tagY = carPos.y - 42.dp.toPx()

                // White Callout Pill Card
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(tagX - 10f, tagY - 6f),
                    size = Size(tagTextResult.size.width + 20f, tagTextResult.size.height + 12f),
                    cornerRadius = CornerRadius(16f, 16f)
                )
                drawRoundRect(
                    color = Color(0xFFDADCE0),
                    topLeft = Offset(tagX - 10f, tagY - 6f),
                    size = Size(tagTextResult.size.width + 20f, tagTextResult.size.height + 12f),
                    cornerRadius = CornerRadius(16f, 16f),
                    style = Stroke(width = 1.dp.toPx())
                )

                drawText(
                    textLayoutResult = tagTextResult,
                    topLeft = Offset(tagX, tagY)
                )
            }
        }

        // Top Search Bar (Google Maps Search Header)
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = if (isSatelliteMode) Color(0xEE1E293B) else Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ค้นหาใน Google Maps",
                        color = if (isSatelliteMode) Color.LightGray else Color(0xFF5F6368),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldSafe.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "GPS LIVE 🟢",
                            color = EmeraldSafe,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice",
                        tint = Color(0xFFEA4335),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Top Right Map Mode & Compass Buttons
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 16.dp),
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
                        tint = if (isSatelliteMode) Color(0xFF4285F4) else Color(0xFF5F6368),
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
                        tint = Color(0xFFEA4335),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Bottom Right Floating Navigation Controls (Recenter + Zoom In/Out)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF1A73E8),
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
                Column {
                    IconButton(
                        onClick = { zoomScale = (zoomScale * 1.25f).coerceAtMost(4.0f) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", tint = Color(0xFF5F6368))
                    }
                    IconButton(
                        onClick = { zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.5f) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color(0xFF5F6368))
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
            // Google Colored Logo
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF4285F4), fontWeight = FontWeight.ExtraBold)) { append("G") }
                    withStyle(SpanStyle(color = Color(0xFFEA4335), fontWeight = FontWeight.ExtraBold)) { append("o") }
                    withStyle(SpanStyle(color = Color(0xFFFBBC05), fontWeight = FontWeight.ExtraBold)) { append("o") }
                    withStyle(SpanStyle(color = Color(0xFF4285F4), fontWeight = FontWeight.ExtraBold)) { append("g") }
                    withStyle(SpanStyle(color = Color(0xFF34A853), fontWeight = FontWeight.ExtraBold)) { append("l") }
                    withStyle(SpanStyle(color = Color(0xFFEA4335), fontWeight = FontWeight.ExtraBold)) { append("e") }
                },
                fontSize = 16.sp
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

private fun DrawScope.drawGoogleMapsLandParcels(
    scalePx: Float,
    panOffset: Offset,
    isSatelliteMode: Boolean
) {
    val parcelColor = if (isSatelliteMode) Color(0xFF1E293B) else Color(0xFFE8ECEB)
    val width = size.width
    val height = size.height

    val spacing = 120.dp.toPx()
    var x = (panOffset.x % spacing)
    while (x < width) {
        var y = (panOffset.y % spacing)
        while (y < height) {
            drawRoundRect(
                color = parcelColor,
                topLeft = Offset(x + 10f, y + 10f),
                size = Size(spacing - 20f, spacing - 20f),
                cornerRadius = CornerRadius(12f, 12f)
            )
            y += spacing
        }
        x += spacing
    }
}

private fun DrawScope.drawGoogleMapsRoads(
    scalePx: Float,
    panOffset: Offset,
    zoomScale: Float,
    roadWhite: Color,
    roadBorder: Color,
    highwayYellow: Color,
    highwayBorder: Color,
    latLngToOffset: (Double, Double) -> Offset
) {
    // 1. Draw Secondary Streets
    val gridSpacing = 90.dp.toPx() * zoomScale
    var x = (panOffset.x % gridSpacing)
    while (x < size.width) {
        // Casing / Border
        drawLine(
            color = roadBorder,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 7.dp.toPx()
        )
        // White Fill
        drawLine(
            color = roadWhite,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 5.dp.toPx()
        )
        x += gridSpacing
    }

    var y = (panOffset.y % gridSpacing)
    while (y < size.height) {
        drawLine(
            color = roadBorder,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 7.dp.toPx()
        )
        drawLine(
            color = roadWhite,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 5.dp.toPx()
        )
        y += gridSpacing
    }

    // 2. Draw Main Highway (Motorway 7 / Rama 9 Expressway)
    val hwStart = latLngToOffset(13.7550, 100.5800)
    val hwMid = latLngToOffset(13.7292, 100.6782)
    val hwEnd = latLngToOffset(13.7000, 100.8200)

    val hwPath = Path().apply {
        moveTo(hwStart.x, hwStart.y)
        lineTo(hwMid.x, hwMid.y)
        lineTo(hwEnd.x, hwEnd.y)
    }

    // Highway Border / Casing
    drawPath(
        path = hwPath,
        color = highwayBorder,
        style = Stroke(
            width = 13.dp.toPx() * zoomScale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Highway Yellow Fill
    drawPath(
        path = hwPath,
        color = highwayYellow,
        style = Stroke(
            width = 10.dp.toPx() * zoomScale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
