package com.example.of1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.of1.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    // Keep REPLACE: If session details change (unlikely but possible), they get updated.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SessionEntity>)

    // --- NEW: Query by Date Range ---
    // Note: SQLite date functions might be complex. Comparing ISO strings often works
    // if the format is consistent and includes timezones correctly (or all UTC).
    // Querying for sessions *within* the broad weekend range.
    @Query("SELECT * FROM sessions WHERE year = :year AND dateStart >= :startDate AND dateStart <= :endDate ORDER BY dateStart ASC")
    fun getSessionsByDateRange(year: Int, startDate: String, endDate: String): Flow<List<SessionEntity>>

    // --- NEW: Query by Meeting Key (useful if we get meetingKey earlier) ---
    @Query("SELECT * FROM sessions WHERE meetingKey = :meetingKey ORDER BY dateStart ASC")
    fun getSessionsByMeetingKey(meetingKey: Int): Flow<List<SessionEntity>>

    // --- NEW: Optional Delete by Date Range (if needed before inserting fresh data) ---
    @Query("DELETE FROM sessions WHERE year = :year AND dateStart >= :startDate AND dateStart <= :endDate")
    suspend fun deleteSessionsByDateRange(year: Int, startDate: String, endDate: String)

    // --- NEW: Optional Delete by Meeting Key ---
    @Query("DELETE FROM sessions WHERE meetingKey = :meetingKey")
    suspend fun deleteSessionsByMeetingKey(meetingKey: Int)

    // Remove or keep old queries based on country/session name if still needed elsewhere
    // @Query("SELECT * FROM sessions WHERE countryName = :countryName AND sessionName = :sessionName AND year = :year")
    // fun getSessions(countryName: String, sessionName: String, year: Int): Flow<List<SessionEntity>>
    // @Query("DELETE FROM sessions WHERE countryName = :countryName AND sessionName = :sessionName AND year = :year")
    // suspend fun deleteSessions(countryName: String, sessionName: String, year: Int)
}