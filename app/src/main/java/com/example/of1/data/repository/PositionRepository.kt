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
import javax.inject.Inject

class PositionRepository @Inject constructor(
    private val apiService: OpenF1ApiService,
    private val positionDao: PositionDao
) {
    fun getPositions(meetingKey: Int, sessionKey: Int): Flow<Resource<List<Position>>> = flow {
        emit(Resource.Loading()) // Emit Loading initially
        Log.d("PositionRepository", "Initial Loading emitted")

        // Determine the timestamp for polling (only for live data, but fetch it here anyway)
        val latestTimestamp = positionDao.getLatestTimestamp(meetingKey, sessionKey)
        Log.d("PositionRepository", "Latest timestamp from DB: $latestTimestamp")

        try {
            Log.d("PositionRepository", "Fetching from API with date>=$latestTimestamp")
            val response = apiService.getPositions(meetingKey, sessionKey, latestTimestamp)

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
                    // Use REPLACE strategy in DAO, so insert updates existing ones based on primary key
                    positionDao.insertPositions(positionEntities)
                    Log.d("PositionRepository", "Inserted/Updated positions in database")
                }

                // After successful API call and DB update, query the DB again to get the full, latest list
                val allPositions = positionDao.getPositionsBySession(meetingKey, sessionKey).first()
                val latestPositions = transformToLatestPositions(allPositions)
                emit(Resource.Success(latestPositions)) // Emit the final Success state
                Log.d("PositionRepository", "Emitted Success from API/DB")

            } else {
                // API call failed, try emitting local data if available
                val localData = positionDao.getPositionsBySession(meetingKey, sessionKey).first()
                if (localData.isNotEmpty()) {
                    Log.w("PositionRepository", "API failed (${response.code()}), emitting cached data.")
                    emit(Resource.Success(transformToLatestPositions(localData)))
                } else {
                    // API failed and no local data
                    val errorBody = response.errorBody()?.string()
                    Log.e("PositionRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                    emit(Resource.Error("Error fetching positions: ${response.code()} - $errorBody"))
                }
            }

        } catch (e: IOException) {
            // Network error, try emitting local data
            val localData = positionDao.getPositionsBySession(meetingKey, sessionKey).first()
            if (localData.isNotEmpty()) {
                Log.w("PositionRepository", "Network error, emitting cached data.", e)
                emit(Resource.Success(transformToLatestPositions(localData)))
            } else {
                Log.e("PositionRepository", "Network error", e)
                emit(Resource.Error("Network error: ${e.localizedMessage ?: "Check your internet connection."}"))
            }
        } catch (e: HttpException) {
            // HTTP error, try emitting local data
            val localData = positionDao.getPositionsBySession(meetingKey, sessionKey).first()
            if (localData.isNotEmpty()) {
                Log.w("PositionRepository", "HTTP error, emitting cached data.", e)
                emit(Resource.Success(transformToLatestPositions(localData)))
            } else {
                Log.e("PositionRepository", "HTTP error", e)
                emit(Resource.Error("HTTP error: ${e.localizedMessage ?: "An unexpected error occurred."}"))
            }
        } finally {
            emit(Resource.Loading(false)) // Ensure loading stops
            Log.d("PositionRepository", "Emitted Loading(false) in finally block")
        }
    }.catch { e ->
        Log.e("PositionRepository", "Flow error", e)
        emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        emit(Resource.Loading(false)) // Ensure loading stops
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