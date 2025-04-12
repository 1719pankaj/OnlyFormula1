package com.example.of1.data.model.openf1

import com.google.gson.annotations.SerializedName

// API Response Model
data class OpenF1RaceControlResponse(
    val category: String?, // Can be null according to some examples? Let's make it nullable.
    val date: String, // Primary identifier
    @SerializedName("driver_number") val driverNumber: Int?, // Nullable if not driver specific
    val flag: String?, // Nullable if not a flag event
    @SerializedName("lap_number") val lapNumber: Int?, // Nullable
    @SerializedName("meeting_key") val meetingKey: Int,
    val message: String?, // The main text
    val scope: String?, // Can be null
    val sector: Int?, // Nullable
    @SerializedName("session_key") val sessionKey: Int
)