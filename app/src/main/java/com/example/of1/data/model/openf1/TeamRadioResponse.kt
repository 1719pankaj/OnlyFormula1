package com.example.of1.data.model.openf1

import com.google.gson.annotations.SerializedName

// Response structure from the API
data class OpenF1TeamRadioResponse(
    val date: String,
    @SerializedName("driver_number") val driverNumber: Int,
    @SerializedName("meeting_key") val meetingKey: Int,
    @SerializedName("recording_url") val recordingUrl: String,
    @SerializedName("session_key") val sessionKey: Int
)

// Common UI/Domain Model
data class TeamRadio(
    val date: String,
    val driverNumber: Int,
    val meetingKey: Int,
    val recordingUrl: String,
    val sessionKey: Int
)