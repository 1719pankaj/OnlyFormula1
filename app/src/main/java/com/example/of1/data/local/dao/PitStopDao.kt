package com.example.of1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.of1.data.local.entity.PitStopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PitStopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPitStops(pitStops: List<PitStopEntity>)

    @Query("SELECT * FROM pitstops WHERE sessionKey = :sessionKey AND driverNumber = :driverNumber")
    fun getPitStopsBySessionAndDriver(sessionKey: Int, driverNumber: Int): Flow<List<PitStopEntity>>

    @Query("SELECT MAX(date) FROM pitstops WHERE sessionKey = :sessionKey AND driverNumber = :driverNumber")
    suspend fun getLatestPitStopTime(sessionKey: Int, driverNumber: Int): String?
}