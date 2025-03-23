package com.example.of1.data.model.openf1

import com.google.gson.annotations.SerializedName

data class OF1DriverResponse(
    @SerializedName("session_key") val sessionKey: Int,
    @SerializedName("meeting_key") val meetingKey: Int,
    @SerializedName("broadcast_name") val broadcastName: String,
    @SerializedName("country_code") val countryCode: String?, // Make nullable in the API response model too
    @SerializedName("first_name") val firstName: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("headshot_url") val headshotUrl: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("driver_number") val driverNumber: Int,
    @SerializedName("team_colour") val teamColour: String,
    @SerializedName("team_name") val teamName: String,
    @SerializedName("name_acronym") val nameAcronym: String
)

//Create common Model
data class OF1Driver(
    val broadcastName: String,
    val countryCode: String?, // Make nullable
    val fullName: String,
    val headshotUrl: String,
    val driverNumber: Int,
    val teamColour: String,
    val teamName: String,
)