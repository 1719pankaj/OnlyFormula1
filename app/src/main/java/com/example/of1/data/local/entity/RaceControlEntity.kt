package com.example.of1.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "race_control_messages",
    indices = [Index(value = ["sessionKey"])] // Index for faster session lookup
)
data class RaceControlEntity(
    @PrimaryKey // Use the unique timestamp as the primary key
    val date: String,
    val sessionKey: Int,
    val meetingKey: Int,
    val category: String?,
    val driverNumber: Int?,
    val flag: String?,
    val lapNumber: Int?,
    val message: String?,
    val scope: String?,
    val sector: Int?
)