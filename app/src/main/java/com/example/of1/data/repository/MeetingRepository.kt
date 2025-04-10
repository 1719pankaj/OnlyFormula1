package com.example.of1.data.repository

import android.util.Log
import com.example.of1.data.local.dao.MeetingDao
import com.example.of1.data.local.entity.MeetingEntity
import com.example.of1.data.model.Meeting
import com.example.of1.data.remote.OpenF1ApiService
import com.example.of1.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class MeetingRepository @Inject constructor(
    private val apiService: OpenF1ApiService,
    private val meetingDao: MeetingDao
) {
    fun getMeetings(year: Int): Flow<Resource<List<Meeting>>> = flow {
        emit(Resource.Loading())
        Log.d("MeetingRepository", "Initial Loading emitted")

        // Fetch from local DB and emit *immediately*
        meetingDao.getMeetingsByYear(year)
            .onStart {  // Emit local data *before* fetching from API
                val localData = meetingDao.getMeetingsByYear(year).first()
                if (localData.isNotEmpty()) {
                    Log.d("MeetingRepository", "Emitting local meetings")
                    emit(Resource.Success(localData.map { it.toMeeting() }))
                    emit(Resource.Loading(false)) // Turn off loading *after* emitting local data
                }
            }
            .map { entities ->
                // Map entities to Meeting objects *inside* the flow
                Resource.Success(entities.map { it.toMeeting() }) // Wrap in Resource.Success
            }
            .catch { e -> // Catch errors within the flow
                emit(Resource.Error<List<Meeting>>("Local DB error: ${e.localizedMessage ?: "Unknown error"}"))
            }
            .collect {
                emit(it) // Forward local data and loading state
                if (it is Resource.Success){
                    emit(Resource.Loading(false))
                }
            }


        try {
            Log.d("MeetingRepository", "Fetching from API...")
            val response = apiService.getMeetings(year)
            if(response.isSuccessful) {
                val meetings = response.body() ?: emptyList()
                Log.d("MeetingRepository", "API call successful: ${meetings.size} meetings")

                // Convert to entity and store in the database
                val meetingEntities = meetings.map { meeting ->
                    MeetingEntity(
                        meetingKey = meeting.meetingKey,
                        circuitKey = meeting.circuitKey,
                        circuitShortName = meeting.circuitShortName,
                        countryCode = meeting.countryCode,
                        countryKey = meeting.countryKey,
                        countryName = meeting.countryName,
                        dateStart = meeting.dateStart,
                        gmtOffset = meeting.gmtOffset,
                        location = meeting.location,
                        meetingName = meeting.meetingName,
                        meetingOfficialName = meeting.meetingOfficialName,
                        year = meeting.year
                    )
                }

                meetingDao.deleteMeetingsByYear(year) // Delete old data for the year
                Log.d("MeetingRepository", "Deleted old meetings from database for year $year")
                meetingDao.insertMeetings(meetingEntities) // Insert new data
                Log.d("MeetingRepository", "Inserted new meetings into database")

                emit(Resource.Success(meetings)) // Emit API data
                Log.d("MeetingRepository", "Emitted Success from API")

            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("MeetingRepository", "API call failed: ${response.code()}, errorBody: $errorBody")
                emit(Resource.Error("Error fetching meetings: ${response.code()} - $errorBody"))
            }

        } catch (e: IOException) {
            Log.e("MeetingRepository", "Network error", e)
            emit(Resource.Error("Network error: ${e.localizedMessage ?: "Check your internet connection."}"))
        } catch (e: HttpException) {
            Log.e("MeetingRepository", "HTTP error", e)
            emit(Resource.Error("HTTP error: ${e.localizedMessage ?: "An unexpected error occurred."}"))
        }
    }.catch { e -> //Catching any errors during flow collection.
        Log.e("MeetingRepository", "Flow error", e)
        emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
    }

    // Extension function to convert Entity to Model
    private fun MeetingEntity.toMeeting(): Meeting {
        return Meeting(
            circuitKey = this.circuitKey,
            circuitShortName = this.circuitShortName,
            countryCode = this.countryCode,
            countryKey = this.countryKey,
            countryName = this.countryName,
            dateStart = this.dateStart,
            gmtOffset = this.gmtOffset,
            location = this.location,
            meetingKey = this.meetingKey,
            meetingName = this.meetingName,
            meetingOfficialName = this.meetingOfficialName,
            year = this.year
        )
    }
}