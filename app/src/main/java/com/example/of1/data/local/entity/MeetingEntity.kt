package com.example.of1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey
    val meetingKey: Int,
    val circuitKey: Int,
    val circuitShortName: String,
    val countryCode: String,
    val countryKey: Int,
    val countryName: String,
    val dateStart: String,
    val gmtOffset: String,
    val location: String,
    val meetingName: String,
    val meetingOfficialName: String,
    val year: Int
)