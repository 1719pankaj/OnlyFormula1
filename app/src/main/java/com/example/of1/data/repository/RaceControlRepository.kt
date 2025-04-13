package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.RaceControlDao // Import DAO
import com.example.of1.data.local.entity.RaceControlEntity // Import Entity
import com.example.of1.data.model.openf1.OpenF1RaceControlResponse
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
class RaceControlRepository @Inject constructor(
    private val apiService: OpenF1ApiService,
    private val raceControlDao: RaceControlDao // Inject DAO
) {
    // Timestamp map removed - handled by DAO

    // Fetches messages, implementing cache-first strategy
    fun getMessages(sessionKey: Int): Flow<Resource<List<OpenF1RaceControlResponse>>> = flow {
        emit(Resource.Loading(true))
        Log.d("RaceControlRepository", "getMessages called for session $sessionKey")

        // 1. Emit Cached Data First
        var emittedCachedData = false
        var cachedMessages: List<OpenF1RaceControlResponse> = emptyList()
        try {
            val cachedEntities = raceControlDao.getAllMessagesForSession(sessionKey).first()
            if (cachedEntities.isNotEmpty()) {
                cachedMessages = cachedEntities.map { mapEntityToApiModel(it) }
                Log.d("RaceControlRepository", "Emitting ${cachedMessages.size} RC messages from cache.")
                emit(Resource.Success(cachedMessages))
                emittedCachedData = true
            } else {
                Log.d("RaceControlRepository", "No cached RC messages found for session $sessionKey.")
            }
        } catch (e: Exception) {
            Log.e("RaceControlRepository", "Error fetching cached RC messages", e)
            // Continue to network fetch
        }

        // 2. Fetch from Network
        var timestampForQuery: String? = null
        try {
            // Get latest timestamp from DAO for polling
            timestampForQuery = raceControlDao.getLatestTimestamp(sessionKey)
            Log.d("RaceControlRepository", "Fetching from API with sessionKey=$sessionKey, date > $timestampForQuery")

            val response = apiService.getRaceControlMessages(sessionKey, timestampForQuery)

            if (response.isSuccessful) {
                val newApiMessages = response.body() ?: emptyList()
                Log.d("RaceControlRepository", "API call successful: ${newApiMessages.size} new RC messages received")

                if (newApiMessages.isNotEmpty()) {
                    val messageEntities = newApiMessages.map { mapApiToEntity(it) }
                    raceControlDao.insertMessages(messageEntities) // IGNORE strategy handles duplicates
                    Log.d("RaceControlRepository", "Inserted/Ignored RC messages in database")

                    // Query DB again to get the complete, updated list
                    val allEntities = raceControlDao.getAllMessagesForSession(sessionKey).first()
                    val allMessages = allEntities.map { mapEntityToApiModel(it) }
                    emit(Resource.Success(allMessages))
                    Log.d("RaceControlRepository", "Emitted Success from combined API/DB RC data")
                } else {
                    Log.d("RaceControlRepository", "API returned no new RC messages.")
                    // If API returned nothing new, ensure cache state (if emitted) is the final success state.
                    if (!emittedCachedData) {
                        emit(Resource.Success(emptyList())) // Emit empty success if cache was also empty
                    }
                    // If cache was emitted, its Success state remains valid.
                }
            } else {
                // API call failed
                val errorBody = response.errorBody()?.string()
                Log.e("RaceControlRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                if (!emittedCachedData) { // Emit error only if cache wasn't shown
                    emit(Resource.Error("Error fetching race control: ${response.code()} - $errorBody"))
                } else {
                    Log.w("RaceControlRepository", "API failed, but cached RC data was shown.")
                }
            }

        } catch (e: IOException) {
            Log.e("RaceControlRepository", "Network error", e)
            if (!emittedCachedData) emit(Resource.Error("Network error fetching race control: ${e.localizedMessage ?: "Check connection"}"))
            else Log.w("RaceControlRepository", "Network error on RC fetch, but cached data was shown.")
        } catch (e: HttpException) {
            Log.e("RaceControlRepository", "HTTP error", e)
            if (!emittedCachedData) emit(Resource.Error("HTTP error fetching race control: ${e.localizedMessage ?: "Unexpected error"}"))
            else Log.w("RaceControlRepository", "HTTP error on RC fetch, but cached data was shown.")
        } catch (e: Exception) {
            Log.e("RaceControlRepository", "Unexpected error during RC fetch", e)
            if (!emittedCachedData) emit(Resource.Error("Unexpected error fetching race control: ${e.localizedMessage ?: "Unknown error"}"))
            else Log.w("RaceControlRepository", "Unexpected error on RC fetch, but cached data was shown.")
        } finally {
            emit(Resource.Loading(false)) // Ensure loading stops
            Log.d("RaceControlRepository", "RC fetch process complete for session $sessionKey.")
        }
    }

    // Helper to map API response to Entity
    private fun mapApiToEntity(api: OpenF1RaceControlResponse): RaceControlEntity {
        return RaceControlEntity(
            date = api.date,
            sessionKey = api.sessionKey,
            meetingKey = api.meetingKey,
            category = api.category,
            driverNumber = api.driverNumber,
            flag = api.flag,
            lapNumber = api.lapNumber,
            message = api.message,
            scope = api.scope,
            sector = api.sector
        )
    }

    // Helper to map Entity to API model (or a dedicated UI model)
    private fun mapEntityToApiModel(entity: RaceControlEntity): OpenF1RaceControlResponse {
        return OpenF1RaceControlResponse(
            date = entity.date,
            sessionKey = entity.sessionKey,
            meetingKey = entity.meetingKey,
            category = entity.category,
            driverNumber = entity.driverNumber,
            flag = entity.flag,
            lapNumber = entity.lapNumber,
            message = entity.message,
            scope = entity.scope,
            sector = entity.sector
        )
    }

    // Remove clearTimestampCache method - no longer needed.
    // fun clearTimestampCache(sessionKey: Int? = null) { ... }
}