package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val licensePlate: String,
    val modelYear: String,
    val status: String, // "MOVING", "IDLE", "STOPPED", "ALERT_OUT_OF_ROUTE"
    val currentLat: Double,
    val currentLng: Double,
    val speedKmh: Int,
    val headingBearing: Float,
    val fuelPercent: Int,
    val batteryVoltage: Double,
    val activeRouteId: String?,
    val isEngineLocked: Boolean = false,
    val driverName: String = "สมชาย ใจดี (คนขับ)",
    val officeName: String = "ปณ.เมืองขอนแก่น",
    val provinceGroup: String = "ขอนแก่น",
    val lastUpdateMillis: Long = System.currentTimeMillis()
)
