package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes_geofences")
data class RouteGeofenceEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val name: String,
    val type: String, // "ROUTE_CORRIDOR" or "CIRCLE_ZONE"
    val centerLat: Double,
    val centerLng: Double,
    val radiusMeters: Double, // Geofence radius or corridor width
    val maxAllowedSpeed: Int, // km/h
    val toleranceMeters: Int, // e.g. 100 meters
    val waypointsJson: String, // Stringified coordinates [lat,lng; lat,lng...]
    val isActive: Boolean = true,
    val startLocationName: String,
    val endLocationName: String
)
