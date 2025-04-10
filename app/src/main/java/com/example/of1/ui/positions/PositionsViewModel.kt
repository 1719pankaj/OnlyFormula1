package com.example.of1.ui.positions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.of1.data.model.Position
import com.example.of1.data.model.openf1.OF1Driver
import com.example.of1.data.repository.DriverRepository
import com.example.of1.data.repository.PositionRepository
import com.example.of1.utils.Constants
import com.example.of1.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PositionsViewModel @Inject constructor(
    private val positionRepository: PositionRepository,
    private val driverRepository: DriverRepository
) : ViewModel() {

    private val _positions = MutableStateFlow<Resource<List<Position>>>(Resource.Loading())
    val positions: StateFlow<Resource<List<Position>>> = _positions

    private val _drivers = MutableStateFlow<Resource<List<OF1Driver>>>(Resource.Loading())
    val drivers: StateFlow<Resource<List<OF1Driver>>> = _drivers

    private var pollingJob: kotlinx.coroutines.Job? = null
    private var isLive = false // Add isLive property
    private var currentSessionKey: Int = -1 // Add, initialize to an invalid value
    private var currentMeetingKey: Int = -1


    // Modified getPositions:  Takes isLive as a parameter, but doesn't start polling immediately
    fun getPositions(meetingKey: Int, sessionKey: Int, isLive: Boolean) {
        this.isLive = isLive // Store isLive
        this.currentSessionKey = sessionKey //store
        this.currentMeetingKey = meetingKey
        viewModelScope.launch {
            positionRepository.getPositions(meetingKey, sessionKey)
                .onEach { _positions.value = it }
                .launchIn(viewModelScope)
        }
        // Fetch Drivers.
        viewModelScope.launch {
            driverRepository.getDrivers(sessionKey, meetingKey)
                .onEach {  _drivers.value = it }
                .launchIn(viewModelScope)
        }
    }

    // Call startPolling *separately* from getPositions.
    fun startPolling() {
        if (isLive && currentSessionKey != -1 && currentMeetingKey !=-1) { // Only start if isLive is true, and we have valid session Key.
            pollingJob?.cancel() // Cancel any existing polling job
            pollingJob = viewModelScope.launch {
                while (true) {
                    delay(Constants.POLLING_RATE)
                    positionRepository.getPositions(currentMeetingKey, currentSessionKey)
                        .onEach { _positions.value = it }
                        .launchIn(this) // Important: Use 'this' (coroutine scope)
                }
            }
        }
    }


    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null // Set to null after canceling
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling() // VERY IMPORTANT. Cancel when ViewModel is cleared
    }

}