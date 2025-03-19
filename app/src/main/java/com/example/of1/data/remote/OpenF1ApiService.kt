package com.example.of1.data.remote

import com.example.of1.data.model.Session
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenF1ApiService {
    @GET("sessions")
    suspend fun getSessions(
        @Query("country_name", encoded = false) countryName: String,
        @Query("session_name", encoded = false) sessionName: String,
        @Query("year") year: Int
    ): Response<List<Session>>
}