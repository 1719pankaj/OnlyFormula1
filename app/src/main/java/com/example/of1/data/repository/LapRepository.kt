package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.LapDao
import com.example.of1.data.local.entity.LapEntity
import com.example.of1.data.model.openf1.Lap
import com.example.of1.data.model.openf1.OpenF1LapResponse // Import API Response
import com.example.of1.data.remote.OpenF1ApiService
import com.example.of1.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

// --- Mappers ---
private fun LapEntity.toLap(): Lap {
    return Lap(
        driverNumber = this.driverNumber, lapNumber = this.lapNumber, lapDuration = this.lapDuration,
        durationSector1 = this.durationSector1, durationSector2 = this.durationSector2, durationSector3 = this.durationSector3,
        i1Speed = this.i1Speed, i2Speed = this.i2Speed, stSpeed = this.stSpeed,
        segmentsSector1 = this.segmentsSector1, segmentsSector2 = this.segmentsSector2, segmentsSector3 = this.segmentsSector3,
        dateStart = this.dateStart, isPitOutLap = this.isPitOutLap
    )
}
private fun OpenF1LapResponse.toEntity(): LapEntity { // Changed receiver type
    return LapEntity(
        meetingKey = this.meetingKey, sessionKey = this.sessionKey, driverNumber = this.driverNumber,
        i1Speed = this.i1Speed, i2Speed = this.i2Speed, stSpeed = this.stSpeed,
        dateStart = this.dateStart, lapDuration = this.lapDuration, isPitOutLap = this.isPitOutLap,
        durationSector1 = this.durationSector1, durationSector2 = this.durationSector2, durationSector3 = this.durationSector3,
        lapNumber = this.lapNumber, segmentsSector1 = this.segmentsSector1, segmentsSector2 = this.segmentsSector2,
        segmentsSector3 = this.segmentsSector3, lastUpdate = System.currentTimeMillis()
    )
}


class LapRepository @Inject constructor(
    private val apiService: OpenF1ApiService,
    private val lapDao: LapDao
) {

    // --- Get Current Lap Number (for Live Display) ---
    // Directly expose the DAO Flow, applying distinctUntilChanged
    fun getCurrentLapNumber(sessionKey: Int): Flow<Int?> {
        Log.d("LapRepository", "Providing Flow for max lap number for session $sessionKey")
        return lapDao.getMaxLapNumberForSession(sessionKey).distinctUntilChanged()
    }


    // --- Get Laps for a Specific Driver (Cache First) ---
    // Renamed from getLaps
    fun getLapsForDriver(sessionKey: Int, driverNumber: Int): Flow<Resource<List<Lap>>> = flow {
        emit(Resource.Loading(true))
        Log.d("LapRepository", "getLapsForDriver called for session $sessionKey, driver $driverNumber")

        // 1. Emit Cached Data First
        var emittedCachedData = false
        try {
            val cachedEntities = lapDao.getLapsBySessionAndDriver(sessionKey, driverNumber).first()
            if (cachedEntities.isNotEmpty()) {
                val cachedLaps = cachedEntities.map { it.toLap() }
                Log.d("LapRepository", "Emitting ${cachedLaps.size} laps for driver $driverNumber from cache.")
                emit(Resource.Success(cachedLaps))
                emittedCachedData = true
            } else {
                Log.d("LapRepository", "No cached laps found for driver $driverNumber.")
            }
        } catch (e: Exception) {
            Log.e("LapRepository", "Error fetching cached laps for driver $driverNumber", e)
        }

        // 2. Fetch from Network (Updates for this specific driver)
        try {
            val latestLapNumberInCache = lapDao.getLatestLapNumber(sessionKey, driverNumber)
            val adjustedLapNumberForQuery = latestLapNumberInCache // Query for laps > latest known for this driver

            Log.d("LapRepository", "Fetching from API for driver $driverNumber with lap_number > $adjustedLapNumberForQuery")
            val response = apiService.getLaps(sessionKey, driverNumber, adjustedLapNumberForQuery)

            if (response.isSuccessful) {
                val newApiLaps = response.body() ?: emptyList()
                Log.d("LapRepository", "API call successful for driver $driverNumber: ${newApiLaps.size} new laps received")

                if (newApiLaps.isNotEmpty()) {
                    val lapEntities = newApiLaps.map { it.toEntity() }
                    lapDao.insertLaps(lapEntities)
                    Log.d("LapRepository", "Inserted/Updated laps into database for driver $driverNumber")

                    val allEntities = lapDao.getLapsBySessionAndDriver(sessionKey, driverNumber).first()
                    val combinedLaps = allEntities.map { it.toLap() }
                    emit(Resource.Success(combinedLaps))
                    Log.d("LapRepository", "Emitted Success for driver $driverNumber from combined API/DB lap data")
                } else {
                    Log.d("LapRepository", "API returned no new laps for driver $driverNumber.")
                    if (!emittedCachedData) emit(Resource.Success(emptyList()))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("LapRepository", "API call failed for driver $driverNumber: ${response.code()}, errorBody: $errorBody")
                if (!emittedCachedData) emit(Resource.Error("Error fetching laps for driver $driverNumber: ${response.code()} - $errorBody"))
                else Log.w("LapRepository", "API failed for driver $driverNumber, but cached data was shown.")
            }
        } catch (e: IOException) {
            Log.e("LapRepository", "Network error for driver $driverNumber", e)
            if (!emittedCachedData) emit(Resource.Error("Network error fetching laps: ${e.localizedMessage ?: "Check connection"}"))
            else Log.w("LapRepository", "Network error on lap fetch for driver $driverNumber, but cached data was shown.")
        } catch (e: HttpException) {
            Log.e("LapRepository", "HTTP error for driver $driverNumber", e)
            if (!emittedCachedData) emit(Resource.Error("HTTP error fetching laps: ${e.localizedMessage ?: "Unexpected error"}"))
            else Log.w("LapRepository", "HTTP error on lap fetch for driver $driverNumber, but cached data was shown.")
        } catch (e: Exception) {
            Log.e("LapRepository", "Unexpected error during lap fetch for driver $driverNumber", e)
            if (!emittedCachedData) emit(Resource.Error("Unexpected error fetching laps: ${e.localizedMessage ?: "Unknown error"}"))
            else Log.w("LapRepository", "Unexpected error on lap fetch for driver $driverNumber, but cached data was shown.")
        } finally {
            emit(Resource.Loading(false))
            Log.d("LapRepository", "Lap fetch process complete for session $sessionKey, driver $driverNumber.")
        }
    }


    // --- NEW: Get ALL Laps for a Session (Cache First - for Fastest Lap calc) ---
    fun getLapsForSession(sessionKey: Int): Flow<Resource<List<Lap>>> = flow {
        emit(Resource.Loading(true))
        Log.d("LapRepository", "getLapsForSession called for session $sessionKey")

        // 1. Emit Cached Data First
        var emittedCachedData = false
        try {
            val cachedEntities = lapDao.getLapsBySession(sessionKey).first()
            if (cachedEntities.isNotEmpty()) {
                val cachedLaps = cachedEntities.map { it.toLap() }
                Log.d("LapRepository", "Emitting ${cachedLaps.size} total laps for session $sessionKey from cache.")
                emit(Resource.Success(cachedLaps))
                emittedCachedData = true
            } else {
                Log.d("LapRepository", "No cached laps found for session $sessionKey.")
            }
        } catch (e: Exception) {
            Log.e("LapRepository", "Error fetching cached laps for session $sessionKey", e)
        }

        // 2. Fetch from Network (All laps for session, no lap_number filter)
        // Note: OpenF1 API doesn't support getting *all* laps without driver_number easily.
        // This approach is flawed for OpenF1.
        // We MUST fetch laps per driver if we need all data.
        // Let's STICK to fetching per driver for now.
        // The ViewModel will need to trigger fetches for *all* drivers if fastest laps are needed.
        // --- REVERTING THIS FUNCTION --- keeping getLapsForDriver

        // **Alternative for Requirement 2:**
        // The ViewModel will observe the list of drivers. When drivers are available,
        // it will launch individual fetches using getLapsForDriver for each one,
        // collecting the results and calculating fastest laps dynamically.

        // For now, just ensure getLapsForDriver works correctly.
        // We will handle the "fetch all laps" logic in the ViewModel later.

        Log.w("LapRepository", "getLapsForSession function is not implemented efficiently for OpenF1. Use getLapsForDriver.")
        // Emit the cached data status again or an error/empty if needed
        if (!emittedCachedData) {
            emit(Resource.Success(emptyList())) // Emit empty if cache was empty and network fetch is skipped
        }
        emit(Resource.Loading(false)) // Stop loading as we are not proceeding with network fetch here


    }.catch { e ->
        // This catch might not be reached if we exit early
        Log.e("LapRepository", "Flow error in getLapsForSession", e)
        emit(Resource.Error("Unexpected error in getLapsForSession: ${e.localizedMessage}"))
        emit(Resource.Loading(false))
    }
}