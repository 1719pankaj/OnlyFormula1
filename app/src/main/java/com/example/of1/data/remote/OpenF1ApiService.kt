package com.example.of1.data.remote

import com.example.of1.data.model.Meeting
import com.example.of1.data.model.openf1.OF1DriverResponse
import com.example.of1.data.model.openf1.OpenF1PositionResponse
import com.example.of1.data.model.openf1.OpenF1SessionResponse

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenF1ApiService {
    @GET("sessions")
    suspend fun getSessions(
        @Query("country_name", encoded = false) countryName: String,
        @Query("session_name", encoded = false) sessionName: String,
        @Query("year") year: Int
    ): Response<List<OpenF1SessionResponse>>

    @GET("meetings") // New endpoint for meetings
    suspend fun getMeetings(@Query("year") year: Int): Response<List<Meeting>>

    @GET("sessions") // Get sessions by meeting key
    suspend fun getSessionsByMeetingKey(@Query("meeting_key") meetingKey: Int): Response<List<OpenF1SessionResponse>>

    @GET("sessions")
    suspend fun getSessionsByDate(
        @Query("year") year: Int,
        @Query(value = "date_start>", encoded = false) dateStart: String,
        @Query(value = "date_end<", encoded = false) dateEnd: String
    ): Response<List<OpenF1SessionResponse>>

    @GET("position")
    suspend fun getPositions(
        @Query("meeting_key") meetingKey: Int,
        @Query("session_key") sessionKey: Int,
        @Query("date") date: String? = null // Optional date for polling
    ): Response<List<OpenF1PositionResponse>>

    @GET("drivers")
    suspend fun getDrivers(
        @Query("session_key") sessionKey: Int // Only session_key is needed
    ): Response<List<OF1DriverResponse>>

}