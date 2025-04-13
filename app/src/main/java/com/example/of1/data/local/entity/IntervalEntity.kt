package com.example.of1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Using date and driverNumber as composite primary key
@Entity(tableName = "intervals", primaryKeys = ["date", "driverNumber"])
data class IntervalEntity(
    // Removed @PrimaryKey autoGenerate = true
    val date: String, // Part of composite primary key
    val driverNumber: Int, // Part of composite primary key
    val sessionKey: Int,
    val meetingKey: Int,
    val gapToLeader: String?, // Store as String as API provides text/null
    val interval: String?     // Store as String
    // Add an index if querying frequently by sessionKey
    // @Index(value = ["sessionKey"])
)