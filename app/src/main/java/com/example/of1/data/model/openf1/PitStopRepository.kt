package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.PitStopDao
import com.example.of1.data.local.entity.PitStopEntity
import com.example.of1.data.model.openf1.OpenF1PitResponse
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
        //Get data from the database
        val localPitStops = pitStopDao.getPitStopsBySessionAndDriver(sessionKey,driverNumber).first()
        if(localPitStops.isNotEmpty()){
            Log.d("PitStopRepository", "Database returned: ${localPitStops.size} pitstops")
            val pitStops = localPitStops.map { entity ->
                PitStop(
                    driverNumber = entity.driverNumber,
                    lapNumber = entity.lapNumber,
                    pitDuration = entity.pitDuration,
                    date = entity.date
                )
            }
            emit(Resource.Success(pitStops))
            emit(Resource.Loading(false))
            Log.d("PitStopRepository", "Emitted Success from database and Loading(false)")
        } else{
            Log.d("PitStopRepository", "Database is empty")
        }

        //Determine the latest pit stop time for updates
        val latestDate = pitStopDao.getLatestPitStopTime(sessionKey,driverNumber)
        Log.d("PitStopRepository", "Latest pit stop time from DB: $latestDate")

        // Fetch from the API (with optional filtering)
        try {
            Log.d("PitStopRepository", "Fetching from API...")
            val response = apiService.getPits(sessionKey, driverNumber, latestDate) // Pass latestDate
            if (response.isSuccessful) {
                val pitStops = response.body() ?: emptyList()
                Log.d("PitStopRepository", "API call successful: ${pitStops.size} pit stops")

                val pitStopEntities = pitStops.map { pitStop ->
                    PitStopEntity(
                        sessionKey = pitStop.sessionKey,
                        meetingKey = pitStop.meetingKey,
                        driverNumber = pitStop.driverNumber,
                        date = pitStop.date,
                        pitDuration = pitStop.pitDuration,
                        lapNumber = pitStop.lapNumber
                    )
                }
                // Insert/update the database
                if (pitStopEntities.isNotEmpty()){
                    pitStopDao.insertPitStops(pitStopEntities)
                    Log.d("PitStopRepository", "Inserted new pit stops into database")
                }

                //Get all pitstops
                val allPitStops = pitStopDao.getPitStopsBySessionAndDriver(sessionKey, driverNumber).first().map { entity ->
                    PitStop(
                        driverNumber = entity.driverNumber,
                        lapNumber = entity.lapNumber,
                        pitDuration = entity.pitDuration,
                        date = entity.date
                    )
                }
                emit(Resource.Success(allPitStops)) // Map to UI model and emit
                Log.d("PitStopRepository", "Emitted Success from API")

            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("PitStopRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                emit(Resource.Error("Error fetching pit stops: ${response.code()} - $errorBody"))
            }
        } catch (e: IOException) {
            Log.e("PitStopRepository", "Network error", e)
            emit(Resource.Error("Network error: ${e.localizedMessage ?: "Check your internet connection."}"))
        } catch (e: HttpException) {
            Log.e("PitStopRepository", "HTTP error", e)
            emit(Resource.Error("HTTP error: ${e.localizedMessage ?: "An unexpected error occurred."}"))
        } finally {
            emit(Resource.Loading(false))
            Log.d("PitStopRepository", "Emitted Loading(false) in finally block")
        }
    }.catch { e ->
        Log.e("LapRepository", "Flow error", e)
        emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        emit(Resource.Loading(false))
    }
}