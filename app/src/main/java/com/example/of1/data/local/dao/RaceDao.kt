package com.example.of1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.of1.data.local.entity.RaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaces(races: List<RaceEntity>)

    @Query("SELECT * FROM races WHERE season = :season")
    fun getRacesForSeason(season: String): Flow<List<RaceEntity>>

    @Query("DELETE FROM races WHERE season = :season")
    suspend fun deleteRacesBySeason(season: String)
}