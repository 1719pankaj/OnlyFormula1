package com.example.of1.data.model.openf1

import com.google.gson.annotations.SerializedName

// API Response Model
data class OpenF1IntervalResponse(
    // Use String? initially to handle potential "+1 LAP" text or Double values.
    // We'll parse/format later in the UI/ViewModel.
    @SerializedName("gap_to_leader") val gapToLeader: String?,
    val interval: String?,
    @SerializedName("driver_number") val driverNumber: Int,
    val date: String, // ISO 8601 format
    @SerializedName("session_key") val sessionKey: Int,
    @SerializedName("meeting_key") val meetingKey: Int
)

// Optional: Domain/UI Model (if needed for more complex logic later)
// For now, we'll use OpenF1IntervalResponse directly in the map
// data class IntervalData(
//     val gapToLeader: String?,
//     val interval: String?,
//     val driverNumber: Int,
//     val date: String // Keep the date for comparison
// )