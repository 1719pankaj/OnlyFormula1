package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.LapDao
import com.example.of1.data.local.entity.LapEntity
import com.example.of1.data.model.openf1.Lap
import com.example.of1.data.remote.OpenF1ApiService
import com.example.of1.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class LapRepository @Inject constructor(
    private val apiService: OpenF1ApiService,
    private val lapDao: LapDao
) {

    fun getLaps(sessionKey: Int, driverNumber: Int): Flow<Resource<List<Lap>>> = flow {
        emit(Resource.Loading()) // Emit Loading initially
        Log.d("LapRepository", "Initial Loading emitted")

        // Determine the latest lap number for updates
        val latestLapNumber = lapDao.getLatestLapNumber(sessionKey, driverNumber)
        val adjustedLapNumber = if (latestLapNumber != null) latestLapNumber - 1 else null
        Log.d("LapRepository", "Latest lap#: $latestLapNumber, Adjusted#: $adjustedLapNumber")

        var emittedDataFromCache = false // Flag to track if cache was used on error

        try {
            Log.d("LapRepository", "Fetching from API...")
            val response = apiService.getLaps(sessionKey, driverNumber, adjustedLapNumber)

            if (response.isSuccessful) {
                val laps = response.body() ?: emptyList()
                Log.d("LapRepository", "API call successful: ${laps.size} laps")

                val lapEntities = laps.map { lap ->
                    LapEntity( /* ... mapping ... */
                        meetingKey = lap.meetingKey, sessionKey = lap.sessionKey, driverNumber = lap.driverNumber,
                        i1Speed = lap.i1Speed, i2Speed = lap.i2Speed, stSpeed = lap.stSpeed,
                        dateStart = lap.dateStart, lapDuration = lap.lapDuration, isPitOutLap = lap.isPitOutLap,
                        durationSector1 = lap.durationSector1, durationSector2 = lap.durationSector2, durationSector3 = lap.durationSector3,
                        lapNumber = lap.lapNumber, segmentsSector1 = lap.segmentsSector1, segmentsSector2 = lap.segmentsSector2,
                        segmentsSector3 = lap.segmentsSector3, lastUpdate = System.currentTimeMillis()
                    )
                }

                if(lapEntities.isNotEmpty()){
                    lapDao.insertLaps(lapEntities)
                    Log.d("LapRepository", "Inserted/Updated laps into database")
                }

                // Query DB *after* successful API call and insert/update
                val allLaps = lapDao.getLapsBySessionAndDriver(sessionKey, driverNumber).first().map { it.toLap() }
                emit(Resource.Success(allLaps))
                Log.d("LapRepository", "Emitted Success from API/DB")

            } else {
                // API call failed, try emitting local data if available
                Log.w("LapRepository", "API failed (${response.code()}), attempting to load from cache.")
                val localData = lapDao.getLapsBySessionAndDriver(sessionKey, driverNumber).first()
                if (localData.isNotEmpty()) {
                    emit(Resource.Success(localData.map { it.toLap() }))
                    emittedDataFromCache = true
                    Log.d("LapRepository", "Emitted Success from cache after API failure.")
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("LapRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                    emit(Resource.Error("Error fetching laps: ${response.code()} - $errorBody"))
                }
            }
        } catch (e: Exception) { // Catch broader exceptions like IOException, HttpException
            Log.e("LapRepository", "Error during lap fetch/processing", e)
            // Attempt to load from cache on any exception during API call/processing
            try {
                val localData = lapDao.getLapsBySessionAndDriver(sessionKey, driverNumber).first()
                if (localData.isNotEmpty()) {
                    Log.w("LapRepository", "Exception occurred, emitting cached data.", e)
                    emit(Resource.Success(localData.map { it.toLap() }))
                    emittedDataFromCache = true
                } else {
                    emit(Resource.Error("Error: ${e.localizedMessage ?: "Unknown fetch error"}"))
                }
            } catch (dbException: Exception) {
                Log.e("LapRepository", "Error fetching from DB after API exception", dbException)
                emit(Resource.Error("Error: ${e.localizedMessage ?: "Unknown fetch error"} and DB error"))
            }
        } finally {
            emit(Resource.Loading(false))
            Log.d("LapRepository", "Emitted Loading(false) in finally block")
        }
    }
    // Helper extension function (can be moved to a utils file if preferred)
    private fun LapEntity.toLap(): Lap {
        return Lap(
            driverNumber = this.driverNumber,
            lapNumber = this.lapNumber,
            lapDuration = this.lapDuration,
            durationSector1 = this.durationSector1,
            durationSector2 = this.durationSector2,
            durationSector3 = this.durationSector3,
            i1Speed = this.i1Speed,
            i2Speed = this.i2Speed,
            stSpeed = this.stSpeed,
            segmentsSector1 = this.segmentsSector1,
            segmentsSector2 = this.segmentsSector2,
            segmentsSector3 = this.segmentsSector3,
            dateStart = this.dateStart
        )
    }
}