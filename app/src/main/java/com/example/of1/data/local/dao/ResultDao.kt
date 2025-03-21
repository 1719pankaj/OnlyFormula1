package com.example.of1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.of1.data.local.entity.ResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<ResultEntity>)

    @Query("SELECT * FROM results WHERE season = :season AND round = :round")
    fun getResultsByRace(season: String, round: String): Flow<List<ResultEntity>>

    @Query("DELETE FROM results WHERE season = :season AND round = :round")
    suspend fun deleteResultsByRace(season: String, round: String)
}