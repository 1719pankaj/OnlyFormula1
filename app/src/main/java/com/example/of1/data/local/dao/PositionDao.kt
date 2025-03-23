package com.example.of1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.of1.data.local.entity.PositionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PositionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPositions(positions: List<PositionEntity>)

    @Query("SELECT * FROM positions WHERE meetingKey = :meetingKey AND sessionKey = :sessionKey ORDER BY date DESC")
    fun getPositionsBySession(meetingKey: Int, sessionKey: Int): Flow<List<PositionEntity>>

    @Query("SELECT MAX(date) FROM positions WHERE meetingKey = :meetingKey AND sessionKey = :sessionKey")
    suspend fun getLatestTimestamp(meetingKey: Int, sessionKey: Int): String?

    @Query("DELETE FROM positions WHERE meetingKey = :meetingKey AND sessionKey = :sessionKey")
    suspend fun deletePositionsBySession(meetingKey: Int, sessionKey: Int)
}