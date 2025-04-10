package com.example.of1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pitstops", primaryKeys = ["sessionKey", "driverNumber", "lapNumber"])
data class PitStopEntity(
    val sessionKey: Int,
    val meetingKey: Int,
    val driverNumber: Int,
    val date: String,
    val pitDuration: Double?,
    val lapNumber: Int
)