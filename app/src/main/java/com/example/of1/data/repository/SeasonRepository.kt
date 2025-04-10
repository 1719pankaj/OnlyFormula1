package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.SeasonDao
import com.example.of1.data.model.Season
import com.example.of1.data.model.jolpica.JolpicaSeason
import com.example.of1.data.remote.JolpicaApiService
import com.example.of1.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

// Keep the extension function at the top level
fun JolpicaSeason.toSeason(): Season = Season(this.season, this.url)

class SeasonRepository @Inject constructor(
    private val apiService: JolpicaApiService,
    private val seasonDao: SeasonDao
) {
    fun getSeasons(): Flow<Resource<List<Season>>> = flow {
        emit(Resource.Loading())
        Log.d("SeasonRepository", "Initial Loading emitted")

        // 1. Attempt to load and emit local data FIRST
        var localDataEmitted = false
        try {
            val localSeasonsEntities = seasonDao.getAllSeasons().first() // Get current local data
            if (localSeasonsEntities.isNotEmpty()) {
                val localSeasons = localSeasonsEntities.map { Season(it.year, it.wikipediaUrl) }.reversed() // Reverse here
                Log.d("SeasonRepository", "Emitting local seasons (reversed): ${localSeasons.size}")
                emit(Resource.Success(localSeasons))
                localDataEmitted = true
                // Don't turn off loading yet, API call follows
            } else {
                Log.d("SeasonRepository", "No local seasons found initially.")
            }
        } catch (e: Exception) {
            Log.e("SeasonRepository", "Error fetching local seasons", e)
            // Don't necessarily emit error yet, let API try
        }

        // 2. Attempt to fetch from API
        try {
            Log.d("SeasonRepository", "Fetching seasons from API...")
            val response = apiService.getSeasons()

            if (response.isSuccessful) {
                val seasonResponse = response.body()
                val seasons = seasonResponse?.mrData?.seasonTable?.seasons ?: emptyList()
                Log.d("SeasonRepository", "API call successful: ${seasons.size} seasons")

                val seasonEntities = seasons.map {
                    com.example.of1.data.local.entity.SeasonEntity(it.season, it.url)
                }
                seasonDao.deleteAllSeasons()
                Log.d("SeasonRepository", "Deleted old seasons from database")
                seasonDao.insertSeasons(seasonEntities)
                Log.d("SeasonRepository", "Inserted new seasons into database")

                val uiSeasons = seasons.map { it.toSeason() }.reversed() // REVERSE API data
                emit(Resource.Success(uiSeasons)) // Emit the FRESH data
                Log.d("SeasonRepository", "Emitted Success from API (reversed)")

            } else {
                // API failed, emit error ONLY if local data wasn't already successfully emitted
                if (!localDataEmitted) {
                    val errorBody = response.errorBody()?.string()
                    Log.e("SeasonRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                    emit(Resource.Error("Error fetching seasons: ${response.code()} - $errorBody"))
                } else {
                    Log.w("SeasonRepository", "API call failed (${response.code()}), but local data was shown.")
                }
            }
        } catch (e: IOException) {
            Log.e("SeasonRepository", "Network error", e)
            if (!localDataEmitted) {
                emit(Resource.Error("Network error: ${e.localizedMessage ?: "Check connection"}"))
            } else {
                Log.w("SeasonRepository", "Network error, but local data was shown.")
            }
        } catch (e: HttpException) {
            Log.e("SeasonRepository", "HTTP error", e)
            if (!localDataEmitted) {
                emit(Resource.Error("HTTP error: ${e.localizedMessage ?: "Unexpected error"}"))
            } else {
                Log.w("SeasonRepository", "HTTP error, but local data was shown.")
            }
        } finally {
            emit(Resource.Loading(false)) // Ensure loading stops at the end
            Log.d("SeasonRepository", "Emitted Loading(false) in finally block")
        }
    }.catch { e -> // Catch errors during the flow execution itself
        Log.e("SeasonRepository", "Flow error", e)
        emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        emit(Resource.Loading(false)) // Ensure loading stops
    }
}