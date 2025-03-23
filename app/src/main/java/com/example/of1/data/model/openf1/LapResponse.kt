package com.example.of1.data.model.openf1

import com.google.gson.annotations.SerializedName

data class OpenF1LapResponse(
    @SerializedName("meeting_key") val meetingKey: Int,
    @SerializedName("session_key") val sessionKey: Int,
    @SerializedName("driver_number") val driverNumber: Int,
    @SerializedName("i1_speed") val i1Speed: Int?, // Nullable
    @SerializedName("i2_speed") val i2Speed: Int?, // Nullable
    @SerializedName("st_speed") val stSpeed: Int?, // Nullable
    @SerializedName("date_start") val dateStart: String?, // Nullable
    @SerializedName("lap_duration") val lapDuration: Double?, // Nullable
    @SerializedName("is_pit_out_lap") val isPitOutLap: Boolean?,  //API docs says boolean, make nullable for safety
    @SerializedName("duration_sector_1") val durationSector1: Double?, // Nullable
    @SerializedName("duration_sector_2") val durationSector2: Double?, // Nullable
    @SerializedName("duration_sector_3") val durationSector3: Double?, // Nullable
    @SerializedName("segments_sector_1") val segmentsSector1: List<Int>?, // Nullable
    @SerializedName("segments_sector_2") val segmentsSector2: List<Int>?, // Nullable
    @SerializedName("segments_sector_3") val segmentsSector3: List<Int>?, // Nullable
    @SerializedName("lap_number") val lapNumber: Int
)

data class Lap( //Common model
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
    val segmentsSector3: List<Int>?
)