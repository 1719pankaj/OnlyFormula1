package com.example.of1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "car_data")
data class CarDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Auto-incrementing primary key
    val meetingKey: Int,
    val sessionKey: Int,
    val driverNumber: Int,
    val date: String, // Store as String (ISO 8601)
    val rpm: Int?,
    val speed: Int?,
    val nGear: Int?,
    val throttle: Int?,
    val drs: Int?,
    val brake: Int?
)