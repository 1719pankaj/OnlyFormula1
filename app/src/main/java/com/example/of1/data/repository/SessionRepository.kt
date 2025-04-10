package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.SessionDao // Keep import, but DAO is unused
import com.example.of1.data.model.Session
import com.example.of1.data.model.openf1.OpenF1SessionResponse
import com.example.of1.data.remote.OpenF1ApiService
import com.example.of1.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

// Keep the extension function at the top level
fun OpenF1SessionResponse.toSession(): Session = Session(
    circuitKey = this.circuitKey,
    circuitShortName = this.circuitShortName,
    countryCode = this.countryCode,
    countryKey = this.countryKey,
    countryName = this.countryName,
    dateEnd = this.dateEnd,
    dateStart = this.dateStart,
    gmtOffset = this.gmtOffset,
    location = this.location,
    meetingKey = this.meetingKey,
    sessionKey = this.sessionKey,
    sessionName = this.sessionName,
    sessionType = this.sessionType,
    year = this.year
)

class SessionRepository @Inject constructor(
    private val apiService: OpenF1ApiService,
    // private val sessionDao: SessionDao // Mark as unused if not implementing caching
) {

    // Get session by meeting key.
    fun getSessionsByMeetingKey(meetingKey: Int): Flow<Resource<List<Session>>> = flow {
        emit(Resource.Loading()) // Emit Loading initially
        Log.d("SessionRepository", "getSessionsByMeetingKey - Initial Loading emitted")
        try {
            Log.d("SessionRepository", "Fetching sessions by meeting key from API...")
            val response = apiService.getSessionsByMeetingKey(meetingKey)
            if (response.isSuccessful) {
                val sessions = response.body() ?: emptyList()
                Log.d("SessionRepository", "API call successful: ${sessions.size} sessions")
                val uiSessions = sessions.map { it.toSession() }
                emit(Resource.Success(uiSessions))
                Log.d("SessionRepository", "Emitted Success from API")
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("SessionRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                emit(Resource.Error("Error fetching sessions: ${response.code()} - $errorBody"))
            }
        } catch (e: IOException) {
            Log.e("SessionRepository", "Network error", e)
            emit(Resource.Error("Network error: ${e.localizedMessage ?: "Check your internet connection."}"))
        } catch (e: HttpException) {
            Log.e("SessionRepository", "HTTP error", e)
            emit(Resource.Error("HTTP error: ${e.localizedMessage ?: "An unexpected error occurred."}"))
        } finally {
            emit(Resource.Loading(false)) // Ensure loading stops
            Log.d("SessionRepository", "getSessionsByMeetingKey - Emitted Loading(false) in finally block")
        }
    }.catch { e ->
        Log.e("SessionRepository", "Flow error in getSessionsByMeetingKey", e)
        emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        emit(Resource.Loading(false)) // Ensure loading stops
    }

    // Get session by date range.
    fun getSessionsByDate(year: Int, dateStart: String, dateEnd: String): Flow<Resource<List<Session>>> = flow {
        emit(Resource.Loading()) // Emit Loading initially
        Log.d("SessionRepository", "getSessionsByDate called with year: $year, dateStart: $dateStart, dateEnd: $dateEnd")
        try {
            Log.d("SessionRepository", "Fetching sessions by date from API...")
            val response = apiService.getSessionsByDate(year, dateStart, dateEnd)
            Log.d("SessionRepository", "API call response raw: ${response.raw()}") // Keep raw log for debugging if needed

            if (response.isSuccessful) {
                val sessions = response.body() ?: emptyList()
                Log.d("SessionRepository", "API call successful: ${sessions.size} sessions")
                val uiSessions = sessions.map { it.toSession() }
                emit(Resource.Success(uiSessions))
                Log.d("SessionRepository", "Emitted Success from API")
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("SessionRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                emit(Resource.Error("Error fetching sessions: ${response.code()} - $errorBody"))
            }
        } catch (e: IOException) {
            Log.e("SessionRepository", "Network error", e)
            emit(Resource.Error("Network error: ${e.localizedMessage ?: "Check your internet connection."}"))
        } catch (e: HttpException) {
            Log.e("SessionRepository", "HTTP error", e)
            emit(Resource.Error("HTTP error: ${e.localizedMessage ?: "An unexpected error occurred."}"))
        } finally {
            emit(Resource.Loading(false)) // Ensure loading stops
            Log.d("SessionRepository", "getSessionsByDate - Emitted Loading(false) in finally block")
        }
    }.catch { e ->
        Log.e("SessionRepository", "Flow error in getSessionsByDate", e)
        emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        emit(Resource.Loading(false)) // Ensure loading stops
    }
}