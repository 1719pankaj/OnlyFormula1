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
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class SeasonRepository @Inject constructor(
    private val apiService: JolpicaApiService,
    private val seasonDao: SeasonDao
) {
    fun getSeasons(): Flow<Resource<List<Season>>> = flow {
        emit(Resource.Loading())
        Log.d("SeasonRepository", "Initial Loading emitted")

        // Fetch from the local database first.
        val localSeasons = seasonDao.getAllSeasons().first() // Use .first() to get a single list
        if (localSeasons.isNotEmpty()) {
            Log.d("SeasonRepository", "Database returned data: ${localSeasons.size} items")
            val seasons = localSeasons.map { entity ->
                Season(entity.year, entity.wikipediaUrl)
            }
            emit(Resource.Success(seasons)) // Emit database data
            emit(Resource.Loading(false)) // Indicate loading is finished
            Log.d("SeasonRepository", "Emitted Success from database and Loading(false)")
        } else {
            Log.d("SeasonRepository", "Database is empty or query returned no results")
        }

        try {
            Log.d("SeasonRepository", "Fetching seasons from API...")
            val response = apiService.getSeasons() // Fetch from API
            if (response.isSuccessful) {
                val seasonResponse = response.body()
                val seasons = seasonResponse?.mrData?.seasonTable?.seasons ?: emptyList()
                Log.d("SeasonRepository", "API call successful: ${seasons.size} seasons")

                // Convert to entity and store in database
                val seasonEntities = seasons.map { jolpicaSeason ->
                    com.example.of1.data.local.entity.SeasonEntity(jolpicaSeason.season, jolpicaSeason.url)
                }
                seasonDao.deleteAllSeasons() // Clear old data
                Log.d("SeasonRepository", "Deleted old seasons from database")
                seasonDao.insertSeasons(seasonEntities)
                Log.d("SeasonRepository", "Inserted new seasons into database")

                // Convert to the common model for the UI
                val uiSeasons = seasons.map { it.toSeason() }
                emit(Resource.Success(uiSeasons))
                Log.d("SeasonRepository", "Emitted Success from API")
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("SeasonRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                emit(Resource.Error("Error fetching seasons: ${response.code()} - $errorBody"))
            }
        } catch (e: IOException) {
            Log.e("SeasonRepository", "Network error", e)
            emit(Resource.Error("Network error: ${e.localizedMessage ?: "Check your internet connection."}"))
        } catch (e: HttpException) {
            Log.e("SeasonRepository", "HTTP error", e)
            emit(Resource.Error("HTTP error: ${e.localizedMessage ?: "An unexpected error occurred."}"))
        } finally {
            emit(Resource.Loading(false))
            Log.d("SeasonRepository", "Emitted Loading(false) in finally block")
        }
    }.catch { e ->
        Log.e("SeasonRepository", "Flow error", e)
        emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        emit(Resource.Loading(false))
    }
}

// Extension function to convert JolpicaSeason to our common Season model
fun JolpicaSeason.toSeason(): Season = Season(this.season, this.url)