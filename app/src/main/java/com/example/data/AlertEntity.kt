package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: String,
    val vehicleName: String,
    val licensePlate: String,
    val alertType: String, // "ROUTE_DEPARTURE", "SPEEDING", "GEOFENCE_EXIT", "ENGINE_IMMOBILIZED"
    val severity: String, // "CRITICAL", "HIGH", "WARNING", "INFO"
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val distanceFromRouteMeters: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isAcknowledged: Boolean = false
)
