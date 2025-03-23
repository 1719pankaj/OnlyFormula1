package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.DriverDao
import com.example.of1.data.local.entity.DriverEntity
import com.example.of1.data.model.openf1.OF1Driver
import com.example.of1.data.model.openf1.OF1DriverResponse
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

class DriverRepository @Inject constructor(
    private val apiService: OpenF1ApiService,
    private val driverDao: DriverDao
) {

    fun getDrivers(sessionKey: Int, meetingKey: Int): Flow<Resource<List<OF1Driver>>> = flow {
        emit(Resource.Loading())
        Log.d("DriverRepository", "Loading emitted")

        if (driverDao.hasDriversForSession(sessionKey)) {
            // Fetch from local database
            val localDrivers = driverDao.getDriversBySession(sessionKey).first()
            Log.d("DriverRepository", "Found data in local: ${localDrivers.size}")
            val drivers = localDrivers.map { entity ->
                OF1Driver(
                    entity.broadcastName,
                    entity.countryCode,
                    entity.fullName,
                    entity.headshotUrl,
                    entity.driverNumber,
                    entity.teamColour,
                    entity.teamName,
                )
            }
            emit(Resource.Success(drivers))
            emit(Resource.Loading(false))
            Log.d("DriverRepository", "Local data emitted")

        } else {
            Log.d("DriverRepository", "No Local data fetching from API...")
            try {
                // Use the session_key to get ALL drivers for the session
                val response = apiService.getDrivers(sessionKey = sessionKey) // Removed driverNumber
                Log.d("DriverRepository", "Response body: ${response.body()}")

                if (response.isSuccessful) {
                    val drivers = response.body() ?: emptyList()
                    Log.d("DriverRepository", "API call successful: ${drivers.size} drivers")

                    val driverEntities = drivers.map { driver ->
                        DriverEntity(
                            driverNumber = driver.driverNumber, // Primary key!
                            broadcastName = driver.broadcastName,
                            countryCode = driver.countryCode,
                            fullName = driver.fullName,
                            headshotUrl = driver.headshotUrl,
                            teamColour = driver.teamColour,
                            teamName = driver.teamName,
                            sessionKey = sessionKey,
                            meetingKey = meetingKey
                        )
                    }
                    driverDao.insertDrivers(driverEntities) // Insert all drivers
                    Log.d("DriverRepository", "Inserted into db ${driverEntities.size}")

                    // Map entities to UI model and emit
                    emit(Resource.Success(driverEntities.map {
                        OF1Driver(
                            it.broadcastName,
                            it.countryCode,
                            it.fullName,
                            it.headshotUrl,
                            it.driverNumber,
                            it.teamColour,
                            it.teamName
                        )
                    }))

                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("DriverRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                    emit(Resource.Error("Error fetching drivers: ${response.code()} - $errorBody"))
                }
            } catch (e: IOException) {
                Log.e("DriverRepository", "Network error", e)
                emit(Resource.Error("Network error: ${e.localizedMessage ?: "Check your internet connection."}"))
            } catch (e: HttpException) {
                Log.e("DriverRepository", "HTTP error", e)
                emit(Resource.Error("HTTP error: ${e.localizedMessage ?: "An unexpected error occurred."}"))
            } finally {
                emit(Resource.Loading(false)) // Ensure loading state is cleared
            }
        }
    }.catch { e ->
        Log.e("DriverRepository", "Flow error", e)
        emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        emit(Resource.Loading(false)) // Ensure loading is false on error
    }

    fun getLocalDriver(driverNumber: Int, sessionKey: Int): Flow<Resource<OF1Driver>> = flow {
        // ... (This method remains the same) ...
        emit(Resource.Loading())
        val driver = driverDao.getDriversBySession(sessionKey)
            .map { driverList ->
                driverList.firstOrNull { it.driverNumber == driverNumber }?.let { entity ->
                    Resource.Success(
                        OF1Driver(
                            entity.broadcastName,
                            entity.countryCode,
                            entity.fullName,
                            entity.headshotUrl,
                            entity.driverNumber,
                            entity.teamColour,
                            entity.teamName,
                        )
                    )
                } ?: Resource.Error("Driver not found")
            }.first() // Important, use .first() here for single emission

        emit(driver)
    }
}