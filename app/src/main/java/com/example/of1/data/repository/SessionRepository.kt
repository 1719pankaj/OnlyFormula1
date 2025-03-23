package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.SessionDao
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

class SessionRepository @Inject constructor(
    private val apiService: OpenF1ApiService,
    private val sessionDao: SessionDao // You might not need this if you're *only* using OpenF1 for sessions
) {

    //Get session by meeting key.
    fun getSessionsByMeetingKey(meetingKey: Int): Flow<Resource<List<Session>>> = flow {
        emit(Resource.Loading())
        Log.d("SessionRepository", "Initial Loading emitted")
        try {
            Log.d("SessionRepository", "Fetching from API...")
            val response = apiService.getSessionsByMeetingKey(meetingKey)
            if (response.isSuccessful) {
                val sessions = response.body() ?: emptyList()
                Log.d("SessionRepository", "API call successful: ${sessions.size} sessions")

                // Convert the OpenF1SessionResponse objects to Session objects for use in the UI.
                val uiSessions = sessions.map { it.toSession() } // Use the extension function
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
            emit(Resource.Loading(false))
            Log.d("SessionRepository", "Emitted Loading(false) in finally block")
        }
    }.catch { e ->
        Log.e("SessionRepository", "Flow error", e)
        emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        emit(Resource.Loading(false))
    }

    //Add get session by date
    fun getSessionsByDate(year: Int, dateStart: String, dateEnd: String): Flow<Resource<List<Session>>> = flow {
        emit(Resource.Loading())
        Log.d("SessionRepository", "getSessionsByDate called with year: $year, dateStart: $dateStart, dateEnd: $dateEnd") // ADDED LOG
        try {
            val response = apiService.getSessionsByDate(year, dateStart, dateEnd)
            Log.d("SessionRepository", "API call response: ${response.raw()}") // ADDED LOG - Log the raw response

            if (response.isSuccessful) {
                val sessions = response.body() ?: emptyList()
                Log.d("SessionRepository", "API call successful: ${sessions.size} sessions") // ADDED LOG
                // Convert to your common Session model
                val uiSessions = sessions.map { it.toSession() }
                emit(Resource.Success(uiSessions))
            } else {
                val errorBody = response.errorBody()?.string() // Get the error body
                Log.e("SessionRepository", "API call failed: ${response.code()}, errorBody: $errorBody") // ADDED LOG
                emit(Resource.Error("Error fetching sessions: ${response.code()} - $errorBody")) // Include error body
            }
        } catch (e: IOException) {
            Log.e("SessionRepository", "Network error", e)
            emit(Resource.Error("Network error: ${e.localizedMessage ?: "Check your internet connection."}"))
        } catch (e: HttpException) {
            Log.e("SessionRepository", "HTTP error", e)
            emit(Resource.Error("HTTP error: ${e.localizedMessage ?: "An unexpected error occurred."}"))
        } finally {
            emit(Resource.Loading(false))
            Log.d("SessionRepository", "Emitted Loading(false) in finally block") // ADDED LOG
        }
    }.catch { e ->
        Log.e("SessionRepository", "Flow error", e) // ADDED LOG - Catch flow errors
        emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        emit(Resource.Loading(false))
    }
}
// Extension function to convert OpenF1SessionResponse to the common Session model
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