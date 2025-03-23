package com.example.of1.data.model.openf1

import com.google.gson.annotations.SerializedName

data class OpenF1CarDataResponse(
    @SerializedName("meeting_key") val meetingKey: Int,
    @SerializedName("session_key") val sessionKey: Int,
    @SerializedName("driver_number") val driverNumber: Int,
    val date: String,
    val rpm: Int?,
    val speed: Int?,
    @SerializedName("n_gear") val nGear: Int?,
    val throttle: Int?,
    val drs: Int?,
    val brake: Int?
)

data class CarData( //Common Model
    val driverNumber: Int,
    val date: String,
    val rpm: Int?,
    val speed: Int?,
    val nGear: Int?,
    val throttle: Int?,
    val drs: Int?,
    val brake: Int?
)