package com.example.of1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "positions")
data class PositionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val date: String,
    val driverNumber: Int,
    val meetingKey: Int,
    val position: Int,
    val sessionKey: Int
)