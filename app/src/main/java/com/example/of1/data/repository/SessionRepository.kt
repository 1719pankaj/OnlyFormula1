package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.SessionDao // Import DAO
import com.example.of1.data.local.entity.SessionEntity // Import Entity
import com.example.of1.data.model.Session
import com.example.of1.data.model.openf1.OpenF1SessionResponse
import com.example.of1.data.remote.OpenF1ApiService
import com.example.of1.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

// Keep the extension function at the top level
fun OpenF1SessionResponse.toSession(): Session = Session(
    circuitKey = this.circuitKey, circuitShortName = this.circuitShortName,
    countryCode = this.countryCode, countryKey = this.countryKey, countryName = this.countryName,
    dateEnd = this.dateEnd, dateStart = this.dateStart, gmtOffset = this.gmtOffset,
    location = this.location, meetingKey = this.meetingKey, sessionKey = this.sessionKey,
    sessionName = this.sessionName, sessionType = this.sessionType, year = this.year
)

// Add mapper from Entity to Domain Model
fun SessionEntity.toSession(): Session = Session(
    circuitKey = this.circuitKey, circuitShortName = this.circuitShortName,
    countryCode = this.countryCode, countryKey = this.countryKey, countryName = this.countryName,
    dateEnd = this.dateEnd, dateStart = this.dateStart, gmtOffset = this.gmtOffset,
    location = this.location, meetingKey = this.meetingKey, sessionKey = this.sessionKey,
    sessionName = this.sessionName, sessionType = this.sessionType, year = this.year
)

// Add mapper from API Model to Entity Model
fun OpenF1SessionResponse.toEntity(): SessionEntity = SessionEntity(
    sessionKey = this.sessionKey, circuitKey = this.circuitKey, circuitShortName = this.circuitShortName,
    countryCode = this.countryCode, countryKey = this.countryKey, countryName = this.countryName,
    dateEnd = this.dateEnd, dateStart = this.dateStart, gmtOffset = this.gmtOffset,
    location = this.location, meetingKey = this.meetingKey, sessionName = this.sessionName,
    sessionType = this.sessionType, year = this.year
)


class SessionRepository @Inject constructor(
    private val apiService: OpenF1ApiService,
    private val sessionDao: SessionDao // Inject DAO
) {

    // --- Get session by date range (Cache First) ---
    fun getSessionsByDate(year: Int, dateStart: String, dateEnd: String): Flow<Resource<List<Session>>> = flow {
        emit(Resource.Loading(true))
        Log.d("SessionRepository", "getSessionsByDate called with year: $year, dateStart: $dateStart, dateEnd: $dateEnd")

        // 1. Emit Cached Data First
        var emittedCachedData = false
        var cachedSessions: List<Session> = emptyList()
        try {
            // Adjust end date slightly for '<=' comparison if OpenF1 end date is exclusive
            // Assuming OpenF1 date_end< means "up to but not including", adjust Room query if needed.
            // For simplicity, using '<=' which might include sessions starting exactly on endDate.
            val cachedEntities = sessionDao.getSessionsByDateRange(year, dateStart, dateEnd).first()
            if (cachedEntities.isNotEmpty()) {
                cachedSessions = cachedEntities.map { it.toSession() }
                Log.d("SessionRepository", "Emitting ${cachedSessions.size} sessions from cache.")
                emit(Resource.Success(cachedSessions))
                emittedCachedData = true
            } else {
                Log.d("SessionRepository", "No cached sessions found for date range.")
            }
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error fetching cached sessions", e)
            // Continue to network fetch
        }

        // 2. Fetch from Network
        try {
            Log.d("SessionRepository", "Fetching sessions by date from API...")
            // API parameters date_start> and date_end< are correct
            val response = apiService.getSessionsByDate(year, dateStart, dateEnd)
            //Log.d("SessionRepository", "API call response raw: ${response.raw()}")

            if (response.isSuccessful) {
                val apiSessions = response.body() ?: emptyList()
                Log.d("SessionRepository", "API call successful: ${apiSessions.size} sessions")

                if (apiSessions.isNotEmpty()) {
                    val sessionEntities = apiSessions.map { it.toEntity() }
                    // Optional: Delete old ones for this range? REPLACE in DAO handles updates well.
                    // sessionDao.deleteSessionsByDateRange(year, dateStart, dateEnd)
                    sessionDao.insertSessions(sessionEntities)
                    Log.d("SessionRepository", "Inserted/Updated sessions in database")

                    // Query DB again for the potentially updated list within the range
                    val allEntities = sessionDao.getSessionsByDateRange(year, dateStart, dateEnd).first()
                    val combinedSessions = allEntities.map { it.toSession() }
                    emit(Resource.Success(combinedSessions))
                    Log.d("SessionRepository", "Emitted Success from combined API/DB session data")
                } else {
                    Log.d("SessionRepository", "API returned no sessions for date range.")
                    if (!emittedCachedData) {
                        emit(Resource.Success(emptyList())) // Ensure empty success if cache was empty too
                    }
                }
            } else {
                // API call failed
                val errorBody = response.errorBody()?.string()
                Log.e("SessionRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                if (!emittedCachedData) {
                    emit(Resource.Error("Error fetching sessions: ${response.code()} - $errorBody"))
                } else {
                    Log.w("SessionRepository", "API failed, but cached session data was shown.")
                }
            }
        } catch (e: IOException) {
            Log.e("SessionRepository", "Network error", e)
            if (!emittedCachedData) emit(Resource.Error("Network error: ${e.localizedMessage ?: "Check connection"}"))
            else Log.w("SessionRepository", "Network error on session fetch, but cached data was shown.")
        } catch (e: HttpException) {
            Log.e("SessionRepository", "HTTP error", e)
            if (!emittedCachedData) emit(Resource.Error("HTTP error: ${e.localizedMessage ?: "Unexpected error"}"))
            else Log.w("SessionRepository", "HTTP error on session fetch, but cached data was shown.")
        } catch (e: Exception) {
            Log.e("SessionRepository", "Unexpected error during session fetch", e)
            if (!emittedCachedData) emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
            else Log.w("SessionRepository", "Unexpected error on session fetch, but cached data was shown.")
        } finally {
            emit(Resource.Loading(false)) // Ensure loading stops
            Log.d("SessionRepository", "Session fetch process complete for date range.")
        }
    }

    // --- Get sessions by meeting key (Also Cache First) ---
    // Keep this if you might navigate using meetingKey directly later
    fun getSessionsByMeetingKey(meetingKey: Int): Flow<Resource<List<Session>>> = flow {
        emit(Resource.Loading(true))
        Log.d("SessionRepository", "getSessionsByMeetingKey called for meetingKey: $meetingKey")

        // 1. Emit Cached Data First
        var emittedCachedData = false
        try {
            val cachedEntities = sessionDao.getSessionsByMeetingKey(meetingKey).first()
            if (cachedEntities.isNotEmpty()) {
                val cachedSessions = cachedEntities.map { it.toSession() }
                Log.d("SessionRepository", "Emitting ${cachedSessions.size} sessions from cache for meeting $meetingKey.")
                emit(Resource.Success(cachedSessions))
                emittedCachedData = true
            } else {
                Log.d("SessionRepository", "No cached sessions found for meeting $meetingKey.")
            }
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error fetching cached sessions by meeting key", e)
        }

        // 2. Fetch from Network
        try {
            Log.d("SessionRepository", "Fetching sessions by meeting key $meetingKey from API...")
            val response = apiService.getSessionsByMeetingKey(meetingKey)

            if (response.isSuccessful) {
                val apiSessions = response.body() ?: emptyList()
                Log.d("SessionRepository", "API call successful: ${apiSessions.size} sessions for meeting $meetingKey")

                if (apiSessions.isNotEmpty()) {
                    val sessionEntities = apiSessions.map { it.toEntity() }
                    // sessionDao.deleteSessionsByMeetingKey(meetingKey) // Optional clear
                    sessionDao.insertSessions(sessionEntities)
                    Log.d("SessionRepository", "Inserted/Updated sessions in database for meeting $meetingKey")

                    val allEntities = sessionDao.getSessionsByMeetingKey(meetingKey).first()
                    val combinedSessions = allEntities.map { it.toSession() }
                    emit(Resource.Success(combinedSessions))
                    Log.d("SessionRepository", "Emitted Success from combined API/DB session data for meeting $meetingKey")
                } else {
                    Log.d("SessionRepository", "API returned no sessions for meeting $meetingKey.")
                    if (!emittedCachedData) emit(Resource.Success(emptyList()))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("SessionRepository", "API call failed for meeting key: ${response.code()}, errorBody: $errorBody")
                if (!emittedCachedData) emit(Resource.Error("Error fetching sessions by meeting key: ${response.code()} - $errorBody"))
                else Log.w("SessionRepository", "API failed for meeting key, but cached session data was shown.")
            }
        } catch (e: IOException) {
            Log.e("SessionRepository", "Network error (meeting key)", e)
            if (!emittedCachedData) emit(Resource.Error("Network error (meeting key): ${e.localizedMessage ?: "Check connection"}"))
            else Log.w("SessionRepository", "Network error on session fetch (meeting key), but cached data was shown.")
        } catch (e: HttpException) {
            Log.e("SessionRepository", "HTTP error (meeting key)", e)
            if (!emittedCachedData) emit(Resource.Error("HTTP error (meeting key): ${e.localizedMessage ?: "Unexpected error"}"))
            else Log.w("SessionRepository", "HTTP error on session fetch (meeting key), but cached data was shown.")
        } catch (e: Exception) {
            Log.e("SessionRepository", "Unexpected error during session fetch (meeting key)", e)
            if (!emittedCachedData) emit(Resource.Error("Unexpected error (meeting key): ${e.localizedMessage ?: "Unknown error"}"))
            else Log.w("SessionRepository", "Unexpected error on session fetch (meeting key), but cached data was shown.")
        } finally {
            emit(Resource.Loading(false))
            Log.d("SessionRepository", "Session fetch process complete for meeting $meetingKey.")
        }
    }
}