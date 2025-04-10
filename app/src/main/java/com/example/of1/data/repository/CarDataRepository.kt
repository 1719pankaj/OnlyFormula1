package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.CarDataDao
import com.example.of1.data.local.entity.CarDataEntity
import com.example.of1.data.model.openf1.CarData
import com.example.of1.data.remote.OpenF1ApiService
import com.example.of1.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class CarDataRepository @Inject constructor(
    private val apiService: OpenF1ApiService,
    private val carDataDao: CarDataDao
) {
    fun getCarData(sessionKey: Int, driverNumber: Int, startDate: String? = null, endDate: String? = null): Flow<Resource<List<CarData>>> = flow {
        emit(Resource.Loading())
        Log.d("CarDataRepository", "Initial Loading emitted")

        // 1. Fetch from local database, FILTERED by date if provided
        val localFlow = if (startDate != null && endDate != null) {
            Log.d("CarDataRepository", "Fetching LOCAL car data between $startDate and $endDate")
            carDataDao.getCarDataBySessionAndDriverAndDateRange(sessionKey, driverNumber, startDate, endDate)
        } else {
            Log.d("CarDataRepository", "Fetching ALL local car data (no date filter)")
            carDataDao.getCarDataBySessionAndDriver(sessionKey, driverNumber)
        }

        val localData = localFlow.first() // Get the first (and only) list emitted by the Flow

        if (localData.isNotEmpty()) {
            Log.d("CarDataRepository", "DB returned: ${localData.size}")
            val carData = localData.map { entity ->
                CarData(
                    driverNumber = entity.driverNumber,
                    date = entity.date,
                    rpm = entity.rpm,
                    speed = entity.speed,
                    nGear = entity.nGear,
                    throttle = entity.throttle,
                    drs = entity.drs,
                    brake = entity.brake
                )
            }
            emit(Resource.Success(carData))
            emit(Resource.Loading(false))
            Log.d("CarDataRepository", "Emitted Success from database and Loading(false)")
        }else{
            Log.d("CarDataRepository", "DB is empty")
        }

        // 2. Determine the latest date for API calls (only for live updates)
        val latestDate = if (startDate != null && endDate == null) { // Only for live updates!
            carDataDao.getLatestCarDataTimestamp(sessionKey, driverNumber)
        } else {
            null // No need for latestDate if we have both start and end
        }
        Log.d("CarDataRepository", "Latest date for API updates: $latestDate")

        // 3. Fetch from API (with appropriate filtering)
        try {
            val response = if (startDate != null && endDate != null) {
                // Historical data: use both start and end dates in API call
                apiService.getCarData(driverNumber, sessionKey, startDate, endDate)
            } else if (startDate != null) {
                // Live data: use only the start date in API call
                apiService.getCarData(driverNumber, sessionKey, startDate)
            }
            else {
                // Should not normally happen; avoid fetching all data.  Log an error
                Log.e("CarDataRepository", "getCarData called with no dates.  This is unexpected.")
                // In a real app, you'd probably want to throw an exception or return an error Resource.
                // For now, just return an empty response.  This prevents a full fetch.
                emit(Resource.Success(emptyList()))
                emit(Resource.Loading(false))
                return@flow
            }


            if (response.isSuccessful) {
                val carDataList = response.body() ?: emptyList()
                Log.d("CarDataRepository", "API call successful: ${carDataList.size} entries")

                val carDataEntities = carDataList.map { carData ->
                    CarDataEntity(
                        meetingKey = carData.meetingKey,
                        sessionKey = carData.sessionKey,
                        driverNumber = carData.driverNumber,
                        date = carData.date,
                        rpm = carData.rpm,
                        speed = carData.speed,
                        nGear = carData.nGear,
                        throttle = carData.throttle,
                        drs = carData.drs,
                        brake = carData.brake
                    )
                }
                if (carDataEntities.isNotEmpty()) {
                    carDataDao.insertCarData(carDataEntities)
                }

                val allCarData = (if (startDate != null && endDate != null) {
                    //If in the past we have data from start to end date.
                    carDataDao.getCarDataBySessionAndDriverAndDateRange(sessionKey, driverNumber, startDate, endDate)
                } else {
                    carDataDao.getCarDataBySessionAndDriver(sessionKey, driverNumber) // Get all if live.

                }).first().map { entity ->
                    CarData(
                        driverNumber = entity.driverNumber,
                        date = entity.date,
                        rpm = entity.rpm,
                        speed = entity.speed,
                        nGear = entity.nGear,
                        throttle = entity.throttle,
                        drs = entity.drs,
                        brake = entity.brake
                    )
                }
                emit(Resource.Success(allCarData))
                Log.d("CarDataRepository", "Emitted Success from API")

            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("CarDataRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                emit(Resource.Error("Error fetching car data: ${response.code()} - $errorBody"))
            }
        } catch (e: IOException) {
            Log.e("CarDataRepository", "Network error", e)
            emit(Resource.Error("Network error: ${e.localizedMessage ?: "Check your internet connection."}"))
        } catch (e: HttpException) {
            Log.e("CarDataRepository", "HTTP error", e)
            emit(Resource.Error("HTTP error: ${e.localizedMessage ?: "An unexpected error occurred."}"))
        } finally {
            emit(Resource.Loading(false)) // Important to stop loading in all cases
            Log.d("CarDataRepository", "Emitted Loading(false) in finally block")
        }
    }.catch { e ->
        Log.e("CarDataRepository", "Flow error", e)
        emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        emit(Resource.Loading(false)) // Ensure loading stops even on flow errors
    }
}