package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.model.openf1.OpenF1RaceControlResponse
import com.example.of1.data.remote.OpenF1ApiService
import com.example.of1.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Repository holds state (timestamp)
class RaceControlRepository @Inject constructor(
    private val apiService: OpenF1ApiService
) {
    // Store the latest timestamp received from the API for subsequent polling
    private var latestTimestampPerSession = mutableMapOf<Int, String?>()

    fun getMessages(sessionKey: Int): Flow<Resource<List<OpenF1RaceControlResponse>>> = flow {
        // Don't emit Loading here, as this is primarily for polling.
        // The ViewModel can manage the initial loading state if needed.
        // emit(Resource.Loading())

        val timestampForQuery = latestTimestampPerSession[sessionKey]
        // Only log if polling (timestamp exists)
        if (timestampForQuery != null) {
            Log.d("RaceControlRepository", "Polling messages for session $sessionKey with date > $timestampForQuery")
        } else {
            Log.d("RaceControlRepository", "Initial fetch for messages for session $sessionKey (fetching all)")
        }


        try {
            val response = apiService.getRaceControlMessages(sessionKey, timestampForQuery)

            if (response.isSuccessful) {
                val newMessages = response.body() ?: emptyList()
                Log.d("RaceControlRepository", "API call successful: ${newMessages.size} race control messages received")

                if (newMessages.isNotEmpty()) {
                    // Find the overall latest timestamp from this batch
                    val batchLatestTimestamp = newMessages.maxByOrNull { it.date }?.date

                    // Update the stored timestamp IF the new one is later
                    updateLatestTimestamp(sessionKey, batchLatestTimestamp)

                    emit(Resource.Success(newMessages)) // Emit only the newly received messages
                } else {
                    // No new messages since the last poll/fetch
                    Log.d("RaceControlRepository", "No new race control messages received.")
                    // Emit empty success to signal completion without new data
                    emit(Resource.Success(emptyList()))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("RaceControlRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                // Emit error, the ViewModel should decide how to handle this (e.g., show toast, retry?)
                emit(Resource.Error("Error fetching race control: ${response.code()} - $errorBody"))
            }
        } catch (e: IOException) {
            Log.e("RaceControlRepository", "Network error", e)
            emit(Resource.Error("Network error fetching race control: ${e.localizedMessage ?: "Check connection"}"))
        } catch (e: HttpException) {
            Log.e("RaceControlRepository", "HTTP error", e)
            emit(Resource.Error("HTTP error fetching race control: ${e.localizedMessage ?: "Unexpected error"}"))
        }
    }.catch { e ->
        Log.e("RaceControlRepository", "Flow error", e)
        emit(Resource.Error("Unexpected error fetching race control: ${e.localizedMessage ?: "Unknown error"}"))
    }

    // Updates the latest known timestamp for the session if the new one is later.
    private fun updateLatestTimestamp(sessionKey: Int, newTimestamp: String?) {
        if (newTimestamp != null) {
            val currentTimestamp = latestTimestampPerSession[sessionKey]
            if (currentTimestamp == null || newTimestamp > currentTimestamp) {
                latestTimestampPerSession[sessionKey] = newTimestamp
                Log.d("RaceControlRepository", "Updated latest RC timestamp for session $sessionKey to: $newTimestamp")
            }
        }
    }

    // Function to clear the timestamp cache if needed (e.g., when ViewModel is cleared)
    fun clearTimestampCache(sessionKey: Int? = null) {
        if (sessionKey != null) {
            latestTimestampPerSession.remove(sessionKey)
            Log.d("RaceControlRepository", "Cleared timestamp cache for session $sessionKey")
        } else {
            latestTimestampPerSession.clear()
            Log.d("RaceControlRepository", "Cleared all timestamp cache")
        }
    }
}