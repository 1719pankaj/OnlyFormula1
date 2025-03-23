package com.example.of1.ui.cardata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.of1.data.model.openf1.CarData
import com.example.of1.data.repository.CarDataRepository
import com.example.of1.utils.Constants
import com.example.of1.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CarDataViewModel @Inject constructor(
    private val repository: CarDataRepository
) : ViewModel() {

    private val _carData = MutableStateFlow<Resource<List<CarData>>>(Resource.Loading()) // Use List<CarData>
    val carData: StateFlow<Resource<List<CarData>>> = _carData // Expose as StateFlow

    private var pollingJob: kotlinx.coroutines.Job? = null

    fun getCarData(sessionKey: Int, driverNumber: Int, isLive: Boolean) {
        viewModelScope.launch {
            if (isLive) {
                startPolling(sessionKey, driverNumber)
            } else {
                // Fetch once if not live
                repository.getCarData(sessionKey, driverNumber)
                    .onEach { _carData.value = it }
                    .launchIn(viewModelScope)
            }
        }
    }
    fun startPolling(sessionKey: Int, driverNumber: Int) {
        pollingJob?.cancel() // Cancel any existing polling job
        pollingJob = viewModelScope.launch {
            while (true) {
                repository.getCarData(sessionKey, driverNumber)
                    .onEach { _carData.value = it } // Update the StateFlow
                    .launchIn(this) // Use the coroutine scope of the while loop
                delay(Constants.POLLING_RATE)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()  // Cancel polling when ViewModel is cleared.
    }

    fun stopPolling() {
        pollingJob?.cancel() // Stop polling function
    }

    // Function to prepare data for the combined Throttle/Brake vs. Time chart
    fun getThrottleBrakeData(): StateFlow<Resource<List<Pair<String, Pair<Int?, Int?>>>>> {
        //Map carData into required format.
        return MutableStateFlow(carData.value.data?.map {
            Pair(it.date, Pair(it.throttle, it.brake))
        }?.let { Resource.Success(it) } ?: Resource.Loading())
    }

    // Function to prepare data for the combined RPM/Speed vs. Time chart
    fun getRpmSpeedData(): StateFlow<Resource<List<Pair<String, Pair<Int?, Int?>>>>> {
        //Map carData into required format.
        return MutableStateFlow(carData.value.data?.map {
            Pair(it.date, Pair(it.rpm, it.speed))
        }?.let { Resource.Success(it) } ?: Resource.Loading())
    }

    // Function to prepare data for Throttle/RPM/Speed vs. Time chart
    fun getCombinedData(): StateFlow<Resource<List<Pair<String, Triple<Int?, Int?, Int?>>>>> {
        //Map carData into required format.
        return MutableStateFlow(carData.value.data?.map {
            Pair(it.date, Triple(it.throttle, it.rpm, it.speed))
        }?.let { Resource.Success(it) } ?: Resource.Loading())
    }
}