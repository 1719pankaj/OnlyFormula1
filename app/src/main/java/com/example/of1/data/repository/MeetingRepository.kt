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

        val localFlow = meetingDao.getMeetingsByYear(year)
        val firstLocalData = localFlow.first()

        if(firstLocalData.isNotEmpty()) {
            Log.d("MeetingRepository", "Database returned data: ${firstLocalData.size} items")
            val localMeetings = firstLocalData.map { entity ->
                Meeting(
                    entity.circuitKey, entity.circuitShortName, entity.countryCode,
                    entity.countryKey, entity.countryName, entity.dateStart,
                    entity.gmtOffset, entity.location, entity.meetingKey,
                    entity.meetingName, entity.meetingOfficialName, entity.year
                )
            }
            emit(Resource.Success(localMeetings))
            emit(Resource.Loading(false))
            Log.d("MeetingRepository", "Emitted Success from database and Loading(false)")
        } else {
            Log.d("MeetingRepository", "Database is empty or query returned no results")
        }

        try {
            Log.d("MeetingRepository", "Fetching from API...")
            val response = apiService.getMeetings(year)
            if(response.isSuccessful) {
                val meetings = response.body() ?: emptyList()
                Log.d("MeetingRepository", "API call successful: ${meetings.size} meetings")
                val meetingEntities = meetings.map { meeting ->
                    MeetingEntity(
                        meeting.meetingKey, meeting.circuitKey, meeting.circuitShortName,
                        meeting.countryCode, meeting.countryKey, meeting.countryName,
                        meeting.dateStart, meeting.gmtOffset, meeting.location,
                        meeting.meetingName, meeting.meetingOfficialName, meeting.year
                    )
                }

                meetingDao.deleteMeetingsByYear(year)
                Log.d("MeetingRepository", "Deleted old meetings from database for year $year")
                meetingDao.insertMeetings(meetingEntities)
                Log.d("MeetingRepository", "Inserted new meetings into database")

                emit(Resource.Success(meetings))
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
        } finally {
            emit(Resource.Loading(false))
            Log.d("MeetingRepository", "Emitted Loading(false) in finally block")
        }
    }.catch { e ->
        Log.e("MeetingRepository", "Flow error", e)
        emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        emit(Resource.Loading(false)) // Ensure loading stops even on flow errors
    }
}