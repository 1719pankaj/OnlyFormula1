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
        emit(Resource.Loading())
        Log.d("LapRepository", "Initial Loading emitted")

        // Determine the latest lap number for updates (do this early)
        val latestLapNumber = lapDao.getLatestLapNumber(sessionKey, driverNumber)
        val adjustedLapNumber = if (latestLapNumber != null) latestLapNumber - 1 else null
        Log.d("LapRepository", "Latest lap number from DB: $latestLapNumber, Adjusted for query: $adjustedLapNumber")

        try {
            Log.d("LapRepository", "Fetching from API...")
            val response = apiService.getLaps(sessionKey, driverNumber, adjustedLapNumber)

            if (response.isSuccessful) {
                val laps = response.body() ?: emptyList()
                Log.d("LapRepository", "API call successful: ${laps.size} laps")

                val lapEntities = laps.map { lap ->
                    LapEntity(
                        meetingKey = lap.meetingKey,
                        sessionKey = lap.sessionKey,
                        driverNumber = lap.driverNumber,
                        i1Speed = lap.i1Speed,
                        i2Speed = lap.i2Speed,
                        stSpeed = lap.stSpeed,
                        dateStart = lap.dateStart,
                        lapDuration = lap.lapDuration,
                        isPitOutLap = lap.isPitOutLap,
                        durationSector1 = lap.durationSector1,
                        durationSector2 = lap.durationSector2,
                        durationSector3 = lap.durationSector3,
                        lapNumber = lap.lapNumber,
                        segmentsSector1 = lap.segmentsSector1,
                        segmentsSector2 = lap.segmentsSector2,
                        segmentsSector3 = lap.segmentsSector3,
                        lastUpdate = System.currentTimeMillis()
                    )
                }

                if(lapEntities.isNotEmpty()){
                    lapDao.insertLaps(lapEntities) // Insert/Update using REPLACE strategy
                    Log.d("LapRepository", "Inserted/Updated laps into database")
                }

                // After successful API call and DB update, query the DB again
                val allLaps = lapDao.getLapsBySessionAndDriver(sessionKey, driverNumber).first().map { entity ->
                    Lap(
                        driverNumber = entity.driverNumber,
                        lapNumber = entity.lapNumber,
                        lapDuration = entity.lapDuration,
                        durationSector1 = entity.durationSector1,
                        durationSector2 = entity.durationSector2,
                        durationSector3 = entity.durationSector3,
                        i1Speed = entity.i1Speed,
                        i2Speed = entity.i2Speed,
                        stSpeed = entity.stSpeed,
                        segmentsSector1 = entity.segmentsSector1,
                        segmentsSector2 = entity.segmentsSector2,
                        segmentsSector3 = entity.segmentsSector3,
                        dateStart = entity.dateStart
                    )
                }
                emit(Resource.Success(allLaps)) // Emit the final Success state
                Log.d("LapRepository", "Emitted Success from API/DB")

            } else {
                // API call failed, try emitting local data if available
                val localData = lapDao.getLapsBySessionAndDriver(sessionKey, driverNumber).first()
                if (localData.isNotEmpty()) {
                    Log.w("LapRepository", "API failed (${response.code()}), emitting cached data.")
                    emit(Resource.Success(localData.map { entity ->
                        Lap(
                            driverNumber = entity.driverNumber,
                            lapNumber = entity.lapNumber,
                            lapDuration = entity.lapDuration,
                            durationSector1 = entity.durationSector1,
                            durationSector2 = entity.durationSector2,
                            durationSector3 = entity.durationSector3,
                            i1Speed = entity.i1Speed,
                            i2Speed = entity.i2Speed,
                            stSpeed = entity.stSpeed,
                            segmentsSector1 = entity.segmentsSector1,
                            segmentsSector2 = entity.segmentsSector2,
                            segmentsSector3 = entity.segmentsSector3,
                            dateStart = entity.dateStart
                        )
                    }))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("LapRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                    emit(Resource.Error("Error fetching laps: ${response.code()} - $errorBody"))
                }
            }
        } catch (e: IOException) {
            // Network error, try emitting local data
            val localData = lapDao.getLapsBySessionAndDriver(sessionKey, driverNumber).first()
            if (localData.isNotEmpty()) {
                Log.w("LapRepository", "Network error, emitting cached data.", e)
                emit(Resource.Success(localData.map { entity ->
                    Lap(
                        driverNumber = entity.driverNumber,
                        lapNumber = entity.lapNumber,
                        lapDuration = entity.lapDuration,
                        durationSector1 = entity.durationSector1,
                        durationSector2 = entity.durationSector2,
                        durationSector3 = entity.durationSector3,
                        i1Speed = entity.i1Speed,
                        i2Speed = entity.i2Speed,
                        stSpeed = entity.stSpeed,
                        segmentsSector1 = entity.segmentsSector1,
                        segmentsSector2 = entity.segmentsSector2,
                        segmentsSector3 = entity.segmentsSector3,
                        dateStart = entity.dateStart
                    )
                }))
            } else {
                Log.e("LapRepository", "Network error", e)
                emit(Resource.Error("Network error: ${e.localizedMessage ?: "Check your internet connection."}"))
            }
        } catch (e: HttpException) {
            // HTTP error, try emitting local data
            val localData = lapDao.getLapsBySessionAndDriver(sessionKey, driverNumber).first()
            if (localData.isNotEmpty()) {
                Log.w("LapRepository", "HTTP error, emitting cached data.", e)
                emit(Resource.Success(localData.map { entity ->
                    Lap(
                        driverNumber = entity.driverNumber,
                        lapNumber = entity.lapNumber,
                        lapDuration = entity.lapDuration,
                        durationSector1 = entity.durationSector1,
                        durationSector2 = entity.durationSector2,
                        durationSector3 = entity.durationSector3,
                        i1Speed = entity.i1Speed,
                        i2Speed = entity.i2Speed,
                        stSpeed = entity.stSpeed,
                        segmentsSector1 = entity.segmentsSector1,
                        segmentsSector2 = entity.segmentsSector2,
                        segmentsSector3 = entity.segmentsSector3,
                        dateStart = entity.dateStart
                    )
                }))
            } else {
                Log.e("LapRepository", "HTTP error", e)
                emit(Resource.Error("HTTP error: ${e.localizedMessage ?: "An unexpected error occurred."}"))
            }
        } finally {
            emit(Resource.Loading(false)) // Ensure loading stops
            Log.d("LapRepository", "Emitted Loading(false) in finally block")
        }
    }.catch { e ->
        Log.e("LapRepository", "Flow error", e)
        emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        emit(Resource.Loading(false))
    }
}