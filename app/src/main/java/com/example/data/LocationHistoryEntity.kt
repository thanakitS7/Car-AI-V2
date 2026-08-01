package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_history")
data class LocationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: String,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Int,
    val heading: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val isDeviationPoint: Boolean = false
)
