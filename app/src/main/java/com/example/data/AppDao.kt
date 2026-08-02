package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // Vehicles
    @Query("SELECT * FROM vehicles")
    fun getAllVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :vehicleId")
    fun getVehicleById(vehicleId: String): Flow<VehicleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<VehicleEntity>)

    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity)

    @Query("DELETE FROM vehicles WHERE id = :vehicleId")
    suspend fun deleteVehicle(vehicleId: String)

    // Route / Geofences
    @Query("SELECT * FROM routes_geofences WHERE vehicleId = :vehicleId")
    fun getRoutesForVehicle(vehicleId: String): Flow<List<RouteGeofenceEntity>>

    @Query("SELECT * FROM routes_geofences")
    fun getAllRoutes(): Flow<List<RouteGeofenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteGeofenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(routes: List<RouteGeofenceEntity>)

    @Update
    suspend fun updateRoute(route: RouteGeofenceEntity)

    @Query("DELETE FROM routes_geofences WHERE id = :routeId")
    suspend fun deleteRoute(routeId: String)

    // Security Alerts
    @Query("SELECT * FROM security_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM security_alerts WHERE vehicleId = :vehicleId ORDER BY timestamp DESC")
    fun getAlertsForVehicle(vehicleId: String): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<AlertEntity>)

    @Query("UPDATE security_alerts SET isAcknowledged = 1 WHERE id = :alertId")
    suspend fun acknowledgeAlert(alertId: Long)

    @Query("DELETE FROM security_alerts")
    suspend fun clearAlerts()

    // Location History
    @Query("SELECT * FROM location_history WHERE vehicleId = :vehicleId ORDER BY timestamp ASC")
    fun getLocationHistory(vehicleId: String): Flow<List<LocationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationPoint(point: LocationHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationPoints(points: List<LocationHistoryEntity>)

    @Query("DELETE FROM location_history WHERE vehicleId = :vehicleId")
    suspend fun clearLocationHistory(vehicleId: String)
}
