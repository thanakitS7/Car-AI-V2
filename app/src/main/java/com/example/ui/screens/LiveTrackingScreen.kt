package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.RouteGeofenceEntity
import com.example.data.VehicleEntity
import com.example.ui.TrackingViewModel
import com.example.ui.components.MapViewCanvas
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberCyanPrimary
import com.example.ui.theme.EmeraldSafe
import com.example.util.GeoUtils

@Composable
fun LiveTrackingScreen(
    viewModel: TrackingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vehicles by viewModel.allVehicles.collectAsState()
    val activeVehicle by viewModel.activeVehicle.collectAsState()
    val activeRoute by viewModel.activeRoute.collectAsState()
    val waypoints by viewModel.activeWaypoints.collectAsState()
    val historyPoints by viewModel.activeHistoryPoints.collectAsState()
    val isTripActive by viewModel.isTripActive.collectAsState()
    val isTripPaused by viewModel.isTripPaused.collectAsState()
    val tripSummary by viewModel.tripSummary.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()
    val isDeviatedTestMode by viewModel.isDeviatedTestMode.collectAsState()
    val simSpeedMultiplier by viewModel.simulationSpeedMultiplier.collectAsState()
    val googleSheetsUrl by viewModel.googleSheetsUrl.collectAsState()
    val isGoogleSheetsSyncEnabled by viewModel.isGoogleSheetsSyncEnabled.collectAsState()
    val lastSyncStatus by viewModel.lastSyncStatus.collectAsState()
    val isSyncingInProcess by viewModel.isSyncingInProcess.collectAsState()
    val tripDistanceMeters by viewModel.tripDistanceMeters.collectAsState()
    val speedLimitKmh by viewModel.speedLimitKmh.collectAsState()
    val isOverspeeding by viewModel.isOverspeeding.collectAsState()

    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showSensorInfoDialog by remember { mutableStateOf(false) }
    var showSpeedLimitDialog by remember { mutableStateOf(false) }
    var showLocationSearchDialog by remember { mutableStateOf(false) }

    // GPS Permission launcher
    var isGpsPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        isGpsPermissionGranted = granted
        viewModel.setGpsPermissionGranted(granted)
        if (granted) {
            Toast.makeText(context, "✅ เปิดใช้งานสิทธิ์ GPS มือถือแล้ว", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "⚠️ กรุณาอนุญาตตำแหน่ง GPS เพื่อติดตามพิกัดจริง", Toast.LENGTH_LONG).show()
        }
    }

    // Auto-prompt GPS permission safely after lifecycle is ready to prevent crashes
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500)
        if (!isGpsPermissionGranted) {
            try {
                val permsList = mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permsList.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                locationPermissionLauncher.launch(permsList.toTypedArray())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Connect real mobile GPS location updates
    DisposableEffect(isGpsPermissionGranted) {
        if (isGpsPermissionGranted) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val speedKmh = if (location.hasSpeed()) (location.speed * 3.6f).toInt() else 0
                    viewModel.updateRealGpsLocation(
                        lat = location.latitude,
                        lng = location.longitude,
                        speedKmh = speedKmh,
                        heading = location.bearing,
                        accuracy = location.accuracy
                    )
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasFine || hasCoarse) {
                try {
                    if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, locationListener)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 3f, locationListener)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            onDispose {
                try {
                    locationManager?.removeUpdates(locationListener)
                } catch (e: Exception) {}
            }
        } else {
            onDispose { }
        }
    }

    val currentVehicle = activeVehicle

    Box(modifier = modifier.fillMaxSize()) {
        // Main Interactive Google Maps GPS View
        MapViewCanvas(
            vehicle = currentVehicle,
            historyPoints = historyPoints,
            modifier = Modifier.fillMaxSize()
        )

        // Top Overlay: Vehicle Selector Chips & Trip Status
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 64.dp)
                .animateContentSize()
        ) {
            // Prominent Overspeed Warning Banner (Centered Underneath Bell Icon)
            if (isOverspeeding || (currentVehicle != null && currentVehicle.speedKmh > speedLimitKmh)) {
                Surface(
                    color = CrimsonAlert,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f)),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Bell Icon in a Glowing Badge
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Bell Alert",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Single Centered Line Warning Text
                        Text(
                            text = "🚨 เตือนความเร็ว",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            // Mandatory GPS Permission Prompt Banner if permission not granted
            if (!isGpsPermissionGranted) {
                Surface(
                    color = CrimsonAlert,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.GpsFixed, contentDescription = "GPS", tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "📍 บังคับเปิดใช้งาน GPS บนมือถือเพื่อรับส่งพิกัดสด",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = CrimsonAlert),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("อนุญาต GPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            // Unblocked Floating Google Maps Location Search Bar
            Surface(
                onClick = { showLocationSearchDialog = true },
                shape = RoundedCornerShape(24.dp),
                color = Color(0xF21E293B),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Google Maps",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "🔍 ค้นหาใน Google Maps (สถานที่/จุดหมาย)",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEF4444).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "GPS 🟢",
                            color = Color(0xFFEF4444),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Bottom Dashboard Card Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            if (currentVehicle != null) {
                VehicleTelemetryCard(
                    vehicle = currentVehicle,
                    vehicles = vehicles,
                    isTripActive = isTripActive,
                    isTripPaused = isTripPaused,
                    tripDistanceMeters = tripDistanceMeters,
                    speedLimitKmh = speedLimitKmh,
                    googleSheetsUrl = googleSheetsUrl,
                    lastSyncStatus = lastSyncStatus,
                    onSelectVehicle = { viewModel.selectVehicle(it) },
                    onAddVehicle = { showAddVehicleDialog = true },
                    onStartTrip = { viewModel.startTrip() },
                    onPauseTrip = { viewModel.pauseTrip() },
                    onEndTrip = { viewModel.endTrip() },
                    onSetSpeedLimit = { viewModel.setSpeedLimitKmh(it) },
                    onUpdateGoogleSheetsUrl = { viewModel.updateGoogleSheetsUrl(it) },
                    onUpdateDriverName = { viewModel.updateVehicleDriverName(currentVehicle.id, it) },
                    onShowSensorInfo = { showSensorInfoDialog = true }
                )
            } else {
                Card(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF21E293B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(CyberCyanPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "No vehicle",
                                tint = CyberCyanPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "ยังไม่มีข้อมูลรถในระบบ",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "กรุณาเพิ่มชื่อและเลขทะเบียนรถของคุณเพื่อเริ่มใช้ระบบติดตาม GPS",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showAddVehicleDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyanPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add vehicle", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("➕ เพิ่มรถคันแรกของคุณ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        if (showAddVehicleDialog) {
            AddVehicleDialog(
                onDismiss = { showAddVehicleDialog = false },
                onAdd = { name, plate, model, driver, office, provinceGroup ->
                    viewModel.addNewVehicle(name, plate, model, driver, office, provinceGroup)
                    showAddVehicleDialog = false
                    Toast.makeText(context, "เพิ่มยานพาหนะ $plate เรียบร้อย!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showSensorInfoDialog) {
            SensorInfoDialog(onDismiss = { showSensorInfoDialog = false })
        }

        if (showLocationSearchDialog) {
            GoogleLocationSearchDialog(
                onDismiss = { showLocationSearchDialog = false },
                onSelectLocation = { name, lat, lng ->
                    showLocationSearchDialog = false
                    viewModel.updateRealGpsLocation(lat, lng, 0, 0f, 5f)
                    Toast.makeText(context, "📍 ย้ายแผนที่ไปยัง $name (พิกัด $lat, $lng)", Toast.LENGTH_LONG).show()
                }
            )
        }

        tripSummary?.let { summary ->
            TripSummaryDialog(
                summary = summary,
                onDismiss = { viewModel.dismissTripSummary() }
            )
        }
    }
}

@Composable
fun SpeedLimitDialog(
    currentLimit: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedSpeed by remember { mutableStateOf(currentLimit) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Speed Limit",
                    tint = CrimsonAlert,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "⚙️ กำหนดขีดจำกัดความเร็ว",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "เมื่อรถขับเกินความเร็วนี้ ระบบจะแจ้งเตือนและบันทึกประวัติทันที",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "$selectedSpeed กม./ชม.",
                    color = CrimsonAlert,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Slider(
                    value = selectedSpeed.toFloat(),
                    onValueChange = { selectedSpeed = it.toInt() },
                    valueRange = 40f..150f,
                    steps = 21,
                    colors = SliderDefaults.colors(
                        thumbColor = CrimsonAlert,
                        activeTrackColor = CrimsonAlert,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf(60, 80, 90, 110, 120)
                    presets.forEach { speed ->
                        val isSel = selectedSpeed == speed
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) CrimsonAlert else Color(0xFF334155),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedSpeed = speed }
                        ) {
                            Text(
                                text = "$speed",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ยกเลิก", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(selectedSpeed)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert)
                    ) {
                        Text("บันทึกเกณฑ์", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleChipItem(
    vehicle: VehicleEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val statusColor = when (vehicle.status) {
        "ALERT_OUT_OF_ROUTE" -> CrimsonAlert
        "MOVING" -> EmeraldSafe
        "STOPPED" -> CrimsonAlert
        else -> AmberWarning
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF1E293B) else Color(0xCC0F172A),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyanPrimary) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(statusColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = vehicle.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = vehicle.licensePlate,
                    color = Color.LightGray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun OutofRouteAlertBanner(
    vehicle: VehicleEntity?,
    route: RouteGeofenceEntity?,
    onContactDriver: () -> Unit,
    onImmobilizeEngine: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CrimsonAlert),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Alert",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⚠️ เตือนภัย! ออกนอกเส้นทางที่กำหนด",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${vehicle?.name ?: "รถยนต์"} (${vehicle?.licensePlate ?: ""}) เคลื่อนที่ออกนอกกรอบ ${route?.name ?: "เส้นทางหลัก"}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onContactDriver,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = CrimsonAlert),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "ติดต่อคนขับ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onImmobilizeEngine,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "ตัดเครื่องยนต์", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun VehicleTelemetryCard(
    vehicle: VehicleEntity,
    vehicles: List<VehicleEntity> = emptyList(),
    isTripActive: Boolean,
    isTripPaused: Boolean,
    tripDistanceMeters: Double,
    speedLimitKmh: Int,
    googleSheetsUrl: String,
    lastSyncStatus: String,
    onSelectVehicle: (String) -> Unit = {},
    onAddVehicle: () -> Unit = {},
    onStartTrip: () -> Unit,
    onPauseTrip: () -> Unit,
    onEndTrip: () -> Unit,
    onSetSpeedLimit: (Int) -> Unit,
    onUpdateGoogleSheetsUrl: (String) -> Unit,
    onUpdateDriverName: (String) -> Unit,
    onShowSensorInfo: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    var showUrlEditDialog by remember { mutableStateOf(false) }
    var showDriverNameEditDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val distKm = tripDistanceMeters / 1000.0
    val distValStr = if (distKm >= 1.0) String.format("%.2f", distKm) else String.format("%.0f", tripDistanceMeters)
    val distUnitStr = if (distKm >= 1.0) "กม." else "เมตร"

    val placeName = remember(vehicle.currentLat, vehicle.currentLng) {
        GeoUtils.getPlaceName(context, vehicle.currentLat, vehicle.currentLng)
    }

    Card(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF21E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(top = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Drag Handle Header (swipe or tap to toggle expand/collapse)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 12) isExpanded = false
                            else if (dragAmount < -12) isExpanded = true
                        }
                    }
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .background(Color.Gray.copy(alpha = 0.6f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isExpanded) "ข้อมูลรถและตำแหน่ง GPS (ปัดลง/แตะเพื่อย่อ)" else "ข้อมูลรถและตำแหน่ง GPS (ปัดขึ้น/แตะเพื่อขยาย)",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = "Toggle Panel",
                        tint = CyberCyanPrimary
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Vehicle Switcher Chips & Add Vehicle Button
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    items(vehicles) { v ->
                        val isSelected = (v.id == vehicle.id)
                        Surface(
                            onClick = { onSelectVehicle(v.id) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) CyberCyanPrimary else Color(0xFF334155),
                            contentColor = if (isSelected) Color.Black else Color.White
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "🚘 ${v.name} (${v.licensePlate})",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            onClick = onAddVehicle,
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF6750A4),
                            contentColor = Color.White
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Vehicle", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ เพิ่มรถ/ใส่ทะเบียน", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Vehicle Header Status Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = vehicle.name,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ทะเบียน: ${vehicle.licensePlate} • ${vehicle.modelYear}",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "👤 ผู้ใช้รถ/คนขับ: ${vehicle.driverName}",
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF334155),
                                onClick = {
                                    Toast.makeText(context, "🔒 สำหรับ Admin เท่านั้น! กรุณาไปที่เมนู 'จัดการข้อมูล' เพื่อแก้ไขข้อมูลผู้ใช้รถและทะเบียนรถ", Toast.LENGTH_LONG).show()
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Admin Managed",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Admin Only", fontSize = 9.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }

                    StatusChip(status = vehicle.status, isLocked = vehicle.isEngineLocked)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Driver Trip Control Buttons (เริ่มเดินทาง / พักรถ / ถึงเป้าหมาย)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    if (!isTripActive) {
                        Button(
                            onClick = onStartTrip,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe, contentColor = Color.Black),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start Trip", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("🚀 เริ่มเดินทาง", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        Button(
                            onClick = onPauseTrip,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTripPaused) AmberWarning else Color(0xFF334155),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = if (isTripPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "Pause/Resume",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isTripPaused) "▶️ เดินทางต่อ" else "⏸️ พักรถ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = onEndTrip,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Arrived", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("🏁 ถึงเป้าหมาย", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Header for Telemetry with Info Explanation Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "📊 สถานะพิกัดสดและเกจวัด",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = onShowSensorInfo,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Sensor Info", tint = CyberCyanPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ℹ️ ความเร็ว/ระยะทาง วัดยังไง?", color = CyberCyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Telemetry Gauges 2x2 Grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Speed Meter Tile (Direct Phone GPS Speed)
                        TelemetryTile(
                            title = "ความเร็ว GPS",
                            value = "${vehicle.speedKmh}",
                            unit = "กม./ชม. สด",
                            icon = Icons.Default.Speed,
                            accentColor = if (vehicle.speedKmh > speedLimitKmh) CrimsonAlert else CyberCyanPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        // Speed Limit Tile (Fixed 90 km/h)
                        TelemetryTile(
                            title = "จำกัดความเร็ว",
                            value = "90",
                            unit = "กม./ชม. (กำหนดไว้ 90)",
                            icon = Icons.Default.Warning,
                            accentColor = CrimsonAlert,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Current Place Name / GPS Location Tile
                        TelemetryTile(
                            title = "สถานที่ปัจจุบัน",
                            value = placeName,
                            unit = String.format("GPS %.3f, %.3f", vehicle.currentLat, vehicle.currentLng),
                            icon = Icons.Default.MyLocation,
                            accentColor = EmeraldSafe,
                            modifier = Modifier.weight(1f)
                        )

                        // Trip Distance Tile (Start -> Goal)
                        TelemetryTile(
                            title = "ระยะทางเดินทาง",
                            value = distValStr,
                            unit = distUnitStr,
                            icon = Icons.Default.AltRoute,
                            accentColor = AmberWarning,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Clean background sync indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Sync Status",
                            tint = if (isTripActive) EmeraldSafe else Color.Gray,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isTripActive) "ซิงค์ Supabase สด: $lastSyncStatus" else "พร้อมซิงค์ Supabase เมื่อเริ่มเดินทาง",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }

                    IconButton(
                        onClick = { showUrlEditDialog = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Edit Webhook",
                            tint = CyberCyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                // Minimized / Collapsed Compact Summary Row (gives maximum map space!)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Vehicle",
                            tint = CyberCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${vehicle.name} (${vehicle.licensePlate})",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "⚡ ${vehicle.speedKmh} กม./ชม. • ระยะทาง: $distValStr ${if (distKm >= 1.0) "กม." else "ม."}",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isTripActive) {
                            Button(
                                onClick = onStartTrip,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("เริ่มเดินทาง", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onEndTrip,
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("ถึงที่หมาย", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUrlEditDialog) {
        var tempUrl by remember { mutableStateOf(googleSheetsUrl) }
        AlertDialog(
            onDismissRequest = { showUrlEditDialog = false },
            containerColor = Color.White,
            title = {
                Text("🔗 Google Sheets Webhook URL", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text("ใส่ URL ของ Google Apps Script (doPost/doGet Web App) เพื่อส่งข้อมูลพิกัดรถสดไปบันทึกลง Google Sheet:", fontSize = 12.sp, color = Color(0xFF49454F))
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✅ ลิ้งก์ Google Sheet จะได้รับข้อมูล driverName (ชื่อผู้ใช้รถ/คนขับ), vehicleName, licensePlate, timestamp, latitude, longitude, speedKmh, status อัตโนมัติ",
                            fontSize = 11.sp,
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("Webhook URL") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateGoogleSheetsUrl(tempUrl)
                        showUrlEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                ) {
                    Text("บันทึก URL")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlEditDialog = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    if (showDriverNameEditDialog) {
        var tempDriverName by remember { mutableStateOf(vehicle.driverName) }
        AlertDialog(
            onDismissRequest = { showDriverNameEditDialog = false },
            containerColor = Color.White,
            title = {
                Text("👤 แก้ไขชื่อผู้ใช้รถ / พนักงานขับรถ", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text("ชื่อนี้จะถูกบันทึกและส่งขึ้นระบบ Supabase พร้อมกับพิกัด GPS สดทุกครั้งที่มีการอัปเดต:", fontSize = 12.sp, color = Color(0xFF49454F))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempDriverName,
                        onValueChange = { tempDriverName = it },
                        label = { Text("ชื่อผู้ใช้รถ / พนักงานขับรถ") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempDriverName.isNotBlank()) {
                            onUpdateDriverName(tempDriverName)
                            showDriverNameEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                ) {
                    Text("บันทึกชื่อผู้ใช้รถ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDriverNameEditDialog = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}

@Composable
fun TelemetryTile(
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = if (value.length > 10) 12.sp else 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = unit,
                color = accentColor,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusChip(status: String, isLocked: Boolean) {
    val (label, bg, fg) = when {
        isLocked -> Triple("ล็อคเครื่องยนต์", CrimsonAlert, Color.White)
        status == "ALERT_OUT_OF_ROUTE" -> Triple("ออกนอกเส้นทาง", CrimsonAlert, Color.White)
        status == "MOVING" -> Triple("กำลังเคลื่อนที่", EmeraldSafe, Color.Black)
        else -> Triple("จอดสแตนบาย", AmberWarning, Color.Black)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AddVehicleDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, licensePlate: String, modelYear: String, driverName: String, officeName: String, provinceGroup: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var modelYear by remember { mutableStateOf("") }
    var driverName by remember { mutableStateOf("") }
    var officeName by remember { mutableStateOf("") }
    var provinceGroup by remember { mutableStateOf("ขอนแก่น") }
    var provinceDropdownExpanded by remember { mutableStateOf(false) }

    val provinceList = listOf(
        "บึงกาฬ",
        "หนองบัวลำภู",
        "ขอนแก่น",
        "อุดรธานี",
        "เลย",
        "หนองคาย",
        "มหาสารคาม",
        "กาฬสินธุ์"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = "🚗 เพิ่มรถ / ใส่ทะเบียนใหม่",
                color = Color(0xFF1D1B20),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = licensePlate,
                    onValueChange = { licensePlate = it },
                    label = { Text("ป้ายทะเบียนรถ (เช่น 1กข-9999 กทม.)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ชื่อเรียกประจำรถ (เช่น รถสิบล้อ #01)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = driverName,
                    onValueChange = { driverName = it },
                    label = { Text("👤 ชื่อผู้ใช้รถ / พนักงานขับรถ") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = officeName,
                    onValueChange = { officeName = it },
                    label = { Text("🏢 ชื่อ ปจ./ปณ. (ชื่อที่ทำการ)") },
                    placeholder = { Text("เช่น ปณ.เมืองขอนแก่น, ปจ.หนองคาย") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Province Group Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = provinceGroup,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("📍 ชื่อ กลุ่ม ปจ. (กลุ่มจังหวัด)", color = Color(0xFF6750A4)) },
                        trailingIcon = {
                            IconButton(onClick = { provinceDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Province Group", tint = Color(0xFF1D1B20))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20),
                            disabledTextColor = Color(0xFF1D1B20),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { provinceDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = provinceDropdownExpanded,
                        onDismissRequest = { provinceDropdownExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        provinceList.forEach { province ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = province, 
                                        color = Color(0xFF1D1B20),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    ) 
                                },
                                onClick = {
                                    provinceGroup = province
                                    provinceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = modelYear,
                    onValueChange = { modelYear = it },
                    label = { Text("รุ่น/ปี (เช่น Isuzu D-Max 2024)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (licensePlate.isNotBlank()) {
                        val finalName = name.ifBlank { "รถขนส่ง $licensePlate" }
                        val finalDriver = driverName.ifBlank { "สมชาย ใจดี (คนขับ)" }
                        val finalOffice = officeName.ifBlank { "ปณ.เมืองขอนแก่น" }
                        onAdd(finalName, licensePlate, modelYear, finalDriver, finalOffice, provinceGroup)
                    }
                },
                enabled = licensePlate.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4), contentColor = Color.White),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("บันทึกข้อมูลรถ", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก", color = Color(0xFF49454F))
            }
        }
    )
}

@Composable
fun SensorInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        titleContentColor = Color.White,
        textContentColor = Color.LightGray,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = "Sensor Info", tint = CyberCyanPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("คำอธิบายการวัดความเร็วและระยะทาง", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡ ความเร็ว (Speed)", color = EmeraldSafe, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(color = EmeraldSafe.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("ใช้ได้จริง 100%", color = EmeraldSafe, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "วัดจากชิปเซนเซอร์ GPS Hardware บนสมาร์ตโฟนของคุณโดยตรง (กม./ชม.) มีความแม่นยำสูงเมื่อเคลื่อนที่ในที่โล่งแจ้ง",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }

                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📏 ระยะทางเดินทาง (Trip Distance)", color = AmberWarning, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(color = AmberWarning.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("ใช้ได้จริง 100%", color = AmberWarning, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "คำนวณสะสมระยะทางจริงจากจุดกด 'เริ่มเดินทาง' ไปจนถึง 'ถึงเป้าหมาย' โดยประมวลผลพิกัด GPS ดาวเทียมบนสมาร์ตโฟนทีละจุดอย่างแม่นยำ",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyanPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("เข้าใจแล้ว", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun TripSummaryDialog(
    summary: com.example.ui.TripSummaryData,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.5.dp, CyberCyanPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(EmeraldSafe.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏁", fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "สรุปผลการเดินทาง",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "ยานพาหนะ: ${summary.vehicleName} (${summary.licensePlate})",
                    color = CyberCyanPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Start", tint = EmeraldSafe, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "จุดเริ่มต้น: ", color = Color.Gray, fontSize = 12.sp)
                            Text(text = summary.startPlace, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "End", tint = CrimsonAlert, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "ถึงเป้าหมาย: ", color = Color.Gray, fontSize = 12.sp)
                            Text(text = summary.endPlace, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val distStr = if (summary.distanceKm >= 1.0) String.format("%.2f กม.", summary.distanceKm) else String.format("%.0f เมตร", summary.distanceKm * 1000)
                    SummaryStatCard(
                        title = "ระยะทางรวม",
                        value = distStr,
                        icon = "🛣️",
                        accentColor = CyberCyanPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    val durationStr = "${summary.durationMinutes} นาที ${summary.durationSeconds} วินาที"
                    SummaryStatCard(
                        title = "ระยะเวลา",
                        value = durationStr,
                        icon = "⏱️",
                        accentColor = AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SummaryStatCard(
                        title = "ความเร็วสูงสุด",
                        value = "${summary.topSpeedKmh} กม./ชม.",
                        icon = "🏎️",
                        accentColor = Color(0xFFA855F7),
                        modifier = Modifier.weight(1f)
                    )

                    SummaryStatCard(
                        title = "ขับเกินความเร็ว",
                        value = "${summary.overspeedCount} ครั้ง",
                        icon = if (summary.overspeedCount > 0) "⚠️" else "✅",
                        accentColor = if (summary.overspeedCount > 0) CrimsonAlert else EmeraldSafe,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe, contentColor = Color.Black),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("ตกลง / ปิดหน้าต่างสรุป", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun SummaryStatCard(
    title: String,
    value: String,
    icon: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, color = Color.Gray, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

data class OnlineSearchResult(
    val title: String,
    val subtitle: String,
    val lat: Double,
    val lng: Double
)

@Composable
fun GoogleLocationSearchDialog(
    onDismiss: () -> Unit,
    onSelectLocation: (String, Double, Double) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var onlineResults by remember { mutableStateOf<List<OnlineSearchResult>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }

    val popularLocations = listOf(
        Triple("✈️ สนามบินสุวรรณภูมิ (Bangkok BKK)", 13.6900, 100.7500),
        Triple("🛫 สนามบินดอนเมือง (Bangkok DMK)", 13.9130, 100.6010),
        Triple("🏢 คลังสินค้า ICD ลาดกระบัง", 13.7292, 100.6782),
        Triple("🏛️ อนุสาวรีย์ชัยสมรภูมิ กรุงเทพฯ", 13.7628, 100.5372),
        Triple("🛍️ สยามพารากอน / ปทุมวัน", 13.7460, 100.5340),
        Triple("🛣️ ด่านทางด่วนมอเตอร์เวย์ พระราม 9", 13.7420, 100.6150),
        Triple("🚚 ถ.บางนา-ตราด กม.10", 13.6350, 100.7050),
        Triple("🌊 นิคมอุตสาหกรรม อมตะ ชลบุรี", 13.3611, 100.9847),
        Triple("🏭 นิคมอุตสาหกรรม มาบตาพุด ระยอง", 12.7214, 101.1552),
        Triple("🏞️ ประตูท่าแพ เชียงใหม่", 18.7877, 98.9932),
        Triple("🏖️ หาดป่าตอง ภูเก็ต", 7.8920, 98.2980),
        Triple("🏙️ เซ็นทรัล ขอนแก่น", 16.4322, 102.8288)
    )

    fun performSearch() {
        if (searchQuery.isBlank()) return
        isSearching = true
        hasSearched = true
        coroutineScope.launch {
            val rawQuery = searchQuery.trim()
            val queryToSearch = if (!rawQuery.contains("ไทย") && !rawQuery.lowercase().contains("thailand")) {
                "$rawQuery, Thailand"
            } else rawQuery

            val list = mutableListOf<OnlineSearchResult>()
            try {
                withContext(Dispatchers.IO) {
                    val encoded = URLEncoder.encode(queryToSearch, "UTF-8")
                    val urlString = "https://nominatim.openstreetmap.org/search?format=json&q=$encoded&addressdetails=1&limit=12&countrycodes=th"
                    val url = URL(urlString)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("User-Agent", "PostCarTrack/1.0")
                    conn.connectTimeout = 6000
                    conn.readTimeout = 6000

                    if (conn.responseCode == 200) {
                        val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                        val jsonArray = JSONArray(responseText)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val rawName = obj.optString("display_name", "")
                            val latVal = obj.optString("lat", "0").toDoubleOrNull() ?: 0.0
                            val lngVal = obj.optString("lon", "0").toDoubleOrNull() ?: 0.0

                            val parts = rawName.split(",")
                            val title = parts.firstOrNull()?.trim() ?: rawQuery
                            val subtitle = if (parts.size > 1) parts.drop(1).take(3).joinToString(", ").trim() else rawName

                            if (latVal != 0.0 && lngVal != 0.0) {
                                list.add(OnlineSearchResult(title, subtitle, latVal, lngVal))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onlineResults = list
                isSearching = false
            }
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ค้นหาสถานที่จริง (Google Maps)",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (it.isBlank()) {
                                hasSearched = false
                                onlineResults = emptyList()
                            }
                        },
                        placeholder = { Text("ค้นหาทุกสถานที่... (เช่น สยาม, เชียงใหม่)", color = Color.Gray, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFEF4444)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEF4444),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { performSearch() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("ค้นหา", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // External Google Maps App Launcher
                Button(
                    onClick = {
                        val queryToOpen = if (searchQuery.isNotBlank()) searchQuery else "ประเทศไทย"
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/search/?api=1&query=${URLEncoder.encode(queryToOpen, "UTF-8")}")
                        )
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🗺️ เปิดค้นหาบนแอป Google Maps โดยตรง", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isSearching) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        CircularProgressIndicator(color = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("กำลังค้นหาข้อมูลพิกัดสถานที่จริง...", color = Color.LightGray, fontSize = 13.sp)
                    }
                } else if (hasSearched && onlineResults.isEmpty()) {
                    Text(
                        text = "❌ ไม่พบสถานที่ดังกล่าว ลองค้นด้วยคำค้นอื่น หรือกดปุ่มเปิดแอป Google Maps ด้านบน",
                        color = Color(0xFFF87171),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else if (onlineResults.isNotEmpty()) {
                    Text(
                        text = "📍 ผลการค้นหาสถานที่จริง (${onlineResults.size} รายการ):",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        items(onlineResults) { item ->
                            Surface(
                                onClick = { onSelectLocation(item.title, item.lat, item.lng) },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF334155),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = item.title,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = item.subtitle,
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "พิกัด: ${String.format("%.4f", item.lat)}, ${String.format("%.4f", item.lng)}",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Popular preset locations when search query is empty
                    Text(
                        text = "สถานที่ยอดนิยมสำหรับขนส่ง / เดินทาง:",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        popularLocations.forEach { (name, lat, lng) ->
                            Surface(
                                onClick = { onSelectLocation(name, lat, lng) },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF334155),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = name,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "พิกัด GPS: ${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}",
                                            color = Color.LightGray,
                                            fontSize = 10.sp
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
