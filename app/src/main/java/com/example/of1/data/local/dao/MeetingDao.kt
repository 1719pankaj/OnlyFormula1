package com.example.of1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.of1.data.local.entity.MeetingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeetings(meetings: List<MeetingEntity>)

    @Query("SELECT * FROM meetings WHERE year = :year")
    fun getMeetingsByYear(year: Int): Flow<List<MeetingEntity>>

    @Query("DELETE FROM meetings WHERE year = :year")
    suspend fun deleteMeetingsByYear(year: Int)
}