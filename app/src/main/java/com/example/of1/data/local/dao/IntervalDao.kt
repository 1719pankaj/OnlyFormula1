package com.example.of1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.of1.data.local.entity.IntervalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IntervalDao {

    // REPLACE strategy: If we get a newer interval for the same driver/timestamp (unlikely but possible), it updates.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntervals(intervals: List<IntervalEntity>)

    // Get all intervals for a session, ordered by date.
    // Used primarily for historical fetches where we process the latest per driver later.
    @Query("SELECT * FROM intervals WHERE sessionKey = :sessionKey ORDER BY date DESC")
    fun getAllIntervalsForSession(sessionKey: Int): Flow<List<IntervalEntity>>

    // Get the latest timestamp for a given session for polling updates.
    @Query("SELECT MAX(date) FROM intervals WHERE sessionKey = :sessionKey")
    suspend fun getLatestTimestamp(sessionKey: Int): String?

    // Delete intervals for a session (optional, if you want to clear before inserting fresh historical data)
    @Query("DELETE FROM intervals WHERE sessionKey = :sessionKey")
    suspend fun deleteIntervalsBySession(sessionKey: Int)
}