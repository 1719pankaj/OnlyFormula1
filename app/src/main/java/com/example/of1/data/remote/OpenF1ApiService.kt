package com.example.of1.data.remote

import com.example.of1.data.model.Meeting
import com.example.of1.data.model.openf1.OF1DriverResponse
import com.example.of1.data.model.openf1.OpenF1CarDataResponse
import com.example.of1.data.model.openf1.OpenF1LapResponse
import com.example.of1.data.model.openf1.OpenF1PitResponse
import com.example.of1.data.model.openf1.OpenF1PositionResponse
import com.example.of1.data.model.openf1.OpenF1SessionResponse
import com.example.of1.data.model.openf1.OpenF1TeamRadioResponse

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
        @Query("date>", encoded = false) date: String? = null // Add >= and encoded = false
    ): Response<List<OpenF1PositionResponse>>

    @GET("drivers")
    suspend fun getDrivers(
        @Query("session_key") sessionKey: Int // Only session_key is needed
    ): Response<List<OF1DriverResponse>>

    @GET("laps")
    suspend fun getLaps(
        @Query("session_key") sessionKey: Int,
        @Query("driver_number") driverNumber: Int,
        @Query("lap_number>") lapNumber: Int? = null // Optional for updates
    ): Response<List<OpenF1LapResponse>>

    @GET("car_data")
    suspend fun getCarData(
        @Query("driver_number") driverNumber: Int,
        @Query("session_key") sessionKey: Int,
        @Query("date>", encoded = false) date: String? = null, // Optional date for updates
        @Query("date<", encoded = false) endDate: String? = null, // Add endDate
        @Query("speed>", encoded = false) speedGreaterThan: Int = 1 // Add speed filter
    ): Response<List<OpenF1CarDataResponse>>

    @GET("pit")
    suspend fun getPits(
        @Query("session_key") sessionKey: Int,
        @Query("driver_number") driverNumber: Int,
        @Query("date>", encoded = false) date: String? = null // For live updates
    ): Response<List<OpenF1PitResponse>>

    @GET("team_radio") // Correct endpoint
    suspend fun getTeamRadio(
        @Query("session_key") sessionKey: Int,
        @Query("date>", encoded = false) date: String? = null // For live updates
    ): Response<List<OpenF1TeamRadioResponse>> // Use the correct response type

    @GET("drivers")
    suspend fun getDriverByName(
        @Query("first_name", encoded = false) firstName: String,
        @Query("last_name", encoded = false) lastName: String
    ): Response<List<OF1DriverResponse>> //

}