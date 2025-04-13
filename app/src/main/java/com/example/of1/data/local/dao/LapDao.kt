package com.example.of1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.of1.data.local.entity.LapEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LapDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaps(laps: List<LapEntity>)

    // Keep this for fetching specific driver laps
    @Query("SELECT * FROM laps WHERE sessionKey = :sessionKey AND driverNumber = :driverNumber ORDER BY lapNumber ASC")
    fun getLapsBySessionAndDriver(sessionKey: Int, driverNumber: Int): Flow<List<LapEntity>>

    // Keep this for getting latest lap number for a specific driver (used in repo for polling check)
    @Query("SELECT MAX(lapNumber) FROM laps WHERE sessionKey = :sessionKey AND driverNumber = :driverNumber")
    suspend fun getLatestLapNumber(sessionKey: Int, driverNumber: Int): Int?

    // --- NEW: Get *all* laps for a session (for fastest lap calculation) ---
    @Query("SELECT * FROM laps WHERE sessionKey = :sessionKey")
    fun getLapsBySession(sessionKey: Int): Flow<List<LapEntity>>

    // We return Flow<Int?> as the value might change during a live session.
    @Query("SELECT MAX(lapNumber) FROM laps WHERE sessionKey = :sessionKey")
    fun getMaxLapNumberForSession(sessionKey: Int): Flow<Int?> // Return Flow

    @Query("DELETE FROM laps WHERE sessionKey = :sessionKey AND driverNumber = :driverNumber")
    suspend fun deleteLapsBySessionAndDriver(sessionKey: Int, driverNumber: Int)

    // Optional: Add delete all laps for session if needed for full refresh
    @Query("DELETE FROM laps WHERE sessionKey = :sessionKey")
    suspend fun deleteLapsBySession(sessionKey: Int)
}