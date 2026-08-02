package com.example.data

import com.example.util.GeoUtils
import com.example.util.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrackingRepository(private val dao: AppDao) {

    val allVehicles: Flow<List<VehicleEntity>> = dao.getAllVehicles()
    val allRoutes: Flow<List<RouteGeofenceEntity>> = dao.getAllRoutes()
    val allAlerts: Flow<List<AlertEntity>> = dao.getAllAlerts()

    suspend fun initializeSampleDataIfNeeded() = withContext(Dispatchers.IO) {
        val existing = dao.getAllVehicles().firstOrNull()
        if (existing.isNullOrEmpty()) {
            dao.insertVehicles(SampleData.INITIAL_VEHICLES)
            dao.insertRoutes(SampleData.INITIAL_ROUTES)
            dao.insertAlerts(SampleData.INITIAL_ALERTS)

            // Seed initial history points for V001
            val historyPoints = SampleData.MOTORWAY_WAYPOINTS.take(4).mapIndexed { idx, point ->
                LocationHistoryEntity(
                    vehicleId = "V001",
                    latitude = point.lat,
                    longitude = point.lng,
                    speedKmh = 75 + (idx * 3),
                    heading = 105f,
                    timestamp = System.currentTimeMillis() - ((4 - idx) * 1000 * 60 * 10),
                    isDeviationPoint = false
                )
            }
            dao.insertLocationPoints(historyPoints)
        }
    }

    fun getVehicleFlow(vehicleId: String): Flow<VehicleEntity?> {
        return dao.getVehicleById(vehicleId)
    }

    fun getRoutesForVehicle(vehicleId: String): Flow<List<RouteGeofenceEntity>> {
        return dao.getRoutesForVehicle(vehicleId)
    }

    fun getLocationHistory(vehicleId: String): Flow<List<LocationHistoryEntity>> {
        return dao.getLocationHistory(vehicleId)
    }

    suspend fun updateVehiclePosition(
        vehicleId: String,
        newLat: Double,
        newLng: Double,
        speedKmh: Int,
        heading: Float
    ) = withContext(Dispatchers.IO) {
        val currentVehicle = dao.getVehicleById(vehicleId).firstOrNull() ?: return@withContext
        val routes = dao.getRoutesForVehicle(vehicleId).firstOrNull() ?: emptyList()
        val activeRoute = routes.firstOrNull { it.id == currentVehicle.activeRouteId } ?: routes.firstOrNull()

        var isDeviated = false
        var distanceFromRoute = 0.0

        if (activeRoute != null) {
            if (activeRoute.type == "CIRCLE_ZONE") {
                distanceFromRoute = GeoUtils.distanceMeters(
                    newLat, newLng,
                    activeRoute.centerLat, activeRoute.centerLng
                )
                if (distanceFromRoute > activeRoute.radiusMeters + activeRoute.toleranceMeters) {
                    isDeviated = true
                }
            } else {
                // ROUTE_CORRIDOR
                val waypoints = parseWaypoints(activeRoute.waypointsJson)
                distanceFromRoute = GeoUtils.minDistanceFromRoute(newLat, newLng, waypoints)
                if (distanceFromRoute > activeRoute.toleranceMeters) {
                    isDeviated = true
                }
            }
        }

        val updatedStatus = when {
            currentVehicle.isEngineLocked -> "STOPPED"
            isDeviated -> "ALERT_OUT_OF_ROUTE"
            speedKmh > 0 -> "MOVING"
            else -> "IDLE"
        }

        val updatedVehicle = currentVehicle.copy(
            currentLat = newLat,
            currentLng = newLng,
            speedKmh = speedKmh,
            headingBearing = heading,
            status = updatedStatus,
            lastUpdateMillis = System.currentTimeMillis()
        )

        dao.updateVehicle(updatedVehicle)

        // Save location breadcrumb
        dao.insertLocationPoint(
            LocationHistoryEntity(
                vehicleId = vehicleId,
                latitude = newLat,
                longitude = newLng,
                speedKmh = speedKmh,
                heading = heading,
                timestamp = System.currentTimeMillis(),
                isDeviationPoint = isDeviated
            )
        )

        // If newly deviated, generate real-time security alert!
        if (isDeviated && currentVehicle.status != "ALERT_OUT_OF_ROUTE") {
            val alert = AlertEntity(
                vehicleId = vehicleId,
                vehicleName = currentVehicle.name,
                licensePlate = currentVehicle.licensePlate,
                alertType = "ROUTE_DEPARTURE",
                severity = "CRITICAL",
                title = "🚨 แจ้งเตือน! รถเคลื่อนที่ออกนอกเส้นทาง",
                description = "ตรวจพบตำแหน่งอยู่นอกเส้นทาง ${activeRoute?.name ?: "ที่กำหนด"} ระยะห่าง ${GeoUtils.formatDistance(distanceFromRoute)}",
                latitude = newLat,
                longitude = newLng,
                distanceFromRouteMeters = distanceFromRoute.toInt(),
                timestamp = System.currentTimeMillis(),
                isAcknowledged = false
            )
            dao.insertAlert(alert)
        }
    }

    suspend fun toggleEngineLock(vehicleId: String, lock: Boolean) = withContext(Dispatchers.IO) {
        val vehicle = dao.getVehicleById(vehicleId).firstOrNull() ?: return@withContext
        val updated = vehicle.copy(
            isEngineLocked = lock,
            speedKmh = if (lock) 0 else vehicle.speedKmh,
            status = if (lock) "STOPPED" else "IDLE"
        )
        dao.updateVehicle(updated)

        val alert = AlertEntity(
            vehicleId = vehicleId,
            vehicleName = vehicle.name,
            licensePlate = vehicle.licensePlate,
            alertType = "ENGINE_IMMOBILIZED",
            severity = if (lock) "HIGH" else "INFO",
            title = if (lock) "🔒 ตัดระบบสตาร์ทเครื่องยนต์" else "🔓 ปลดล็อคระบบสตาร์ทเครื่องยนต์",
            description = if (lock) "ส่งคำสั่งสั่งดับ/ล็อคเครื่องยนต์ทางไกลสำเร็จ" else "ปลดล็อคเครื่องยนต์พร้อมใช้งาน",
            latitude = vehicle.currentLat,
            longitude = vehicle.currentLng,
            distanceFromRouteMeters = 0,
            timestamp = System.currentTimeMillis(),
            isAcknowledged = true
        )
        dao.insertAlert(alert)
    }

    suspend fun acknowledgeAlert(alertId: Long) = withContext(Dispatchers.IO) {
        dao.acknowledgeAlert(alertId)
    }

    suspend fun addAlert(alert: AlertEntity) = withContext(Dispatchers.IO) {
        dao.insertAlert(alert)
    }

    suspend fun addVehicle(vehicle: VehicleEntity) = withContext(Dispatchers.IO) {
        dao.insertVehicle(vehicle)
    }

    suspend fun updateVehicle(vehicle: VehicleEntity) = withContext(Dispatchers.IO) {
        dao.updateVehicle(vehicle)
    }

    suspend fun deleteVehicle(vehicleId: String) = withContext(Dispatchers.IO) {
        dao.deleteVehicle(vehicleId)
    }

    suspend fun addRoute(route: RouteGeofenceEntity) = withContext(Dispatchers.IO) {
        dao.insertRoute(route)
    }

    suspend fun deleteRoute(routeId: String) = withContext(Dispatchers.IO) {
        dao.deleteRoute(routeId)
    }

    fun parseWaypoints(jsonStr: String): List<LatLng> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            jsonStr.split(";").mapNotNull { part ->
                val coords = part.split(",")
                if (coords.size == 2) {
                    LatLng(coords[0].trim().toDouble(), coords[1].trim().toDouble())
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
