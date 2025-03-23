package com.example.of1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.of1.utils.IntListConverter

@Entity(tableName = "laps",
    primaryKeys = ["sessionKey", "driverNumber", "lapNumber"] // Composite primary key
)
@TypeConverters(IntListConverter::class)
data class LapEntity(
    // No more auto-generated 'id'.  The primary key is now the combination below.
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
    val lapNumber: Int,  // Part of the primary key
    val segmentsSector1: List<Int>?,
    val segmentsSector2: List<Int>?,
    val segmentsSector3: List<Int>?,
    val lastUpdate: Long // Keep this for potential future use (though not part of the key)
)