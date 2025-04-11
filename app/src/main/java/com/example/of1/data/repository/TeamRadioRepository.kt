package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.TeamRadioDao
import com.example.of1.data.local.entity.TeamRadioEntity
import com.example.of1.data.model.openf1.TeamRadio
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

class TeamRadioRepository @Inject constructor(
    private val apiService: OpenF1ApiService,
    private val teamRadioDao: TeamRadioDao
) {

    fun getTeamRadio(sessionKey: Int): Flow<Resource<List<TeamRadio>>> = flow {
        emit(Resource.Loading())
        Log.d("TeamRadioRepository", "Initial Loading emitted")

        // 1. Emit local data if available
        var localDataEmitted = false
        try {
            val localEntities = teamRadioDao.getTeamRadiosBySession(sessionKey).first()
            if (localEntities.isNotEmpty()) {
                val localRadios = localEntities.map { it.toTeamRadio() }
                Log.d("TeamRadioRepository", "Emitting local radio data: ${localRadios.size}")
                emit(Resource.Success(localRadios))
                localDataEmitted = true
                // Don't stop loading yet
            } else {
                Log.d("TeamRadioRepository", "No local radio data found.")
            }
        } catch (e: Exception) {
            Log.e("TeamRadioRepository", "Error fetching local radio data", e)
        }

        // 2. Fetch from API
        try {
            val latestTimestamp = teamRadioDao.getLatestRadioTimestamp(sessionKey)
            Log.d("TeamRadioRepository", "Fetching radio from API with date > $latestTimestamp")
            val response = apiService.getTeamRadio(sessionKey, latestTimestamp)

            if (response.isSuccessful) {
                val radioResponses = response.body() ?: emptyList()
                Log.d("TeamRadioRepository", "API call successful: ${radioResponses.size} radio messages")

                val radioEntities = radioResponses.map {
                    TeamRadioEntity(
                        recordingUrl = it.recordingUrl,
                        date = it.date,
                        driverNumber = it.driverNumber,
                        meetingKey = it.meetingKey,
                        sessionKey = it.sessionKey
                    )
                }

                if (radioEntities.isNotEmpty()) {
                    teamRadioDao.insertTeamRadios(radioEntities) // Use IGNORE strategy
                    Log.d("TeamRadioRepository", "Inserted new radio messages into DB")
                }

                // Query DB again for the full list after potential inserts
                val allRadios = teamRadioDao.getTeamRadiosBySession(sessionKey).first().map { it.toTeamRadio() }
                emit(Resource.Success(allRadios))
                Log.d("TeamRadioRepository", "Emitted Success from API/DB")

            } else {
                if (!localDataEmitted) {
                    val errorBody = response.errorBody()?.string()
                    Log.e("TeamRadioRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                    emit(Resource.Error("Error fetching team radio: ${response.code()} - $errorBody"))
                } else {
                    Log.w("TeamRadioRepository", "API call failed (${response.code()}), but local data was shown.")
                }
            }
        } catch (e: IOException) {
            Log.e("TeamRadioRepository", "Network error", e)
            if (!localDataEmitted) {
                emit(Resource.Error("Network error: ${e.localizedMessage ?: "Check connection"}"))
            } else {
                Log.w("TeamRadioRepository", "Network error, but local data was shown.")
            }
        } catch (e: HttpException) {
            Log.e("TeamRadioRepository", "HTTP error", e)
            if (!localDataEmitted) {
                emit(Resource.Error("HTTP error: ${e.localizedMessage ?: "Unexpected error"}"))
            } else {
                Log.w("TeamRadioRepository", "HTTP error, but local data was shown.")
            }
        } finally {
            emit(Resource.Loading(false))
            Log.d("TeamRadioRepository", "Emitted Loading(false) in finally block")
        }

    }.catch { e ->
        Log.e("TeamRadioRepository", "Flow error", e)
        emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        emit(Resource.Loading(false))
    }

    // Helper extension function
    private fun TeamRadioEntity.toTeamRadio(): TeamRadio {
        return TeamRadio(
            date = this.date,
            driverNumber = this.driverNumber,
            meetingKey = this.meetingKey,
            recordingUrl = this.recordingUrl,
            sessionKey = this.sessionKey
        )
    }
}