package com.example.of1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.of1.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SessionEntity>)

    @Query("SELECT * FROM sessions WHERE countryName = :countryName AND sessionName = :sessionName AND year = :year")
    fun getSessions(countryName: String, sessionName: String, year: Int): Flow<List<SessionEntity>>

    @Query("DELETE FROM sessions WHERE countryName = :countryName AND sessionName = :sessionName AND year = :year")
    suspend fun deleteSessions(countryName: String, sessionName: String, year: Int)
    // Add other queries as needed (e.g., get all sessions, delete all, etc.)
}