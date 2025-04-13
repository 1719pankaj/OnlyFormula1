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
        emit(Resource.Loading(true)) // Start loading
        Log.d("PositionRepository", "getPositions called for session $sessionKey")

        // 1. Emit Cached Data First
        var emittedCachedData = false
        var localData: List<PositionEntity> = emptyList() // Store local data for later checks
        try {
            localData = positionDao.getPositionsBySession(meetingKey, sessionKey).first()
            if (localData.isNotEmpty()) {
                val latestPositions = transformToLatestPositions(localData)
                Log.d("PositionRepository", "Emitting ${latestPositions.size} latest positions from cache.")
                emit(Resource.Success(latestPositions))
                emittedCachedData = true
                // Keep Loading(true) as we will fetch from network
            } else {
                Log.d("PositionRepository", "No cached positions found for session $sessionKey.")
            }
        } catch (e: Exception) {
            Log.e("PositionRepository", "Error fetching cached positions", e)
            // Continue to network fetch, emit error later if network fails too
        }

        // 2. Fetch from Network
        var latestTimestamp: String? = null // Initialize here
        try {
            // Determine the timestamp for polling *after* emitting cache
            latestTimestamp = if (emittedCachedData) {
                positionDao.getLatestTimestamp(meetingKey, sessionKey) // Fetch timestamp only if cache existed
            } else {
                null // Fetch all if cache was empty
            }
            Log.d("PositionRepository", "Fetching from API with sessionKey=$sessionKey, date>=$latestTimestamp")

            val response = apiService.getPositions(meetingKey, sessionKey, latestTimestamp)

            if (response.isSuccessful) {
                val newApiPositions = response.body() ?: emptyList()
                Log.d("PositionRepository", "API call successful: ${newApiPositions.size} new position entries")

                if (newApiPositions.isNotEmpty()) {
                    val positionEntities = newApiPositions.map {
                        PositionEntity( // Map API response to Entity
                            date = it.date,
                            driverNumber = it.driverNumber,
                            meetingKey = it.meetingKey,
                            position = it.position,
                            sessionKey = it.sessionKey
                        )
                    }
                    positionDao.insertPositions(positionEntities) // Insert new data (REPLACE strategy handles updates)
                    Log.d("PositionRepository", "Inserted/Updated positions in database")

                    // Query DB again to get the complete, updated list
                    val allPositions = positionDao.getPositionsBySession(meetingKey, sessionKey).first()
                    val latestCombinedPositions = transformToLatestPositions(allPositions)
                    emit(Resource.Success(latestCombinedPositions)) // Emit the final combined list
                    Log.d("PositionRepository", "Emitted Success from combined API/DB data")

                } else {
                    Log.d("PositionRepository", "API returned no new positions.")
                    // If API returned nothing new, the cached data (if any) is still the latest success state.
                    // We just need to ensure the Loading state is turned off.
                    if (!emittedCachedData) {
                        // If cache was empty AND API returned empty, emit empty success
                        emit(Resource.Success(emptyList()))
                    }
                    // If cache was emitted, its Success state remains valid.
                }
            } else {
                // API call failed
                val errorBody = response.errorBody()?.string()
                Log.e("PositionRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                // Emit error only if we didn't show cached data
                if (!emittedCachedData) {
                    emit(Resource.Error("Error fetching positions: ${response.code()} - $errorBody"))
                } else {
                    // If cache was shown, we don't emit error, just stop loading.
                    Log.w("PositionRepository", "API failed, but cached data was shown.")
                }
            }

        } catch (e: IOException) {
            Log.e("PositionRepository", "Network error", e)
            if (!emittedCachedData) { // Emit error only if cache wasn't shown
                emit(Resource.Error("Network error: ${e.localizedMessage ?: "Check connection"}"))
            } else {
                Log.w("PositionRepository", "Network error, but cached data was shown.")
            }
        } catch (e: HttpException) {
            Log.e("PositionRepository", "HTTP error", e)
            if (!emittedCachedData) { // Emit error only if cache wasn't shown
                emit(Resource.Error("HTTP error: ${e.localizedMessage ?: "Unexpected error"}"))
            } else {
                Log.w("PositionRepository", "HTTP error, but cached data was shown.")
            }
        } catch (e: Exception) {
            Log.e("PositionRepository", "Unexpected error during position fetch", e)
            if (!emittedCachedData) {
                emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
            } else {
                Log.w("PositionRepository", "Unexpected error, but cached data was shown.")
            }
        }
        finally {
            emit(Resource.Loading(false)) // Ensure loading stops regardless of path
            Log.d("PositionRepository", "Emitted Loading(false) in finally block")
        }
    } //.catch { ... } // .catch is less needed here as try/catch handles flow exceptions

    // Helper function remains the same
    private fun transformToLatestPositions(entities: List<PositionEntity>): List<Position> {
        return entities.groupBy { it.driverNumber }
            .mapValues { entry -> entry.value.maxByOrNull { it.date } }
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
            .sortedBy { it.position }
    }
}