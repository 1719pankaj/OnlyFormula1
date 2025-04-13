package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.IntervalDao
import com.example.of1.data.local.entity.IntervalEntity
import com.example.of1.data.model.openf1.OpenF1IntervalResponse
import com.example.of1.data.remote.OpenF1ApiService
import com.example.of1.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntervalRepository @Inject constructor(
    private val apiService: OpenF1ApiService,
    private val intervalDao: IntervalDao // Inject DAO
) {
    // No longer need latestTimestampPerSession map, DAO handles it

    // Fetches intervals, implementing cache-first strategy
    fun getLatestIntervals(sessionKey: Int, isLive: Boolean): Flow<Resource<Map<Int, OpenF1IntervalResponse>>> = flow {
        emit(Resource.Loading(true))
        Log.d("IntervalRepository", "getLatestIntervals called for session $sessionKey (isLive: $isLive)")

        // 1. Emit Cached Data First (always try)
        var emittedCachedData = false
        try {
            // Get all cached intervals and process to find latest per driver
            val cachedEntities = intervalDao.getAllIntervalsForSession(sessionKey).first()
            if (cachedEntities.isNotEmpty()) {
                val latestCachedMap = processToLatestMap(cachedEntities)
                Log.d("IntervalRepository", "Emitting ${latestCachedMap.size} latest intervals from cache.")
                emit(Resource.Success(latestCachedMap))
                emittedCachedData = true
            } else {
                Log.d("IntervalRepository", "No cached intervals found for session $sessionKey.")
            }
        } catch (e: Exception) {
            Log.e("IntervalRepository", "Error fetching cached intervals", e)
            // Continue to network fetch
        }

        // 2. Fetch from Network
        var timestampForQuery: String? = null
        try {
            // Determine timestamp ONLY if live AND cache was successfully emitted or empty
            timestampForQuery = if (isLive) {
                intervalDao.getLatestTimestamp(sessionKey) // Use DAO for latest timestamp
            } else {
                null // Fetch all for historical
            }
            Log.d("IntervalRepository", "Fetching from API with sessionKey=$sessionKey, date > $timestampForQuery (isLive=$isLive)")

            val response = apiService.getIntervals(sessionKey, timestampForQuery)

            if (response.isSuccessful) {
                val newApiIntervals = response.body() ?: emptyList()
                Log.d("IntervalRepository", "API call successful: ${newApiIntervals.size} new interval entries received")

                if (newApiIntervals.isNotEmpty()) {
                    val intervalEntities = newApiIntervals.map { mapApiToEntity(it) }
                    // For historical, clear old before insert? Optional, REPLACE handles updates.
                    // If you expect massive overlap and want efficiency:
                    if (!isLive && timestampForQuery == null) {
                        Log.d("IntervalRepository", "Historical fetch: Deleting old intervals for session $sessionKey before insert.")
                        intervalDao.deleteIntervalsBySession(sessionKey)
                    }
                    intervalDao.insertIntervals(intervalEntities)
                    Log.d("IntervalRepository", "Inserted/Updated intervals in database")

                    // Query DB again to get the complete, updated list and process
                    val allIntervals = intervalDao.getAllIntervalsForSession(sessionKey).first()
                    val latestCombinedMap = processToLatestMap(allIntervals)
                    emit(Resource.Success(latestCombinedMap))
                    Log.d("IntervalRepository", "Emitted Success from combined API/DB interval data")

                } else {
                    Log.d("IntervalRepository", "API returned no new intervals.")
                    // If API returned nothing new, the cached data (if any) is the latest success state.
                    if (!emittedCachedData) {
                        emit(Resource.Success(emptyMap())) // Ensure empty success if cache was empty too
                    }
                }
            } else {
                // API call failed
                val errorBody = response.errorBody()?.string()
                Log.e("IntervalRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                if (!emittedCachedData) { // Emit error only if cache wasn't shown
                    emit(Resource.Error("Error fetching intervals: ${response.code()} - $errorBody"))
                } else {
                    Log.w("IntervalRepository", "API failed, but cached interval data was shown.")
                }
            }

        } catch (e: IOException) {
            Log.e("IntervalRepository", "Network error", e)
            if (!emittedCachedData) emit(Resource.Error("Network error fetching intervals: ${e.localizedMessage ?: "Check connection"}"))
            else Log.w("IntervalRepository", "Network error on interval fetch, but cached data was shown.")
        } catch (e: HttpException) {
            Log.e("IntervalRepository", "HTTP error", e)
            if (!emittedCachedData) emit(Resource.Error("HTTP error fetching intervals: ${e.localizedMessage ?: "Unexpected error"}"))
            else Log.w("IntervalRepository", "HTTP error on interval fetch, but cached data was shown.")
        } catch (e: Exception) {
            Log.e("IntervalRepository", "Unexpected error during interval fetch", e)
            if (!emittedCachedData) emit(Resource.Error("Unexpected error fetching intervals: ${e.localizedMessage ?: "Unknown error"}"))
            else Log.w("IntervalRepository", "Unexpected error on interval fetch, but cached data was shown.")
        } finally {
            emit(Resource.Loading(false)) // Ensure loading stops
            Log.d("IntervalRepository", "Interval fetch process complete for session $sessionKey.")
        }
    }

    // Helper to process a list of entities into a map of latest interval per driver
    private fun processToLatestMap(entities: List<IntervalEntity>): Map<Int, OpenF1IntervalResponse> {
        return entities
            .groupBy { it.driverNumber }
            .mapValues { entry ->
                // Find the latest entity for this driver
                val latestEntity = entry.value.maxByOrNull { it.date }!!
                // Map the latest entity back to the API response model for the ViewModel/UI
                mapEntityToApiModel(latestEntity)
            }
    }

    // Helper to map API response to Entity
    private fun mapApiToEntity(api: OpenF1IntervalResponse): IntervalEntity {
        return IntervalEntity(
            date = api.date,
            driverNumber = api.driverNumber,
            sessionKey = api.sessionKey,
            meetingKey = api.meetingKey,
            gapToLeader = api.gapToLeader,
            interval = api.interval
        )
    }

    // Helper to map Entity to API model (or a dedicated UI model if preferred)
    private fun mapEntityToApiModel(entity: IntervalEntity): OpenF1IntervalResponse {
        return OpenF1IntervalResponse(
            date = entity.date,
            driverNumber = entity.driverNumber,
            sessionKey = entity.sessionKey,
            meetingKey = entity.meetingKey,
            gapToLeader = entity.gapToLeader,
            interval = entity.interval
        )
    }

    // Cache clearing is now less critical as data is persisted, but can be kept if needed
    // for manual refresh scenarios.
    fun clearTimestampCache(sessionKey: Int? = null) {
        // This concept is handled by the DAO/database now.
        Log.d("IntervalRepository", "Timestamp cache is managed by DAO.")
    }
}