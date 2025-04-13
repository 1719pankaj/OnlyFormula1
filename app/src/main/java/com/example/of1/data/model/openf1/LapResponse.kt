package com.example.of1.data.model.openf1

import com.google.gson.annotations.SerializedName

// API Response Model
data class OpenF1LapResponse(
    @SerializedName("meeting_key") val meetingKey: Int,
    @SerializedName("session_key") val sessionKey: Int,
    @SerializedName("driver_number") val driverNumber: Int,
    @SerializedName("i1_speed") val i1Speed: Int?,
    @SerializedName("i2_speed") val i2Speed: Int?,
    @SerializedName("st_speed") val stSpeed: Int?,
    @SerializedName("date_start") val dateStart: String?,
    @SerializedName("lap_duration") val lapDuration: Double?,
    @SerializedName("is_pit_out_lap") val isPitOutLap: Boolean?, // Keep this nullable
    @SerializedName("duration_sector_1") val durationSector1: Double?,
    @SerializedName("duration_sector_2") val durationSector2: Double?,
    @SerializedName("duration_sector_3") val durationSector3: Double?,
    @SerializedName("segments_sector_1") val segmentsSector1: List<Int>?,
    @SerializedName("segments_sector_2") val segmentsSector2: List<Int>?,
    @SerializedName("segments_sector_3") val segmentsSector3: List<Int>?,
    @SerializedName("lap_number") val lapNumber: Int
)

// Common UI/Domain model - ADD isPitOutLap
data class Lap(
    val driverNumber: Int,
    val lapNumber: Int,
    val lapDuration: Double?,
    val durationSector1: Double?,
    val durationSector2: Double?,
    val durationSector3: Double?,
    val i1Speed: Int?,
    val i2Speed: Int?,
    val stSpeed: Int?,
    val segmentsSector1: List<Int>?,
    val segmentsSector2: List<Int>?,
    val segmentsSector3: List<Int>?,
    val dateStart: String?,
    val isPitOutLap: Boolean? // ADD this field (nullable to match others)
)