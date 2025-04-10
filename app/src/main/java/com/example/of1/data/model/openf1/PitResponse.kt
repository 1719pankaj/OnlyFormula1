package com.example.of1.data.model.openf1

import com.google.gson.annotations.SerializedName

data class OpenF1PitResponse (
    @SerializedName("session_key") val sessionKey: Int,
    @SerializedName("meeting_key") val meetingKey: Int,
    @SerializedName("driver_number") val driverNumber: Int,
    val date: String,
    @SerializedName("pit_duration") val pitDuration: Double?, // Can be null
    @SerializedName("lap_number") val lapNumber: Int
)

data class PitStop( // Add common Model
    val driverNumber: Int,
    val lapNumber: Int,
    val pitDuration: Double?,
    val date: String,
)