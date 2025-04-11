package com.example.of1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "team_radio")
data class TeamRadioEntity(
    @PrimaryKey
    val recordingUrl: String, // Use URL as primary key
    val date: String,
    val driverNumber: Int,
    val meetingKey: Int,
    val sessionKey: Int
)