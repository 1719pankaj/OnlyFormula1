package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.SessionDao
import com.example.of1.data.local.entity.SessionEntity
import com.example.of1.data.model.Session
import com.example.of1.data.remote.OpenF1ApiService
import com.example.of1.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class SessionRepository @Inject constructor(
    private val apiService: OpenF1ApiService,
    private val sessionDao: SessionDao
) {
    fun getSessions(countryName: String, sessionName: String, year: Int): Flow<Resource<List<Session>>> = flow {
        emit(Resource.Loading())
        Log.d("SessionRepository", "Initial Loading emitted")

        val localFlow: Flow<List<SessionEntity>> = sessionDao.getSessions(countryName, sessionName, year)
        val firstLocalData = localFlow.first()

        if (firstLocalData.isNotEmpty()) {
            Log.d("SessionRepository", "Database returned data: ${firstLocalData.size} items")
            val localSessions = firstLocalData.map { entity ->
                Session(
                    entity.circuitKey, entity.circuitShortName, entity.countryCode,
                    entity.countryKey, entity.countryName, entity.dateEnd, entity.dateStart,
                    entity.gmtOffset, entity.location, entity.meetingKey, entity.sessionKey,
                    entity.sessionName, entity.sessionType, entity.year
                )
            }
            emit(Resource.Success(localSessions))
            emit(Resource.Loading(false))
            Log.d("SessionRepository", "Emitted Success from database and Loading(false)")
        } else {
            Log.d("SessionRepository", "Database is empty or query returned no results.")
        }

        try {
            Log.d("SessionRepository", "Fetching from API...")
            //Crucial Change to obtain the response
            val response = apiService.getSessions(countryName, sessionName, year)

            // Log the raw response body *before* Moshi processing
            val rawResponse = response.raw().body?.string()
            Log.d("SessionRepository", "Raw JSON response: $rawResponse")


            if (response.isSuccessful) {
                val sessions = response.body() ?: emptyList() //This is where moshi is parsing
                Log.d("SessionRepository", "API call successful: ${sessions.size} sessions")

                val sessionEntities = sessions.map { session ->
                    SessionEntity(
                        session.sessionKey,
                        session.circuitKey,
                        session.circuitShortName,
                        session.countryCode,
                        session.countryKey,
                        session.countryName,
                        session.dateEnd,
                        session.dateStart,
                        session.gmtOffset,
                        session.location,
                        session.meetingKey,
                        session.sessionName,
                        session.sessionType,
                        session.year
                    )
                }
                sessionDao.deleteSessions(countryName, sessionName, year)
                Log.d("SessionRepository", "Deleted old sessions from database")
                sessionDao.insertSessions(sessionEntities)
                Log.d("SessionRepository", "Inserted new sessions into database")

                emit(Resource.Success(sessions))
                Log.d("SessionRepository", "Emitted Success from API")

            } else {
                // Log the error, if any (already implemented, kept for completeness)
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

    // Helper function to get the raw response body as a string (for logging)
    private fun Response<*>.rawBodyString(): String? {
        return try {
            val source = this.raw().body?.source()
            source?.request(Long.MAX_VALUE) // Buffer the entire body.
            val buffer = source?.buffer
            buffer?.clone()?.readUtf8()
        } catch (e: IOException) {
            Log.e("SessionRepository", "Error reading raw response body", e)
            null
        }
    }
}