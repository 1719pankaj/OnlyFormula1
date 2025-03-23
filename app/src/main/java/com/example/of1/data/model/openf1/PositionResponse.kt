package com.example.of1.data.model.openf1

import com.google.gson.annotations.SerializedName

data class OpenF1PositionResponse(
    val date: String,
    @SerializedName("driver_number") val driverNumber: Int,
    @SerializedName("meeting_key") val meetingKey: Int,
    val position: Int,
    @SerializedName("session_key") val sessionKey: Int
)