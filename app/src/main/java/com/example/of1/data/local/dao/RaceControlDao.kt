package com.example.of1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.of1.data.local.entity.RaceControlEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RaceControlDao {

    // Use IGNORE because if the exact message (by date PK) exists, we don't need to update it.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessages(messages: List<RaceControlEntity>)

    // Get all messages for a session, ordered by date ascending (chronological order).
    @Query("SELECT * FROM race_control_messages WHERE sessionKey = :sessionKey ORDER BY date ASC")
    fun getAllMessagesForSession(sessionKey: Int): Flow<List<RaceControlEntity>>

    // Get the latest timestamp for a given session for polling updates.
    @Query("SELECT MAX(date) FROM race_control_messages WHERE sessionKey = :sessionKey")
    suspend fun getLatestTimestamp(sessionKey: Int): String?

    // Delete messages for a session (optional, e.g., if doing a full historical refresh)
    @Query("DELETE FROM race_control_messages WHERE sessionKey = :sessionKey")
    suspend fun deleteMessagesBySession(sessionKey: Int)
}