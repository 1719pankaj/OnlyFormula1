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

    @Query("SELECT * FROM laps WHERE sessionKey = :sessionKey AND driverNumber = :driverNumber ORDER BY lapNumber ASC")
    fun getLapsBySessionAndDriver(sessionKey: Int, driverNumber: Int): Flow<List<LapEntity>>

    @Query("SELECT MAX(lapNumber) FROM laps WHERE sessionKey = :sessionKey AND driverNumber = :driverNumber")
    suspend fun getLatestLapNumber(sessionKey: Int, driverNumber: Int): Int?

    @Query("DELETE FROM laps WHERE sessionKey = :sessionKey AND driverNumber = :driverNumber") // Added Delete query
    suspend fun deleteLapsBySessionAndDriver(sessionKey: Int, driverNumber: Int)
}