package com.example.of1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.of1.data.local.entity.TeamRadioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamRadioDao {
    // Use IGNORE because if a message exists, we don't need to update it
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTeamRadios(radios: List<TeamRadioEntity>)

    @Query("SELECT * FROM team_radio WHERE sessionKey = :sessionKey ORDER BY date ASC")
    fun getTeamRadiosBySession(sessionKey: Int): Flow<List<TeamRadioEntity>>

    @Query("SELECT * FROM team_radio WHERE sessionKey = :sessionKey AND driverNumber = :driverNumber ORDER BY date ASC")
    fun getTeamRadiosBySessionAndDriver(sessionKey: Int, driverNumber: Int): Flow<List<TeamRadioEntity>>

    @Query("SELECT MAX(date) FROM team_radio WHERE sessionKey = :sessionKey")
    suspend fun getLatestRadioTimestamp(sessionKey: Int): String?
}