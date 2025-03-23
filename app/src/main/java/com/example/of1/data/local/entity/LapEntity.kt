package com.example.of1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.of1.utils.IntListConverter

@Entity(tableName = "laps")
@TypeConverters(IntListConverter::class)
data class LapEntity(
    @PrimaryKey(autoGenerate = true) //AutoIncrement
    val id: Long = 0, // Use a unique ID, auto-generated
    val meetingKey: Int,
    val sessionKey: Int,
    val driverNumber: Int,
    val i1Speed: Int?,
    val i2Speed: Int?,
    val stSpeed: Int?,
    val dateStart: String?,
    val lapDuration: Double?,
    val isPitOutLap: Boolean?,
    val durationSector1: Double?,
    val durationSector2: Double?,
    val durationSector3: Double?,
    val lapNumber: Int,
    val segmentsSector1: List<Int>?, //for segments
    val segmentsSector2: List<Int>?, //for segments
    val segmentsSector3: List<Int>?, //for segments
    val lastUpdate: Long // Timestamp of the last update for this lap
)