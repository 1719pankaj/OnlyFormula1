package com.example.of1.data.model.openf1

import com.google.gson.annotations.SerializedName

data class OpenF1SessionResponse(
    @SerializedName("circuit_key") val circuitKey: Int,
    @SerializedName("circuit_short_name") val circuitShortName: String,
    @SerializedName("country_code") val countryCode: String,
    @SerializedName("country_key") val countryKey: Int,
    @SerializedName("country_name") val countryName: String,
    @SerializedName("date_end") val dateEnd: String,
    @SerializedName("date_start") val dateStart: String,
    @SerializedName("gmt_offset") val gmtOffset: String,
    val location: String,
    @SerializedName("meeting_key") val meetingKey: Int,
    @SerializedName("session_key") val sessionKey: Int,
    @SerializedName("session_name") val sessionName: String,
    @SerializedName("session_type") val sessionType: String,
    val year: Int
)