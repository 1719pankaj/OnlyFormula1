package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.RaceDao
import com.example.of1.data.local.entity.RaceEntity
import com.example.of1.data.model.Circuit
import com.example.of1.data.model.Location
import com.example.of1.data.model.Practice
import com.example.of1.data.model.Race
import com.example.of1.data.model.jolpica.JolpicaRace
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



class RaceRepository @Inject constructor(
    private val apiService: JolpicaApiService,
    private val raceDao: RaceDao
){
    fun getRaces(season: String): Flow<Resource<List<Race>>> = flow {
        emit(Resource.Loading())
        Log.d("RaceRepository", "Initial Loading emitted")

        // 1. Emit local data if available (but don't stop the flow)
        val localData = raceDao.getRacesForSeason(season).first()
        if (localData.isNotEmpty()) {
            Log.d("RaceRepository", "Emitting initial local races")
            emit(Resource.Success(localData.map { entity ->
                Race(
                    entity.season, entity.round, entity.url, entity.raceName,
                    Circuit(entity.circuitId, entity.circuitUrl, entity.circuitName, Location(entity.circuitLat, entity.circuitLong, entity.circuitLocality, entity.circuitCountry)),
                    entity.date, entity.time,
                    entity.firstPracticeDate?.let { Practice(it, entity.firstPracticeTime) },
                    entity.secondPracticeDate?.let { Practice(it, entity.secondPracticeTime) },
                    entity.thirdPracticeDate?.let { Practice(it, entity.thirdPracticeTime) },
                    entity.qualifyingDate?.let { Practice(it, entity.qualifyingTime) },
                    entity.sprintDate?.let { Practice(it, entity.sprintTime) }
                )
            }))
            // DON'T emit Loading(false) here yet, as we still need to fetch from API
        } else {
            Log.d("RaceRepository", "No local race data found initially.")
        }

        // 2. Always try to fetch from the API
        try {
            Log.d("RaceRepository", "Fetching races from API...")
            val response = apiService.getRacesForSeason(season)
            if (response.isSuccessful) {
                val raceResponse = response.body()
                val races = raceResponse?.mrData?.raceTable?.races ?: emptyList()
                Log.d("RaceRepository", "API call successful: ${races.size} races")

                val raceEntities = races.map { jolpicaRace ->
                    RaceEntity(
                        season = jolpicaRace.season, round = jolpicaRace.round, url = jolpicaRace.url, raceName = jolpicaRace.raceName,
                        circuitId = jolpicaRace.circuit.circuitId, circuitUrl = jolpicaRace.circuit.url, circuitName = jolpicaRace.circuit.circuitName,
                        circuitLat = jolpicaRace.circuit.location.lat, circuitLong = jolpicaRace.circuit.location.long, circuitLocality = jolpicaRace.circuit.location.locality, circuitCountry = jolpicaRace.circuit.location.country,
                        date = jolpicaRace.date, time = jolpicaRace.time,
                        firstPracticeDate = jolpicaRace.firstPractice?.date, firstPracticeTime = jolpicaRace.firstPractice?.time,
                        secondPracticeDate = jolpicaRace.secondPractice?.date, secondPracticeTime = jolpicaRace.secondPractice?.time,
                        thirdPracticeDate = jolpicaRace.thirdPractice?.date, thirdPracticeTime = jolpicaRace.thirdPractice?.time,
                        qualifyingDate = jolpicaRace.qualifying?.date, qualifyingTime = jolpicaRace.qualifying?.time,
                        sprintDate = jolpicaRace.sprint?.date, sprintTime = jolpicaRace.sprint?.time
                    )
                }
                raceDao.deleteRacesBySeason(season)
                Log.d("RaceRepository", "Deleted old races from database")
                raceDao.insertRaces(raceEntities)
                Log.d("RaceRepository", "Inserted new races into database")

                val uiRaces = races.map { it.toRace() }
                emit(Resource.Success(uiRaces)) // Emit the final, updated list
                Log.d("RaceRepository", "Emitted Success from API")
            } else {
                // API failed, emit error *only if* local data wasn't shown
                if (localData.isEmpty()) {
                    val errorBody = response.errorBody()?.string()
                    Log.e("RaceRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                    emit(Resource.Error("Error fetching races: ${response.code()} - $errorBody"))
                } else {
                    Log.w("RaceRepository", "API call failed, but local data was already emitted.")
                }
            }
        } catch (e: IOException) {
            Log.e("RaceRepository", "Network error", e)
            if (localData.isEmpty()) { // Only emit error if local data wasn't shown
                emit(Resource.Error("Network error: ${e.localizedMessage ?: "Check connection"}"))
            } else {
                Log.w("RaceRepository", "Network error, but local data was already emitted.")
            }
        } catch (e: HttpException) {
            Log.e("RaceRepository", "HTTP error", e)
            if (localData.isEmpty()) { // Only emit error if local data wasn't shown
                emit(Resource.Error("HTTP error: ${e.localizedMessage ?: "Unexpected error"}"))
            } else {
                Log.w("RaceRepository", "HTTP error, but local data was already emitted.")
            }
        } finally {
            emit(Resource.Loading(false)) // Ensure loading is always turned off at the very end
            Log.d("RaceRepository", "Emitted Loading(false) in finally block")
        }

    }.catch { e ->
        Log.e("RaceRepository", "Flow error", e)
        emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        emit(Resource.Loading(false))
    }

    // Keep the extension function at the top level
    fun JolpicaRace.toRace(): Race = Race(
        season = this.season,
        round = this.round,
        url = this.url,
        raceName = this.raceName,
        circuit = Circuit(
            circuitId = this.circuit.circuitId,
            url = this.circuit.url,
            circuitName = this.circuit.circuitName,
            location = Location(
                lat = this.circuit.location.lat,
                long = this.circuit.location.long,
                locality = this.circuit.location.locality,
                country = this.circuit.location.country
            )
        ),
        date = this.date,
        time = this.time,
        firstPractice = this.firstPractice?.let { Practice(it.date, it.time) },
        secondPractice = this.secondPractice?.let { Practice(it.date, it.time) },
        thirdPractice = this.thirdPractice?.let { Practice(it.date, it.time) },
        qualifying = this.qualifying?.let { Practice(it.date, it.time) },
        sprint = this.sprint?.let { Practice(it.date, it.time) }
    )
}