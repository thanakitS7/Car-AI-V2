package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.RouteGeofenceEntity
import com.example.data.SampleData
import com.example.data.TrackingRepository
import com.example.data.VehicleEntity
import com.example.util.GeoUtils
import com.example.util.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = TrackingRepository(db.appDao())

    val allVehicles = repository.allVehicles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allRoutes = repository.allRoutes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allAlerts = repository.allAlerts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedVehicleId = MutableStateFlow("V001")
    val selectedVehicleId: StateFlow<String> = _selectedVehicleId.asStateFlow()

    private val _isTripActive = MutableStateFlow(false)
    val isTripActive: StateFlow<Boolean> = _isTripActive.asStateFlow()

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _isDeviatedTestMode = MutableStateFlow(false)
    val isDeviatedTestMode: StateFlow<Boolean> = _isDeviatedTestMode.asStateFlow()

    private val _simulationSpeedMultiplier = MutableStateFlow(1)
    val simulationSpeedMultiplier: StateFlow<Int> = _simulationSpeedMultiplier.asStateFlow()

    // Google Sheets Cloud Sync State
    private val _googleSheetsUrl = MutableStateFlow(com.example.util.GoogleSheetsSyncManager.DEFAULT_WEBHOOK_URL)
    val googleSheetsUrl: StateFlow<String> = _googleSheetsUrl.asStateFlow()

    private val _isGoogleSheetsSyncEnabled = MutableStateFlow(true)
    val isGoogleSheetsSyncEnabled: StateFlow<Boolean> = _isGoogleSheetsSyncEnabled.asStateFlow()

    private val _lastSyncStatus = MutableStateFlow("ยังไม่ได้เชื่อมต่อ")
    val lastSyncStatus: StateFlow<String> = _lastSyncStatus.asStateFlow()

    private val _isSyncingInProcess = MutableStateFlow(false)
    val isSyncingInProcess: StateFlow<Boolean> = _isSyncingInProcess.asStateFlow()

    // Playback state
    private val _playbackIndex = MutableStateFlow(0)
    val playbackIndex: StateFlow<Int> = _playbackIndex.asStateFlow()

    private val _isPlaybackPlaying = MutableStateFlow(false)
    val isPlaybackPlaying: StateFlow<Boolean> = _isPlaybackPlaying.asStateFlow()

    private var simulationJob: Job? = null
    private var playbackJob: Job? = null

    // Combined active vehicle state
    val activeVehicle = combine(allVehicles, _selectedVehicleId) { vehicles, id ->
        vehicles.firstOrNull { it.id == id } ?: vehicles.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Active route for selected vehicle
    val activeRoute = combine(allRoutes, activeVehicle) { routes, vehicle ->
        if (vehicle == null) null
        else routes.firstOrNull { it.id == vehicle.activeRouteId } ?: routes.firstOrNull { it.vehicleId == vehicle.id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Parsed waypoints for active route
    val activeWaypoints = combine(activeRoute) { routes ->
        val route = routes.firstOrNull()
        if (route != null) {
            repository.parseWaypoints(route.waypointsJson)
        } else {
            SampleData.MOTORWAY_WAYPOINTS
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SampleData.MOTORWAY_WAYPOINTS
    )

    val locationHistory = combine(selectedVehicleId) { id ->
        id
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "V001"
    )

    val activeHistoryPoints = repository.getLocationHistory("V001").stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.initializeSampleDataIfNeeded()
        }
    }

    private val _isRealGpsActive = MutableStateFlow(false)
    val isRealGpsActive: StateFlow<Boolean> = _isRealGpsActive.asStateFlow()

    private val _isGpsPermissionGranted = MutableStateFlow(false)
    val isGpsPermissionGranted: StateFlow<Boolean> = _isGpsPermissionGranted.asStateFlow()

    private val _currentGpsAccuracy = MutableStateFlow(0f)
    val currentGpsAccuracy: StateFlow<Float> = _currentGpsAccuracy.asStateFlow()

    private val _tripDistanceMeters = MutableStateFlow(0.0)
    val tripDistanceMeters: StateFlow<Double> = _tripDistanceMeters.asStateFlow()

    private val _speedLimitKmh = MutableStateFlow(90)
    val speedLimitKmh: StateFlow<Int> = _speedLimitKmh.asStateFlow()

    private val _isOverspeeding = MutableStateFlow(false)
    val isOverspeeding: StateFlow<Boolean> = _isOverspeeding.asStateFlow()

    private var lastOverspeedAlertTimeMs = 0L

    private var lastGpsLat: Double = 0.0
    private var lastGpsLng: Double = 0.0

    private var lastSyncTimeMs = 0L

    fun setSpeedLimitKmh(limit: Int) {
        _speedLimitKmh.value = limit.coerceIn(30, 200)
    }

    private fun checkAndHandleOverspeed(vehicle: VehicleEntity, speedKmh: Int, lat: Double, lng: Double) {
        val limit = _speedLimitKmh.value
        val isExceeded = speedKmh > limit
        _isOverspeeding.value = isExceeded

        if (isExceeded) {
            val now = System.currentTimeMillis()
            if (now - lastOverspeedAlertTimeMs > 10000L) {
                lastOverspeedAlertTimeMs = now
                triggerVibrationAlert()
                viewModelScope.launch {
                    val placeName = GeoUtils.getFallbackThaiLandmark(lat, lng)
                    val alert = com.example.data.AlertEntity(
                        vehicleId = vehicle.id,
                        vehicleName = vehicle.name,
                        licensePlate = vehicle.licensePlate,
                        alertType = "SPEEDING",
                        severity = "CRITICAL",
                        title = "⚠️ แจ้งเตือน! รถขับเกินความเร็วที่กำหนด",
                        description = "ความเร็วปัจจุบัน ${speedKmh} กม./ชม. (ขีดจำกัด ${limit} กม./ชม.) ณ ${placeName}",
                        latitude = lat,
                        longitude = lng,
                        distanceFromRouteMeters = 0,
                        timestamp = now,
                        isAcknowledged = false
                    )
                    repository.addAlert(alert)

                    if (_isGoogleSheetsSyncEnabled.value) {
                        com.example.util.GoogleSheetsSyncManager.sendTelemetryToGoogleSheets(
                            webhookUrl = _googleSheetsUrl.value,
                            vehicleId = vehicle.id,
                            vehicleName = vehicle.name,
                            licensePlate = vehicle.licensePlate,
                            status = "⚠️ OVERSPEED (ขับเร็ว ${speedKmh} กม./ชม. เกินจำกัด ${limit} กม./ชม.)",
                            latitude = lat,
                            longitude = lng,
                            speedKmh = speedKmh,
                            fuelPercent = vehicle.fuelPercent,
                            batteryVoltage = vehicle.batteryVoltage
                        )
                    }
                }
            }
        }
    }

    fun setGpsPermissionGranted(granted: Boolean) {
        _isGpsPermissionGranted.value = granted
    }

    fun updateRealGpsLocation(
        lat: Double,
        lng: Double,
        speedKmh: Int,
        heading: Float,
        accuracy: Float = 5f
    ) {
        _isRealGpsActive.value = true
        _currentGpsAccuracy.value = accuracy

        val vehicle = activeVehicle.value ?: return

        checkAndHandleOverspeed(vehicle, speedKmh, lat, lng)

        // Accumulate trip distance if trip is active
        if (_isTripActive.value) {
            if (lastGpsLat != 0.0 && lastGpsLng != 0.0) {
                val dist = GeoUtils.distanceMeters(lastGpsLat, lastGpsLng, lat, lng)
                // Filter minor GPS noise (<1m) and unrealistic jumps (>1000m)
                if (dist > 1.0 && dist < 1000.0) {
                    _tripDistanceMeters.value += dist
                }
            }
            lastGpsLat = lat
            lastGpsLng = lng
        }

        viewModelScope.launch {
            repository.updateVehiclePosition(
                vehicleId = vehicle.id,
                newLat = lat,
                newLng = lng,
                speedKmh = speedKmh,
                heading = heading
            )

            // Auto-sync real GPS coordinates to Google Sheets if trip is active
            val now = System.currentTimeMillis()
            if (_isTripActive.value && _isGoogleSheetsSyncEnabled.value && (now - lastSyncTimeMs > 5000L)) {
                lastSyncTimeMs = now
                val result = com.example.util.GoogleSheetsSyncManager.sendTelemetryToGoogleSheets(
                    webhookUrl = _googleSheetsUrl.value,
                    vehicleId = vehicle.id,
                    vehicleName = vehicle.name,
                    licensePlate = vehicle.licensePlate,
                    status = if (speedKmh > 3) "MOVING (GPS สด)" else "IDLE (จอดพัก)",
                    latitude = lat,
                    longitude = lng,
                    speedKmh = speedKmh,
                    fuelPercent = vehicle.fuelPercent,
                    batteryVoltage = vehicle.batteryVoltage
                )
                _lastSyncStatus.value = result.getOrElse { "ส่งพิกัด GPS สดสำเร็จ (${speedKmh} กม./ชม.)" }
            }
        }
    }

    fun updateFuelLevel(newFuelPercent: Int) {
        val vehicle = activeVehicle.value ?: return
        viewModelScope.launch {
            repository.updateVehiclePosition(
                vehicleId = vehicle.id,
                newLat = vehicle.currentLat,
                newLng = vehicle.currentLng,
                speedKmh = vehicle.speedKmh,
                heading = vehicle.headingBearing
            )
        }
    }

    fun startTrip() {
        _isTripActive.value = true
        _tripDistanceMeters.value = 0.0
        val vehicle = activeVehicle.value
        if (vehicle != null) {
            lastGpsLat = vehicle.currentLat
            lastGpsLng = vehicle.currentLng
            viewModelScope.launch {
                repository.updateVehiclePosition(
                    vehicleId = vehicle.id,
                    newLat = vehicle.currentLat,
                    newLng = vehicle.currentLng,
                    speedKmh = vehicle.speedKmh,
                    heading = vehicle.headingBearing
                )
                _lastSyncStatus.value = "เริ่มออกเดินทางแล้ว (เปิดรับส่งพิกัดสด GPS มือถือ)"
                com.example.util.GoogleSheetsSyncManager.sendTelemetryToGoogleSheets(
                    webhookUrl = _googleSheetsUrl.value,
                    vehicleId = vehicle.id,
                    vehicleName = vehicle.name,
                    licensePlate = vehicle.licensePlate,
                    status = "MOVING (เริ่มเดินทาง GPS สด)",
                    latitude = vehicle.currentLat,
                    longitude = vehicle.currentLng,
                    speedKmh = vehicle.speedKmh,
                    fuelPercent = vehicle.fuelPercent,
                    batteryVoltage = vehicle.batteryVoltage
                )
            }
        }
    }

    fun endTrip() {
        _isTripActive.value = false
        _isSimulating.value = false
        simulationJob?.cancel()

        val vehicle = activeVehicle.value
        if (vehicle != null) {
            viewModelScope.launch {
                repository.updateVehiclePosition(
                    vehicleId = vehicle.id,
                    newLat = vehicle.currentLat,
                    newLng = vehicle.currentLng,
                    speedKmh = 0,
                    heading = vehicle.headingBearing
                )
                _lastSyncStatus.value = "ถึงที่หมายแล้ว (หยุดการส่งข้อมูล)"
                com.example.util.GoogleSheetsSyncManager.sendTelemetryToGoogleSheets(
                    webhookUrl = _googleSheetsUrl.value,
                    vehicleId = vehicle.id,
                    vehicleName = vehicle.name,
                    licensePlate = vehicle.licensePlate,
                    status = "PARKED (ถึงเป้าหมายเรียบร้อย)",
                    latitude = vehicle.currentLat,
                    longitude = vehicle.currentLng,
                    speedKmh = 0,
                    fuelPercent = vehicle.fuelPercent,
                    batteryVoltage = vehicle.batteryVoltage
                )
            }
        }
    }

    fun selectVehicle(vehicleId: String) {
        _selectedVehicleId.value = vehicleId
        _isDeviatedTestMode.value = false
    }

    fun toggleSimulation(enable: Boolean) {
        _isSimulating.value = enable
        if (enable) {
            startSimulationLoop()
        } else {
            simulationJob?.cancel()
        }
    }

    fun setSimulationSpeed(multiplier: Int) {
        _simulationSpeedMultiplier.value = multiplier
    }

    fun triggerTestDeviation(forceDeviate: Boolean) {
        _isDeviatedTestMode.value = forceDeviate
        if (forceDeviate) {
            triggerVibrationAlert()
        }
    }

    fun toggleEngineLock(vehicleId: String, lock: Boolean) {
        viewModelScope.launch {
            repository.toggleEngineLock(vehicleId, lock)
            if (lock) triggerVibrationAlert()
        }
    }

    fun acknowledgeAlert(alertId: Long) {
        viewModelScope.launch {
            repository.acknowledgeAlert(alertId)
        }
    }

    fun addNewVehicle(name: String, licensePlate: String, modelYear: String) {
        viewModelScope.launch {
            val newId = "V${System.currentTimeMillis() % 10000}"
            val vehicle = VehicleEntity(
                id = newId,
                name = name,
                licensePlate = licensePlate,
                modelYear = modelYear.ifEmpty { "2024" },
                status = "MOVING",
                currentLat = 13.7381,
                currentLng = 100.6283,
                speedKmh = 60,
                headingBearing = 90f,
                fuelPercent = 95,
                batteryVoltage = 12.8,
                activeRouteId = "R001",
                isEngineLocked = false
            )
            repository.addVehicle(vehicle)
            selectVehicle(newId)
        }
    }

    fun addNewRoute(
        routeName: String,
        type: String,
        startName: String,
        endName: String,
        toleranceMeters: Int,
        maxSpeed: Int
    ) {
        viewModelScope.launch {
            val vId = selectedVehicleId.value
            val newRoute = RouteGeofenceEntity(
                id = "R_${System.currentTimeMillis()}",
                vehicleId = vId,
                name = routeName,
                type = type,
                centerLat = 13.7381,
                centerLng = 100.6283,
                radiusMeters = if (type == "CIRCLE_ZONE") 1000.0 else 300.0,
                maxAllowedSpeed = maxSpeed,
                toleranceMeters = toleranceMeters,
                waypointsJson = "13.7381,100.6283;13.7292,100.6782;13.7125,100.7421;13.6821,100.8251",
                isActive = true,
                startLocationName = startName,
                endLocationName = endName
            )
            repository.addRoute(newRoute)
        }
    }

    fun updateGoogleSheetsUrl(url: String) {
        _googleSheetsUrl.value = url
    }

    fun toggleGoogleSheetsSync(enabled: Boolean) {
        _isGoogleSheetsSyncEnabled.value = enabled
    }

    fun syncCurrentVehicleToGoogleSheetsNow() {
        val vehicle = activeVehicle.value ?: return
        viewModelScope.launch {
            _isSyncingInProcess.value = true
            _lastSyncStatus.value = "กำลังส่งข้อมูลเข้า Google Sheets..."
            val result = com.example.util.GoogleSheetsSyncManager.sendTelemetryToGoogleSheets(
                webhookUrl = _googleSheetsUrl.value,
                vehicleId = vehicle.id,
                vehicleName = vehicle.name,
                licensePlate = vehicle.licensePlate,
                status = vehicle.status,
                latitude = vehicle.currentLat,
                longitude = vehicle.currentLng,
                speedKmh = vehicle.speedKmh,
                fuelPercent = vehicle.fuelPercent,
                batteryVoltage = vehicle.batteryVoltage
            )
            _isSyncingInProcess.value = false
            _lastSyncStatus.value = result.getOrElse { "ผิดพลาด: ${it.message}" }
        }
    }

    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            var stepIndex = 0
            val waypoints = SampleData.MOTORWAY_WAYPOINTS

            while (_isSimulating.value && _isTripActive.value) {
                val delayMs = (2000L / _simulationSpeedMultiplier.value).coerceAtLeast(400L)
                delay(delayMs)

                val vehicle = activeVehicle.value ?: continue
                if (vehicle.isEngineLocked) continue

                var targetPoint: LatLng
                var targetSpeed = 80 + (stepIndex % 15)

                if (_isDeviatedTestMode.value) {
                    // Test speed burst > speed limit
                    targetPoint = LatLng(
                        lat = 13.7480 + ((stepIndex % 10) * 0.0020),
                        lng = 100.7700 + ((stepIndex % 10) * 0.0025)
                    )
                    targetSpeed = 105 + ((stepIndex % 4) * 5) // 105 - 120 km/h overspeed test
                } else {
                    // Follow normal motorway sequence
                    val wayIdx = (stepIndex % waypoints.size)
                    val baseWay = waypoints[wayIdx]
                    val nextWay = waypoints[(wayIdx + 1) % waypoints.size]

                    // Smooth interpolate between points
                    val progress = (stepIndex % 5) / 5.0
                    targetPoint = LatLng(
                        lat = baseWay.lat + (nextWay.lat - baseWay.lat) * progress,
                        lng = baseWay.lng + (nextWay.lng - baseWay.lng) * progress
                    )
                }

                checkAndHandleOverspeed(vehicle, targetSpeed, targetPoint.lat, targetPoint.lng)

                val bearing = GeoUtils.calculateBearing(
                    vehicle.currentLat, vehicle.currentLng,
                    targetPoint.lat, targetPoint.lng
                )

                val dist = GeoUtils.distanceMeters(vehicle.currentLat, vehicle.currentLng, targetPoint.lat, targetPoint.lng)
                if (dist > 1.0) {
                    _tripDistanceMeters.value += dist
                }

                repository.updateVehiclePosition(
                    vehicleId = vehicle.id,
                    newLat = targetPoint.lat,
                    newLng = targetPoint.lng,
                    speedKmh = targetSpeed,
                    heading = bearing
                )

                // Sync to Google Sheets every 3 steps if sync enabled
                if (_isGoogleSheetsSyncEnabled.value && stepIndex % 3 == 0) {
                    val currentVeh = activeVehicle.value ?: vehicle
                    launch {
                        val result = com.example.util.GoogleSheetsSyncManager.sendTelemetryToGoogleSheets(
                            webhookUrl = _googleSheetsUrl.value,
                            vehicleId = currentVeh.id,
                            vehicleName = currentVeh.name,
                            licensePlate = currentVeh.licensePlate,
                            status = currentVeh.status,
                            latitude = targetPoint.lat,
                            longitude = targetPoint.lng,
                            speedKmh = targetSpeed,
                            fuelPercent = currentVeh.fuelPercent,
                            batteryVoltage = currentVeh.batteryVoltage
                        )
                        _lastSyncStatus.value = result.getOrElse { "ผิดพลาด: ${it.message}" }
                    }
                }

                stepIndex++
            }
        }
    }

    private fun triggerVibrationAlert() {
        try {
            val context = getApplication<Application>().applicationContext
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            }
        } catch (e: Exception) {
            // Ignore if vibration permissions unavailable
        }
    }
}
