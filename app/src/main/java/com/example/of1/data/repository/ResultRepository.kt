package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.ResultDao
import com.example.of1.data.local.entity.ResultEntity
import com.example.of1.data.model.Constructor
import com.example.of1.data.model.Driver
import com.example.of1.data.model.FastestLap
import com.example.of1.data.model.FastestLapTime
import com.example.of1.data.model.Result
import com.example.of1.data.model.ResultTime
import com.example.of1.data.model.jolpica.JolpicaRaceResult
import com.example.of1.data.remote.JolpicaApiService
import com.example.of1.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class ResultRepository @Inject constructor(
    private val apiService: JolpicaApiService,
    private val resultDao: ResultDao
) {
    fun getResults(season: String, round: String): Flow<Resource<List<Result>>> = flow {
        emit(Resource.Loading())
        Log.d("ResultRepository", "Initial Loading emitted")
        // Fetch from the local database first.
        val localResults = resultDao.getResultsByRace(season, round).first()
        if (localResults.isNotEmpty()) {
            Log.d("ResultRepository", "Database returned data: ${localResults.size} items")
            val results = localResults.map { entity ->
                // Map from your ResultEntity to your Result data class
                Result(
                    entity.driverNumber,
                    entity.position,
                    entity.positionText,
                    entity.points,
                    Driver(entity.driverId, entity.driverPermanentNumber, entity.driverCode, entity.driverUrl, entity.driverGivenName, entity.driverFamilyName, entity.driverDateOfBirth, entity.driverNationality),
                    Constructor(entity.constructorId, entity.constructorUrl, entity.constructorName, entity.constructorNationality),
                    entity.grid,
                    entity.laps,
                    entity.status,
                    entity.timeMillis?.let { ResultTime(it, entity.time ?: "") }, // Handle null time
                    entity.fastestLapRank?.let { FastestLap(it, entity.fastestLapLap, entity.fastestLapTime?.let{ FastestLapTime(it)}) } //Handle null fastest lap
                )
            }
            emit(Resource.Success(results)) // Emit database data
            emit(Resource.Loading(false))
            Log.d("ResultRepository", "Emitted Success from database and Loading(false)")
        }else{
            Log.d("ResultRepository", "Database is empty or query returned no results")
        }

        try {
            Log.d("ResultRepository", "Fetching from API...")
            val response = apiService.getResultsForRace(season, round)
            if(response.isSuccessful){
                val resultResponse = response.body()
                val results = resultResponse?.mrData?.raceTable?.races?.firstOrNull()?.results ?: emptyList() //Get first race
                Log.d("ResultRepository", "API call successful: ${results.size} results")

                //Convert to entity
                val resultEntities = results.map { jolpicaRaceResult ->
                    ResultEntity(
                        season = season,
                        round = round,
                        driverNumber = jolpicaRaceResult.number,
                        position = jolpicaRaceResult.position,
                        positionText = jolpicaRaceResult.positionText,
                        points = jolpicaRaceResult.points,
                        driverId = jolpicaRaceResult.driver.driverId,
                        driverPermanentNumber = jolpicaRaceResult.driver.permanentNumber,
                        driverCode = jolpicaRaceResult.driver.code,
                        driverUrl = jolpicaRaceResult.driver.url,
                        driverGivenName = jolpicaRaceResult.driver.givenName,
                        driverFamilyName = jolpicaRaceResult.driver.familyName,
                        driverDateOfBirth = jolpicaRaceResult.driver.dateOfBirth,
                        driverNationality = jolpicaRaceResult.driver.nationality,
                        constructorId = jolpicaRaceResult.constructor.constructorId,
                        constructorUrl = jolpicaRaceResult.constructor.url,
                        constructorName = jolpicaRaceResult.constructor.name,
                        constructorNationality = jolpicaRaceResult.constructor.nationality,
                        grid = jolpicaRaceResult.grid,
                        laps = jolpicaRaceResult.laps,
                        status = jolpicaRaceResult.status,
                        timeMillis = jolpicaRaceResult.time?.millis,
                        time = jolpicaRaceResult.time?.time,
                        fastestLapRank = jolpicaRaceResult.fastestLap?.rank,
                        fastestLapLap = jolpicaRaceResult.fastestLap?.lap,
                        fastestLapTime = jolpicaRaceResult.fastestLap?.time?.time
                    )
                }
                resultDao.deleteResultsByRace(season,round) //Clear old data
                Log.d("ResultRepository", "Deleted old results from database")
                resultDao.insertResults(resultEntities)
                Log.d("ResultRepository", "Inserted new results into database")

                //Map to the UI model
                val uiResults = results.map { it.toResult() }
                emit(Resource.Success(uiResults))
                Log.d("ResultRepository", "Emitted Success from API")
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ResultRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                emit(Resource.Error("Error fetching results: ${response.code()} - $errorBody"))
            }

        }  catch (e: IOException) {
            Log.e("ResultRepository", "Network error", e)
            emit(Resource.Error("Network error: ${e.localizedMessage ?: "Check your internet connection."}"))
        } catch (e: HttpException) {
            Log.e("ResultRepository", "HTTP error", e)
            emit(Resource.Error("HTTP error: ${e.localizedMessage ?: "An unexpected error occurred."}"))
        } finally {
            emit(Resource.Loading(false)) // Always emit Loading(false)
            Log.d("ResultRepository", "Emitted Loading(false) in finally block")
        }
    }.catch { e ->
        Log.e("ResultRepository", "Flow error", e)
        emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        emit(Resource.Loading(false)) // Ensure loading stops even on flow errors
    }
}

fun JolpicaRaceResult.toResult(): Result = Result(
    driverNumber = this.number,
    position = this.position,
    positionText = this.positionText,
    points = this.points,
    driver = Driver(
        driverId = this.driver.driverId,
        permanentNumber = this.driver.permanentNumber,
        code = this.driver.code,
        url = this.driver.url,
        givenName = this.driver.givenName,
        familyName = this.driver.familyName,
        dateOfBirth = this.driver.dateOfBirth,
        nationality = this.driver.nationality
    ),
    constructor = Constructor(
        constructorId = this.constructor.constructorId,
        url = this.constructor.url,
        name = this.constructor.name,
        nationality = this.constructor.nationality
    ),
    grid = this.grid,
    laps = this.laps,
    status = this.status,
    time = this.time?.let { ResultTime(it.millis, it.time) },
    fastestLap = this.fastestLap?.let { FastestLap(it.rank, it.lap, it.time?.let{ FastestLapTime(it.time)}) }
)