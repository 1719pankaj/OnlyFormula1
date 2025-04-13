package com.example.of1.data.remote

import com.example.of1.data.model.jolpica.RaceResponse
import com.example.of1.data.model.jolpica.ResultResponse
import com.example.of1.data.model.jolpica.SeasonResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JolpicaApiService {
    @GET("{season}/races?format=json") // season in the path
    suspend fun getRacesForSeason(@Path("season") season: String): Response<RaceResponse>

    @GET("{season}/{round}/results?format=json")
    suspend fun getResultsForRace(
        @Path("season") season: String,
        @Path("round") round: String
    ): Response<ResultResponse>
}