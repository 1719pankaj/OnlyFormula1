package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.DriverDao
import com.example.of1.data.local.dao.DriverNameTuple
import com.example.of1.data.local.entity.DriverEntity
import com.example.of1.data.model.openf1.OF1Driver
import com.example.of1.data.remote.OpenF1ApiService
import com.example.of1.utils.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


// --- Extension function ---
fun DriverEntity.toOF1Driver(): OF1Driver = OF1Driver(
    broadcastName = this.broadcastName,
    countryCode = this.countryCode,
    fullName = this.fullName,
    headshotUrl = this.headshotUrl,
    driverNumber = this.driverNumber,
    teamColour = this.teamColour,
    teamName = this.teamName,
    firstName = this.firstName,
    lastName = this.lastName
)
// --- End Extension ---


@Singleton // Make repository a singleton
class DriverRepository @Inject constructor(
    private val apiService: OpenF1ApiService,
    private val driverDao: DriverDao
) {
    // Create a dedicated scope for background tasks like fallback
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun getDrivers(sessionKey: Int, meetingKey: Int): Flow<Resource<List<OF1Driver>>> = flow {
        emit(Resource.Loading())
        Log.d("DriverRepository", "getDrivers called for sessionKey: $sessionKey")

        // 1. Try emitting local data first
        var localDataEmitted = false
        try {
            val localEntities = driverDao.getDriversBySession(sessionKey).first()
            if (localEntities.isNotEmpty()) {
                Log.d("DriverRepository", "Found ${localEntities.size} drivers locally")
                val localDrivers = localEntities.map { it.toOF1Driver() }
                emit(Resource.Success(localDrivers))
                localDataEmitted = true
                // Check for drivers needing headshot update *after* emitting local data
                checkAndFetchMissingHeadshots(localEntities, sessionKey)
            } else {
                Log.d("DriverRepository", "No local drivers found.")
            }
        } catch (e: Exception) {
            Log.e("DriverRepository", "Error fetching local drivers", e)
        }

        // 2. Fetch from API only if local data was empty
        if (!localDataEmitted) {
            Log.d("DriverRepository", "Fetching from API because local cache was empty...")
            try {
                val response = apiService.getDrivers(sessionKey = sessionKey)
                if (response.isSuccessful) {
                    val drivers = response.body() ?: emptyList()
                    Log.d("DriverRepository", "API call successful: ${drivers.size} drivers")

                    val driverEntities = drivers.map { driver ->
                        DriverEntity(
                            driverNumber = driver.driverNumber,
                            broadcastName = driver.broadcastName,
                            countryCode = driver.countryCode,
                            fullName = driver.fullName,
                            headshotUrl = driver.headshotUrl, // Potentially null here
                            teamColour = driver.teamColour,
                            teamName = driver.teamName,
                            sessionKey = sessionKey,
                            meetingKey = meetingKey,
                            firstName = driver.firstName, // Get names
                            lastName = driver.lastName
                        )
                    }
                    driverDao.insertDrivers(driverEntities)
                    Log.d("DriverRepository", "Inserted API drivers into db: ${driverEntities.size}")

                    val uiDrivers = driverEntities.map { it.toOF1Driver() }
                    emit(Resource.Success(uiDrivers))
                    // Check for missing headshots in the newly fetched data
                    checkAndFetchMissingHeadshots(driverEntities, sessionKey)

                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("DriverRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                    emit(Resource.Error("Error fetching drivers: ${response.code()} - $errorBody"))
                }
            } catch (e: IOException) {
                Log.e("DriverRepository", "Network error", e)
                emit(Resource.Error("Network error: ${e.localizedMessage ?: "Check connection"}"))
            } catch (e: HttpException) {
                Log.e("DriverRepository", "HTTP error", e)
                emit(Resource.Error("HTTP error: ${e.localizedMessage ?: "Unexpected error"}"))
            }
        }
        // No finally block needed here as Loading(false) is implicitly handled by Success/Error emission

    }.catch { e ->
        Log.e("DriverRepository", "Flow error", e)
        emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        emit(Resource.Loading(false)) // Ensure loading stops on flow error
    }

    // --- Fallback Headshot Logic ---
    private fun checkAndFetchMissingHeadshots(drivers: List<DriverEntity>, sessionKey: Int) {
        val driversWithoutHeadshots = drivers.filter { it.headshotUrl == null }
        if (driversWithoutHeadshots.isNotEmpty()) {
            Log.d("DriverRepository", "Found ${driversWithoutHeadshots.size} drivers with missing headshots for session $sessionKey. Launching fallback fetch.")
            repositoryScope.launch { // Launch in background scope
                driversWithoutHeadshots.forEach { driverEntity ->
                    fetchHeadshotByName(driverEntity, sessionKey)
                }
            }
        } else {
            Log.d("DriverRepository", "All drivers have headshots for session $sessionKey.")
        }
    }

    private suspend fun fetchHeadshotByName(driverNeedingUpdate: DriverEntity, sessionKey: Int) {
        Log.d("DriverRepository", "Attempting fallback fetch for ${driverNeedingUpdate.fullName} (Number: ${driverNeedingUpdate.driverNumber})")
        try {
            // Use names stored in the entity
            val response = apiService.getDriverByName(driverNeedingUpdate.firstName, driverNeedingUpdate.lastName)
            if (response.isSuccessful) {
                val potentialDrivers = response.body() ?: emptyList()
                // Find the first entry in the response with a non-null headshot
                val foundHeadshotUrl = potentialDrivers.firstNotNullOfOrNull { it.headshotUrl }

                if (foundHeadshotUrl != null) {
                    Log.i("DriverRepository", "Found fallback headshot for ${driverNeedingUpdate.fullName}: $foundHeadshotUrl")
                    // Update the specific driver entry in the database
                    driverDao.updateHeadshotUrl(sessionKey, driverNeedingUpdate.driverNumber, foundHeadshotUrl)
                    Log.d("DriverRepository","Updated headshot in DB for driver ${driverNeedingUpdate.driverNumber}")
                    // Note: This DB update WILL trigger the original Flow from the DAO to re-emit,
                    // which will update the UI automatically via the ViewModel's combine.
                } else {
                    Log.w("DriverRepository", "Fallback query for ${driverNeedingUpdate.fullName} returned no entries with a headshot URL.")
                }
            } else {
                Log.e("DriverRepository", "Fallback API call failed for ${driverNeedingUpdate.fullName}: ${response.code()}")
            }
        } catch (e: Exception) { // Catch broader exceptions for background task
            Log.e("DriverRepository", "Error during fallback headshot fetch for ${driverNeedingUpdate.fullName}", e)
        }
    }
    // --- End Fallback ---

    // getLocalDriver remains the same
    fun getLocalDriver(driverNumber: Int, sessionKey: Int): Flow<Resource<OF1Driver>> = flow {
        //... (same as before, ensure mapping includes names)
        emit(Resource.Loading())
        val driver = driverDao.getDriversBySession(sessionKey)
            .map { driverList ->
                driverList.firstOrNull { it.driverNumber == driverNumber }?.let { entity ->
                    Resource.Success(entity.toOF1Driver()) // Use extension fun
                } ?: Resource.Error("Driver not found")
            }.first()
        emit(driver)
    }
}