package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.model.openf1.OpenF1IntervalResponse
import com.example.of1.data.remote.OpenF1ApiService
import com.example.of1.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntervalRepository @Inject constructor(
    private val apiService: OpenF1ApiService
) {
    private var latestTimestampPerSession = mutableMapOf<Int, String?>()

    // MODIFIED: Added isLive parameter
    fun getLatestIntervals(sessionKey: Int, isLive: Boolean): Flow<Resource<Map<Int, OpenF1IntervalResponse>>> = flow {
        emit(Resource.Loading())
        Log.d("IntervalRepository", "Fetching latest intervals for session $sessionKey (isLive: $isLive)")

        val timestampForQuery = if (isLive) {
            // For live sessions, use the polling timestamp logic
            getTimestampForQuery(sessionKey)
        } else {
            // For historical sessions, fetch everything
            null
        }

        Log.d("IntervalRepository", "Querying API with sessionKey=$sessionKey, date > $timestampForQuery")

        try {
            // Make the API call using the determined timestamp (null for historical)
            val response = apiService.getIntervals(sessionKey, timestampForQuery)

            if (response.isSuccessful) {
                val intervals = response.body() ?: emptyList()
                Log.d("IntervalRepository", "API call successful: ${intervals.size} interval entries received")

                if (intervals.isNotEmpty()) {
                    // If it's a live session, update the latest timestamp for the next poll
                    if (isLive) {
                        val batchLatestTimestamp = intervals.maxByOrNull { it.date }?.date
                        updateLatestTimestamp(sessionKey, batchLatestTimestamp)
                    }

                    // Process to get only the latest entry per driver (applies to both live poll results and full historical data)
                    val latestIntervalsMap = intervals
                        .groupBy { it.driverNumber }
                        .mapValues { entry ->
                            entry.value.maxByOrNull { it.date }!! // Find the most recent entry for this driver
                        }

                    Log.d("IntervalRepository", "Processed latest intervals for ${latestIntervalsMap.size} drivers")
                    emit(Resource.Success(latestIntervalsMap))
                } else {
                    // No data received (either nothing new in poll, or historical session had no data)
                    Log.d("IntervalRepository", "No interval data received for this query.")
                    emit(Resource.Success(emptyMap())) // Emit empty success
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("IntervalRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                emit(Resource.Error("Error fetching intervals: ${response.code()} - $errorBody"))
            }
        } catch (e: IOException) {
            Log.e("IntervalRepository", "Network error", e)
            emit(Resource.Error("Network error fetching intervals: ${e.localizedMessage ?: "Check connection"}"))
        } catch (e: HttpException) {
            Log.e("IntervalRepository", "HTTP error", e)
            emit(Resource.Error("HTTP error fetching intervals: ${e.localizedMessage ?: "Unexpected error"}"))
        }
        // No finally block needed here
    }.catch { e ->
        Log.e("IntervalRepository", "Flow error", e)
        emit(Resource.Error("Unexpected error fetching intervals: ${e.localizedMessage ?: "Unknown error"}"))
    }

    // --- Helper functions remain the same ---

    // Determines the correct timestamp for LIVE polling queries.
    private fun getTimestampForQuery(sessionKey: Int): String? {
        val storedTimestamp = latestTimestampPerSession[sessionKey]
        if (storedTimestamp != null) {
            return storedTimestamp
        } else {
            // Initial fetch for LIVE session: Get data from the last 15 seconds
            return try {
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.add(Calendar.SECOND, -15)
                // Using ISO 8601 format expected by the API for date comparisons
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
                dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                val initialTimestamp = dateFormat.format(calendar.time)
                Log.d("IntervalRepository", "Initial LIVE fetch for session $sessionKey, using timestamp: $initialTimestamp")
                initialTimestamp
            } catch (e: Exception) {
                Log.e("IntervalRepository", "Error generating initial timestamp", e)
                null // Fallback if date generation fails
            }
        }
    }

    // Updates the latest known timestamp for LIVE polling.
    private fun updateLatestTimestamp(sessionKey: Int, newTimestamp: String?) {
        if (newTimestamp != null) {
            val currentTimestamp = latestTimestampPerSession[sessionKey]
            if (currentTimestamp == null || newTimestamp > currentTimestamp) {
                latestTimestampPerSession[sessionKey] = newTimestamp
                Log.d("IntervalRepository", "Updated latest timestamp for session $sessionKey to: $newTimestamp")
            }
        }
    }

    fun clearTimestampCache() {
        latestTimestampPerSession.clear()
        Log.d("IntervalRepository", "Cleared timestamp cache")
    }
}