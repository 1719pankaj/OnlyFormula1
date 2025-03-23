package com.example.of1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.of1.data.local.entity.DriverEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrivers(drivers: List<DriverEntity>)

    @Query("SELECT * FROM drivers WHERE sessionKey = :sessionKey") // Get All drivers.
    fun getDriversBySession(sessionKey: Int): Flow<List<DriverEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM drivers WHERE sessionKey = :sessionKey LIMIT 1)") // Check if driver exist.
    suspend fun hasDriversForSession(sessionKey: Int): Boolean

    @Query("DELETE FROM drivers WHERE sessionKey = :sessionKey")
    suspend fun deleteDriversBySession(sessionKey: Int)
}