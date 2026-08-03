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

    private val _isTripPaused = MutableStateFlow(false)
    val isTripPaused: StateFlow<Boolean> = _isTripPaused.asStateFlow()

    private val _tripStartTimeMs = MutableStateFlow(0L)
    private val _tripTopSpeedKmh = MutableStateFlow(0)
    private val _tripOverspeedCount = MutableStateFlow(0)

    private val _tripSummary = MutableStateFlow<TripSummaryData?>(null)
    val tripSummary: StateFlow<TripSummaryData?> = _tripSummary.asStateFlow()

    private val _speedLimitKmh = MutableStateFlow(90)
    val speedLimitKmh: StateFlow<Int> = _speedLimitKmh.asStateFlow()

    private val _isOverspeeding = MutableStateFlow(false)
    val isOverspeeding: StateFlow<Boolean> = _isOverspeeding.asStateFlow()

    private var lastOverspeedAlertTimeMs = 0L

    private var lastGpsLat: Double = 0.0
    private var lastGpsLng: Double = 0.0

    private var lastSyncTimeMs = 0L

    fun setSpeedLimitKmh(limit: Int) {
        _speedLimitKmh.value = 90
    }

    private fun checkAndHandleOverspeed(vehicle: VehicleEntity, speedKmh: Int, lat: Double, lng: Double) {
        if (speedKmh > _tripTopSpeedKmh.value) {
            _tripTopSpeedKmh.value = speedKmh
        }

        val limit = _speedLimitKmh.value
        val isExceeded = speedKmh > limit
        _isOverspeeding.value = isExceeded

        if (isExceeded) {
            _tripOverspeedCount.value += 1
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
                            driverName = vehicle.driverName,
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

        // If trip is paused, do not update movement telemetry
        if (_isTripPaused.value) {
            return
        }

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
                    driverName = vehicle.driverName,
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
        _isTripPaused.value = false
        _tripStartTimeMs.value = System.currentTimeMillis()
        _tripDistanceMeters.value = 0.0
        _tripTopSpeedKmh.value = 0
        _tripOverspeedCount.value = 0
        _tripSummary.value = null

        try {
            com.example.service.TrackingForegroundService.startService(getApplication())
        } catch (e: Exception) {
            e.printStackTrace()
        }

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
                    driverName = vehicle.driverName,
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

    fun pauseTrip() {
        val newPausedState = !_isTripPaused.value
        _isTripPaused.value = newPausedState

        val vehicle = activeVehicle.value
        if (vehicle != null) {
            viewModelScope.launch {
                val statusText = if (newPausedState) "⏸️ พักรถ (พักการทำงาน GPS)" else "▶️ เดินทางต่อ"
                repository.updateVehiclePosition(
                    vehicleId = vehicle.id,
                    newLat = vehicle.currentLat,
                    newLng = vehicle.currentLng,
                    speedKmh = if (newPausedState) 0 else vehicle.speedKmh,
                    heading = vehicle.headingBearing
                )
                _lastSyncStatus.value = statusText
                if (_isGoogleSheetsSyncEnabled.value) {
                    com.example.util.GoogleSheetsSyncManager.sendTelemetryToGoogleSheets(
                        webhookUrl = _googleSheetsUrl.value,
                        vehicleId = vehicle.id,
                        vehicleName = vehicle.name,
                        licensePlate = vehicle.licensePlate,
                        driverName = vehicle.driverName,
                        status = statusText,
                        latitude = vehicle.currentLat,
                        longitude = vehicle.currentLng,
                        speedKmh = if (newPausedState) 0 else vehicle.speedKmh,
                        fuelPercent = vehicle.fuelPercent,
                        batteryVoltage = vehicle.batteryVoltage
                    )
                }
            }
        }
    }

    fun endTrip() {
        val vehicle = activeVehicle.value
        val startTime = if (_tripStartTimeMs.value > 0L) _tripStartTimeMs.value else System.currentTimeMillis() - 600000L
        val durationMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1000L)
        val totalSecs = durationMs / 1000L
        val mins = totalSecs / 60L
        val secs = totalSecs % 60L

        if (vehicle != null) {
            val context = getApplication<Application>().applicationContext
            val startLoc = GeoUtils.getFallbackThaiLandmark(lastGpsLat.takeIf { it != 0.0 } ?: vehicle.currentLat, lastGpsLng.takeIf { it != 0.0 } ?: vehicle.currentLng)
            val endLoc = GeoUtils.getFallbackThaiLandmark(vehicle.currentLat, vehicle.currentLng)

            _tripSummary.value = TripSummaryData(
                vehicleName = vehicle.name,
                licensePlate = vehicle.licensePlate,
                distanceKm = _tripDistanceMeters.value / 1000.0,
                durationMinutes = mins,
                durationSeconds = secs,
                topSpeedKmh = _tripTopSpeedKmh.value.coerceAtLeast(vehicle.speedKmh),
                startPlace = startLoc,
                endPlace = endLoc,
                overspeedCount = _tripOverspeedCount.value
            )
        }

        _isTripActive.value = false
        _isTripPaused.value = false
        _isSimulating.value = false
        simulationJob?.cancel()

        try {
            com.example.service.TrackingForegroundService.stopService(getApplication())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (vehicle != null) {
            viewModelScope.launch {
                repository.updateVehiclePosition(
                    vehicleId = vehicle.id,
                    newLat = vehicle.currentLat,
                    newLng = vehicle.currentLng,
                    speedKmh = 0,
                    heading = vehicle.headingBearing
                )
                _lastSyncStatus.value = "ถึงที่หมายแล้ว (สรุปการเดินทางเรียบร้อย)"
                com.example.util.GoogleSheetsSyncManager.sendTelemetryToGoogleSheets(
                    webhookUrl = _googleSheetsUrl.value,
                    vehicleId = vehicle.id,
                    vehicleName = vehicle.name,
                    licensePlate = vehicle.licensePlate,
                    driverName = vehicle.driverName,
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

    fun dismissTripSummary() {
        _tripSummary.value = null
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

    fun addNewVehicle(name: String, licensePlate: String, modelYear: String, driverName: String = "") {
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
                isEngineLocked = false,
                driverName = driverName.ifBlank { "สมชาย ใจดี (คนขับ)" }
            )
            repository.addVehicle(vehicle)
            selectVehicle(newId)
        }
    }

    fun updateVehicleDriverName(vehicleId: String, newDriverName: String) {
        viewModelScope.launch {
            val vehicles = allVehicles.value
            val v = vehicles.firstOrNull { it.id == vehicleId } ?: return@launch
            val updated = v.copy(driverName = newDriverName)
            repository.addVehicle(updated)
        }
    }

    fun updateVehicleDetails(vehicleId: String, name: String, licensePlate: String, modelYear: String, driverName: String) {
        viewModelScope.launch {
            val vehicles = allVehicles.value
            val v = vehicles.firstOrNull { it.id == vehicleId } ?: return@launch
            val updated = v.copy(
                name = name.ifBlank { v.name },
                licensePlate = licensePlate.ifBlank { v.licensePlate },
                modelYear = modelYear.ifBlank { v.modelYear },
                driverName = driverName.ifBlank { v.driverName }
            )
            repository.updateVehicle(updated)
            if (_isGoogleSheetsSyncEnabled.value) {
                com.example.util.GoogleSheetsSyncManager.sendTelemetryToGoogleSheets(
                    webhookUrl = _googleSheetsUrl.value,
                    vehicleId = updated.id,
                    vehicleName = updated.name,
                    licensePlate = updated.licensePlate,
                    driverName = updated.driverName,
                    status = "UPDATED (แก้ไขข้อมูลคนขับ/รถ)",
                    latitude = updated.currentLat,
                    longitude = updated.currentLng,
                    speedKmh = updated.speedKmh,
                    fuelPercent = updated.fuelPercent,
                    batteryVoltage = updated.batteryVoltage
                )
            }
        }
    }

    fun deleteVehicle(vehicleId: String) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicleId)
            val remaining = allVehicles.value.filter { it.id != vehicleId }
            if (remaining.isNotEmpty()) {
                selectVehicle(remaining.first().id)
            }
        }
    }

    fun syncVehiclesFromCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val result = com.example.util.GoogleSheetsSyncManager.fetchVehiclesFromCloud(_googleSheetsUrl.value)
            if (result.isSuccess) {
                val list = result.getOrNull() ?: emptyList()
                if (list.isNotEmpty()) {
                    for (jsonObj in list) {
                        val vId = jsonObj.optString("vehicleId").ifBlank {
                            jsonObj.optString("รหัสรถ").ifBlank { jsonObj.optString("id") }
                        }
                        if (vId.isNotBlank()) {
                            val name = jsonObj.optString("vehicleName").ifBlank {
                                jsonObj.optString("ชื่อรถ").ifBlank { jsonObj.optString("name", "Vehicle") }
                            }
                            val plate = jsonObj.optString("licensePlate").ifBlank {
                                jsonObj.optString("ทะเบียนรถ").ifBlank { jsonObj.optString("plate", "-") }
                            }
                            val driver = jsonObj.optString("driverName").ifBlank {
                                jsonObj.optString("ชื่อผู้ใช้/พนักงานขับรถ").ifBlank {
                                    jsonObj.optString("พนักงานขับรถ").ifBlank {
                                        jsonObj.optString("driver", "Driver")
                                    }
                                }
                            }
                            val status = jsonObj.optString("status").ifBlank {
                                jsonObj.optString("สถานะ", "STOPPED")
                            }
                            val lat = jsonObj.optDouble("latitude", jsonObj.optDouble("ละติจูด", jsonObj.optDouble("lat", 13.7563)))
                            val lng = jsonObj.optDouble("longitude", jsonObj.optDouble("ลองจิจูด", jsonObj.optDouble("lng", 100.5018)))
                            val speed = jsonObj.optInt("speedKmh", jsonObj.optInt("ความเร็ว", jsonObj.optInt("speed", 0)))

                            val existing = allVehicles.value.firstOrNull { it.id == vId }
                            if (existing != null) {
                                repository.updateVehicle(
                                    existing.copy(
                                        name = name,
                                        licensePlate = plate,
                                        driverName = driver,
                                        status = status,
                                        currentLat = if (lat != 0.0) lat else existing.currentLat,
                                        currentLng = if (lng != 0.0) lng else existing.currentLng,
                                        speedKmh = speed
                                    )
                                )
                            } else {
                                repository.addVehicle(
                                    com.example.data.VehicleEntity(
                                        id = vId,
                                        name = name,
                                        licensePlate = plate,
                                        modelYear = "2024",
                                        status = status,
                                        currentLat = if (lat != 0.0) lat else 13.7563,
                                        currentLng = if (lng != 0.0) lng else 100.5018,
                                        speedKmh = speed,
                                        headingBearing = 0f,
                                        fuelPercent = 100,
                                        batteryVoltage = 12.6,
                                        activeRouteId = null,
                                        driverName = driver
                                    )
                                )
                            }
                        }
                    }
                    onComplete(true, "ซิงค์รายชื่อรถและคนขับ ${list.size} คันจาก Google Sheets สำเร็จ!")
                } else {
                    onComplete(true, "ไม่มีข้อมูลใหม่จาก Google Sheets")
                }
            } else {
                onComplete(false, result.exceptionOrNull()?.message ?: "ไม่สามารถดึงข้อมูลได้")
            }
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
                driverName = vehicle.driverName,
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
                            driverName = currentVeh.driverName,
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

data class TripSummaryData(
    val vehicleName: String,
    val licensePlate: String,
    val distanceKm: Double,
    val durationMinutes: Long,
    val durationSeconds: Long,
    val topSpeedKmh: Int,
    val startPlace: String,
    val endPlace: String,
    val overspeedCount: Int
)
