package com.example.of1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "results")
data class ResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val season: String, // Needed for the query
    val round: String,   // Needed for the query
    val driverNumber: String,
    val position: String,
    val positionText: String,
    val points: String,
    val driverId: String,
    val driverPermanentNumber: String?,
    val driverCode: String?,
    val driverUrl: String,
    val driverGivenName: String,
    val driverFamilyName: String,
    val driverDateOfBirth: String,
    val driverNationality: String,
    val constructorId: String,
    val constructorUrl: String,
    val constructorName: String,
    val constructorNationality: String,
    val grid: String?,
    val laps: String?,
    val status: String?,
    val timeMillis: String?,
    val time: String?,
    val fastestLapRank: String?,
    val fastestLapLap: String?,
    val fastestLapTime: String?
)