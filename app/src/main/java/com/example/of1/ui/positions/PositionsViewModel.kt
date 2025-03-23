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
    private val driverRepository: DriverRepository // Inject DriverRepository
) : ViewModel() {

    private val _positions = MutableStateFlow<Resource<List<Position>>>(Resource.Loading())
    val positions: StateFlow<Resource<List<Position>>> = _positions

    private val _drivers = MutableStateFlow<Resource<List<OF1Driver>>>(Resource.Loading()) //For drivers.
    val drivers: StateFlow<Resource<List<OF1Driver>>> = _drivers

    private var pollingJob: kotlinx.coroutines.Job? = null

    fun getPositions(meetingKey: Int, sessionKey: Int, isLive: Boolean) {

        viewModelScope.launch {
            if (isLive) {
                startPolling(meetingKey, sessionKey)
            } else {
                positionRepository.getPositions(meetingKey, sessionKey)
                    .onEach { _positions.value = it }
                    .launchIn(viewModelScope)
            }
        }

        // Fetch Drivers -  This happens regardless of 'isLive'
        viewModelScope.launch {
            driverRepository.getDrivers(sessionKey, meetingKey)
                .onEach { _drivers.value = it }
                .launchIn(viewModelScope)
        }
    }

    private fun startPolling(meetingKey: Int, sessionKey: Int){
        pollingJob?.cancel() // Cancel any existing polling job
        pollingJob = viewModelScope.launch {
            while (true){
                delay(Constants.POLLING_RATE) //delay first.
                positionRepository.getPositions(meetingKey, sessionKey) //Fetch data.
                    .onEach {
                        _positions.value = it
                    }.launchIn(this)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    fun stopPolling() {
        pollingJob?.cancel()
    }
}