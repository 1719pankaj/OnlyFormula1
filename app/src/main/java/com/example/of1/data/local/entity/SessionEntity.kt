package com.example.of1.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    // Add index for faster querying by year and potentially date range start
    indices = [Index(value = ["year", "dateStart"])]
)
data class SessionEntity(
    @PrimaryKey
    val sessionKey: Int,
    val circuitKey: Int?, // Keep consistent with model (nullable)
    val circuitShortName: String?, // Keep consistent with model (nullable)
    val countryCode: String?, // Keep consistent with model (nullable)
    val countryKey: Int?, // Keep consistent with model (nullable)
    val countryName: String?, // Keep consistent with model (nullable)
    val dateEnd: String?, // Keep consistent with model (nullable)
    val dateStart: String?, // Keep consistent with model (nullable)
    val gmtOffset: String?, // Keep consistent with model (nullable)
    val location: String?, // Keep consistent with model (nullable)
    val meetingKey: Int,
    val sessionName: String,
    val sessionType: String?, // Keep consistent with model (nullable)
    val year: Int? // Keep consistent with model (nullable)
)