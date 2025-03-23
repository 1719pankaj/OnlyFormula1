package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.CarDataDao
import com.example.of1.data.local.entity.CarDataEntity
import com.example.of1.data.model.openf1.CarData
import com.example.of1.data.model.openf1.OpenF1CarDataResponse
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

class CarDataRepository @Inject constructor(
    private val apiService: OpenF1ApiService,
    private val carDataDao: CarDataDao
) {
    fun getCarData(sessionKey: Int, driverNumber: Int): Flow<Resource<List<CarData>>> = flow {
        emit(Resource.Loading())
        Log.d("CarDataRepository", "Initial Loading emitted")

        val localData = carDataDao.getCarDataBySessionAndDriver(sessionKey, driverNumber).first()
        if (localData.isNotEmpty()){
            Log.d("CarDataRepository", "DB returned: ${localData.size}")
            //Transform data
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

        //Get latest date
        val latestDate = carDataDao.getLatestCarDataTimestamp(sessionKey, driverNumber)
        Log.d("CarDataRepository", "Latest date: $latestDate")

        try {
            val response = apiService.getCarData(driverNumber, sessionKey, latestDate)
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
                //Insert the new data
                if (carDataEntities.isNotEmpty()){
                    carDataDao.insertCarData(carDataEntities)
                }

                // Get all the data from db for emitting
                val allCarData = carDataDao.getCarDataBySessionAndDriver(sessionKey, driverNumber).first().map { entity ->
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
                emit(Resource.Success(allCarData)) // Map to UI model
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
            emit(Resource.Loading(false))
            Log.d("CarDataRepository", "Emitted Loading(false) in finally block")
        }
    }.catch { e ->
        Log.e("CarDataRepository", "Flow error", e)
        emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        emit(Resource.Loading(false))
    }
}