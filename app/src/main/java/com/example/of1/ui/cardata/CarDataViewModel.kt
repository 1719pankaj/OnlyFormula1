package com.example.of1.ui.cardata

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.of1.data.model.openf1.CarData
import com.example.of1.data.repository.CarDataRepository
import com.example.of1.utils.Constants
import com.example.of1.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class CarDataViewModel @Inject constructor(
    private val repository: CarDataRepository
) : ViewModel() {

    private val _carData = MutableStateFlow<Resource<List<CarData>>>(Resource.Loading())
    val carData: StateFlow<Resource<List<CarData>>> = _carData

    private var pollingJob: kotlinx.coroutines.Job? = null
    private var isLive = false
    private var currentSessionKey: Int = -1
    private var currentDriverNumber: Int = -1

    fun getCarData(sessionKey: Int, driverNumber: Int, startDate: String? = null, endDate: String? = null, isLive: Boolean = false) {
        this.isLive = isLive
        this.currentSessionKey = sessionKey
        this.currentDriverNumber = driverNumber
        viewModelScope.launch {
            repository.getCarData(sessionKey, driverNumber, startDate, endDate)
                .onEach { _carData.value = it }
                .launchIn(viewModelScope) // Use viewModelScope

        }
    }

    // Start polling *separately*, only if isLive is true
    fun startPolling() {
        if (isLive && currentSessionKey != -1 && currentDriverNumber != -1) { // Add isLive check
            pollingJob?.cancel() // Cancel any existing job
            pollingJob = viewModelScope.launch {
                while (true) {
                    delay(Constants.POLLING_RATE)
                    repository.getCarData(currentSessionKey, currentDriverNumber, null, null) // Pass parameters
                        .onEach { _carData.value = it }
                        .launchIn(this) // Use the coroutine scope of startPolling
                }
            }
        }
    }
    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    // --- Chart Data Transformations (Corrected and Simplified) ---

    // Throttle and Brake vs. Time
    val throttleBrakeData: StateFlow<Resource<List<Pair<Float, Pair<Float?, Float?>>>>> = _carData.map { resource ->
        when (resource) {
            is Resource.Success -> {
                // Get the first timestamp (if data exists), and normalize all times relative to it.
                val firstTimestamp = resource.data?.firstOrNull()?.date?.let { parseDateToMillis(it) } ?: 0L

                val transformedData = resource.data?.mapNotNull { carData ->
                    val time = (parseDateToMillis(carData.date) - firstTimestamp).toFloat() // Normalize time
                    val throttle = carData.throttle?.toFloat()
                    val brake = carData.brake?.toFloat()
                    // Only include data points where both throttle and brake are available
                    if (throttle != null && brake != null) {
                        Pair(time, Pair(throttle, brake))
                    } else {
                        null // Skip this data point
                    }
                } ?: emptyList()
                Resource.Success(transformedData)
            }
            is Resource.Error -> Resource.Error(resource.message ?: "An error occurred")
            is Resource.Loading -> Resource.Loading() // Use parameterized Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading<List<Pair<Float, Pair<Float?, Float?>>>>()) //Initial state.


    // RPM and Speed vs. Time, similarly:
    val rpmSpeedData: StateFlow<Resource<List<Pair<Float, Pair<Float?, Float?>>>>> = _carData.map { resource ->
        when (resource) {
            is Resource.Success -> {
                val firstTimestamp = resource.data?.firstOrNull()?.date?.let { parseDateToMillis(it) } ?: 0L
                val transformedData = resource.data?.mapNotNull { carData ->
                    val time = (parseDateToMillis(carData.date) - firstTimestamp).toFloat() // Normalize!
                    val rpm = carData.rpm?.toFloat()
                    val speed = carData.speed?.toFloat()
                    if (rpm != null && speed != null) {
                        Pair(time, Pair(rpm, speed))
                    } else {
                        null
                    }
                } ?: emptyList()
                Resource.Success(transformedData)
            }
            is Resource.Error -> Resource.Error(resource.message ?: "An error occurred")
            is Resource.Loading -> Resource.Loading() // Use parameterized Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading<List<Pair<Float, Pair<Float?, Float?>>>>())



    val combinedData: StateFlow<Resource<List<Pair<Float, Triple<Float?, Float?, Float?>>>>> = _carData.map { resource ->
        when (resource) {
            is Resource.Success -> {
                val firstTimestamp = resource.data?.firstOrNull()?.date?.let { parseDateToMillis(it) } ?: 0L
                val transformedData = resource.data?.mapNotNull {carData ->
                    val time = (parseDateToMillis(carData.date) - firstTimestamp).toFloat() //Normalize time.
                    val throttle = carData.throttle?.toFloat()
                    val rpm = carData.rpm?.toFloat()
                    val speed = carData.speed?.toFloat()
                    if (throttle != null && rpm != null && speed != null) {
                        Pair(time, Triple(throttle, rpm, speed))
                    } else {
                        null // Skip this data point if any value is missing
                    }
                } ?: emptyList()
                Resource.Success(transformedData)
            }
            is Resource.Error -> Resource.Error(resource.message ?: "An error occurred")
            is Resource.Loading -> Resource.Loading() // Use parameterized Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading<List<Pair<Float, Triple<Float?, Float?, Float?>>>>()) // Initial value


    private fun parseDateToMillis(dateString: String): Long {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault()) // Correct format
        format.timeZone = TimeZone.getTimeZone("UTC") // Set correct timezone
        return try {
            // Remove the extra '000' before parsing
            val adjustedDateString = dateString.replace("000+00:00", "+00:00")
            format.parse(adjustedDateString)?.time ?: 0L
        } catch (e: Exception) {
            Log.e("CarDataFragment", "Error parsing date: $dateString", e)
            0L
        }
    }
}