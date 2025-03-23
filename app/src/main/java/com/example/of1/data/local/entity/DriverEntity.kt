package com.example.of1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drivers")
data class DriverEntity(
    @PrimaryKey
    val driverNumber: Int,
    val broadcastName: String,
    val countryCode: String?, // Make nullable
    val fullName: String,
    val headshotUrl: String,
    val teamColour: String,
    val teamName: String,
    val sessionKey: Int,
    val meetingKey: Int
)