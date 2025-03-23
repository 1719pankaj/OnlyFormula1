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
import kotlinx.coroutines.flow.firstOrNull
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
        val localPositionsFlow = positionDao.getPositionsBySession(meetingKey, sessionKey)
        val localPositions = localPositionsFlow.first() // Get the current data

        if (localPositions.isNotEmpty()) {
            Log.d("PositionRepository", "Database returned data: ${localPositions.size} items")
            val latestPositions = transformToLatestPositions(localPositions)
            emit(Resource.Success(latestPositions))
            emit(Resource.Loading(false))
            Log.d("PositionRepository", "Emitted Success from database and Loading(false)")
        } else {
            Log.d("PositionRepository", "Database is empty or query returned no results")
        }

        // Determine if we need to filter by date (for polling)
        val latestTimestamp = positionDao.getLatestTimestamp(meetingKey, sessionKey)
        Log.d("PositionRepository", "Latest timestamp from DB: $latestTimestamp")
        val formattedDate = latestTimestamp // Use timestamp only for polling

        try {
            Log.d("PositionRepository", "Fetching from API with date: $formattedDate")
            //Crucial Change: Pass formattedDate (which can be null)
            val response = apiService.getPositions(meetingKey, sessionKey, formattedDate)
            if (response.isSuccessful) {
                val positions = response.body() ?: emptyList()
                Log.d("PositionRepository", "API call successful: ${positions.size} positions")

                val positionEntities = positions.map {
                    PositionEntity(
                        date = it.date,
                        driverNumber = it.driverNumber,
                        meetingKey = it.meetingKey,
                        position = it.position,
                        sessionKey = it.sessionKey
                    )
                }
                if (positionEntities.isNotEmpty()) {
                    //Insert/update, we do not need to delete, because position data should not be deleted if there is new data.
                    positionDao.insertPositions(positionEntities)
                    Log.d("PositionRepository", "Inserted new positions into database")
                }

                // Get *latest* data from DB after insert/update
                val latestPositions = transformToLatestPositions(positionDao.getPositionsBySession(meetingKey, sessionKey).first())
                emit(Resource.Success(latestPositions)) // Emit combined data
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
    private fun transformToLatestPositions(entities: List<PositionEntity>): List<Position> {
        // Group by driver number, then take the latest entry for each driver based on the 'date' field.
        return entities.groupBy { it.driverNumber }
            .mapValues { entry ->
                entry.value.maxByOrNull { it.date }
            }
            .values
            .filterNotNull() // Ensure no nulls are passed forward
            .map { entity ->
                // Map from your PositionEntity to your Position data class.
                Position(
                    entity.date,
                    entity.driverNumber,
                    entity.meetingKey,
                    entity.position,
                    entity.sessionKey
                )
            }
            .sortedBy { it.position } // Add this line to sort by position
    }
}