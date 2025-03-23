package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.PositionDao
import com.example.of1.data.local.entity.PositionEntity
import com.example.of1.data.model.Position
import com.example.of1.data.remote.OpenF1ApiService
import com.example.of1.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class PositionRepository @Inject constructor(
    private val apiService: OpenF1ApiService,
    private val positionDao: PositionDao
) {
    fun getPositions(meetingKey: Int, sessionKey: Int): Flow<Resource<List<Position>>> = flow {
        emit(Resource.Loading())
        Log.d("PositionRepository", "Initial Loading emitted")
        // Fetch from the local database first.
        val localPositions = positionDao.getPositionsBySession(meetingKey, sessionKey).first()
        if (localPositions.isNotEmpty()) {
            Log.d("PositionRepository", "Database returned data: ${localPositions.size} items")
            //Transform the data to have the latest position of each driver.
            val latestPositions = transformToLatestPositions(localPositions)

            emit(Resource.Success(latestPositions)) // Emit database data
            emit(Resource.Loading(false))
            Log.d("PositionRepository", "Emitted Success from database and Loading(false)")
        }else{
            Log.d("PositionRepository", "Database is empty or query returned no results")
        }


        // Determine the timestamp for polling
        val latestTimestamp = positionDao.getLatestTimestamp(meetingKey, sessionKey)
        Log.d("PositionRepository", "Latest timestamp from DB: $latestTimestamp")
        val formattedDate = if (latestTimestamp != null) {
            // Use the latest timestamp from the database.
            latestTimestamp
        } else {
            //If no data use default value.
            null
        }

        try {
            Log.d("PositionRepository", "Fetching from API with date: $formattedDate")
            val response = apiService.getPositions(meetingKey, sessionKey, formattedDate)
            if(response.isSuccessful){
                val positions = response.body() ?: emptyList()
                Log.d("PositionRepository", "API call successful: ${positions.size} positions")
                // Convert API response to entities and save to database.
                val positionEntities = positions.map {
                    PositionEntity(
                        date = it.date,
                        driverNumber = it.driverNumber,
                        meetingKey = it.meetingKey,
                        position = it.position,
                        sessionKey = it.sessionKey
                    )
                }
                if(positionEntities.isNotEmpty()){
                    positionDao.insertPositions(positionEntities)
                }

                //Transform to latest postion
                val latestPositions = transformToLatestPositions(positionDao.getPositionsBySession(meetingKey, sessionKey).first()) // Fetch latest data
                emit(Resource.Success(latestPositions))
                Log.d("PositionRepository", "Emitted Success from API")

            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("PositionRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                emit(Resource.Error("Error fetching positions: ${response.code()} - $errorBody"))
            }

        } catch (e: IOException) {
            Log.e("PositionRepository", "Network error", e)
            emit(Resource.Error("Network error: ${e.localizedMessage ?: "Check your internet connection."}"))
        } catch (e: HttpException) {
            Log.e("PositionRepository", "HTTP error", e)
            emit(Resource.Error("HTTP error: ${e.localizedMessage ?: "An unexpected error occurred."}"))
        } finally {
            emit(Resource.Loading(false))
            Log.d("PositionRepository", "Emitted Loading(false) in finally block")
        }
    }.catch { e ->
        Log.e("PositionRepository", "Flow error", e)
        emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        emit(Resource.Loading(false))

    }
    // Helper function to convert a list of PositionEntity objects to a list of Position objects containing only the most recent position for each driver.
// Helper function to get the latest driver at each position
    private fun transformToLatestPositions(entities: List<PositionEntity>): List<Position> {
        return entities.groupBy { it.position } // Group by *position*
            .mapValues { entry ->
                entry.value.maxByOrNull { it.date } // Get the latest entry for each position
            }
            .values
            .filterNotNull()
            .map { entity ->
                Position(
                    entity.date,
                    entity.driverNumber,
                    entity.meetingKey,
                    entity.position,
                    entity.sessionKey
                )
            }
            .sortedBy { it.position } // Ensure correct order (though grouping by position should already do this)
    }
}