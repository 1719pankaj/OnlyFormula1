package com.example.of1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.of1.data.local.entity.CarDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarData(carData: List<CarDataEntity>)

    @Query("SELECT * FROM car_data WHERE sessionKey = :sessionKey AND driverNumber = :driverNumber ORDER BY date DESC")
    fun getCarDataBySessionAndDriver(sessionKey: Int, driverNumber: Int): Flow<List<CarDataEntity>>

    @Query("SELECT MAX(date) FROM car_data WHERE sessionKey = :sessionKey AND driverNumber = :driverNumber") // Get Latest
    suspend fun getLatestCarDataTimestamp(sessionKey: Int, driverNumber: Int): String?
}