package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.PitStopDao
import com.example.of1.data.local.entity.PitStopEntity
import com.example.of1.data.model.openf1.PitStop
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

class PitStopRepository @Inject constructor(
    private val apiService: OpenF1ApiService,
    private val pitStopDao: PitStopDao
) {

    fun getPitStops(sessionKey: Int, driverNumber: Int): Flow<Resource<List<PitStop>>> = flow {
        emit(Resource.Loading())
        Log.d("PitStopRepository", "Initial Loading emitted")

        val latestDate = pitStopDao.getLatestPitStopTime(sessionKey, driverNumber)
        Log.d("PitStopRepository", "Latest pit stop time from DB: $latestDate")

        var emittedDataFromCache = false

        try {
            Log.d("PitStopRepository", "Fetching pit stops from API...")
            val response = apiService.getPits(sessionKey, driverNumber, latestDate)

            if (response.isSuccessful) {
                val pitStops = response.body() ?: emptyList()
                Log.d("PitStopRepository", "API call successful: ${pitStops.size} pit stops")

                val pitStopEntities = pitStops.map { pitStop ->
                    PitStopEntity( /* ... mapping ... */
                        sessionKey = pitStop.sessionKey, meetingKey = pitStop.meetingKey, driverNumber = pitStop.driverNumber,
                        date = pitStop.date, pitDuration = pitStop.pitDuration, lapNumber = pitStop.lapNumber
                    )
                }

                if (pitStopEntities.isNotEmpty()) {
                    pitStopDao.insertPitStops(pitStopEntities) // REPLACE strategy updates
                    Log.d("PitStopRepository", "Inserted/Updated pit stops into database")
                }

                // Query DB *after* successful API call and insert/update
                val allPitStops = pitStopDao.getPitStopsBySessionAndDriver(sessionKey, driverNumber).first().map { it.toPitStop() }
                emit(Resource.Success(allPitStops))
                Log.d("PitStopRepository", "Emitted Success from API/DB")

            } else {
                // API call failed, try emitting local data if available
                Log.w("PitStopRepository", "API failed (${response.code()}), attempting to load from cache.")
                val localData = pitStopDao.getPitStopsBySessionAndDriver(sessionKey, driverNumber).first()
                if (localData.isNotEmpty()) {
                    emit(Resource.Success(localData.map { it.toPitStop() }))
                    emittedDataFromCache = true
                    Log.d("PitStopRepository", "Emitted Success from cache after API failure.")
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("PitStopRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                    emit(Resource.Error("Error fetching pit stops: ${response.code()} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            Log.e("PitStopRepository", "Error during pit stop fetch/processing", e)
            // Attempt to load from cache on any exception
            try {
                val localData = pitStopDao.getPitStopsBySessionAndDriver(sessionKey, driverNumber).first()
                if (localData.isNotEmpty()) {
                    Log.w("PitStopRepository", "Exception occurred, emitting cached data.", e)
                    emit(Resource.Success(localData.map { it.toPitStop() }))
                    emittedDataFromCache = true
                } else {
                    emit(Resource.Error("Error: ${e.localizedMessage ?: "Unknown fetch error"}"))
                }
            } catch (dbException: Exception) {
                Log.e("PitStopRepository", "Error fetching from DB after API exception", dbException)
                emit(Resource.Error("Error: ${e.localizedMessage ?: "Unknown fetch error"} and DB error"))
            }
        } finally {
            emit(Resource.Loading(false))
            Log.d("PitStopRepository", "Emitted Loading(false) in finally block")
        }
    }
    // Helper extension function
    private fun PitStopEntity.toPitStop(): PitStop {
        return PitStop(
            driverNumber = this.driverNumber,
            lapNumber = this.lapNumber,
            pitDuration = this.pitDuration,
            date = this.date
        )
    }
}