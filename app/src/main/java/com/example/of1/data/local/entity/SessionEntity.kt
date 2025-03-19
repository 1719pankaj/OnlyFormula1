package com.example.of1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    val sessionKey: Int,
    val circuitKey: Int,
    val circuitShortName: String,
    val countryCode: String,
    val countryKey: Int,
    val countryName: String,
    val dateEnd: String,
    val dateStart: String,
    val gmtOffset: String,
    val location: String,
    val meetingKey: Int,
    val sessionName: String,
    val sessionType: String,
    val year: Int
)