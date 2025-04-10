package com.example.of1.ui.laps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.of1.data.model.openf1.Lap
import com.example.of1.data.model.openf1.PitStop
import com.example.of1.data.repository.LapRepository
import com.example.of1.data.repository.PitStopRepository
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
class LapsViewModel @Inject constructor(
    private val lapRepository: LapRepository,
    private val pitStopRepository: PitStopRepository
) : ViewModel() {

    private val _laps = MutableStateFlow<Resource<List<Lap>>>(Resource.Loading())
    val laps: StateFlow<Resource<List<Lap>>> = _laps

    private val _pitStops = MutableStateFlow<Resource<List<PitStop>>>(Resource.Loading())
    val pitStops: StateFlow<Resource<List<PitStop>>> = _pitStops

    private var lapsPollingJob: kotlinx.coroutines.Job? = null
    private var pitStopsPollingJob: kotlinx.coroutines.Job? = null

    // Add isLive and session/driver tracking
    private var isLive = false
    private var currentSessionKey: Int = -1
    private var currentDriverNumber: Int = -1

    fun getLaps(sessionKey: Int, driverNumber: Int, isLive: Boolean) {
        this.isLive = isLive // Store isLive
        this.currentSessionKey = sessionKey
        this.currentDriverNumber = driverNumber

        viewModelScope.launch {
            lapRepository.getLaps(sessionKey, driverNumber)
                .onEach { _laps.value = it }
                .launchIn(this) // Use 'this' for the coroutine scope

            pitStopRepository.getPitStops(sessionKey, driverNumber)
                .onEach { _pitStops.value = it }
                .launchIn(this)
        }
    }

    // Call startPolling *separately*
    fun startPolling() {
        if (isLive && currentSessionKey != -1 && currentDriverNumber != -1) { // Only start if live
            lapsPollingJob?.cancel()
            lapsPollingJob = viewModelScope.launch {
                while (true) {
                    delay(Constants.POLLING_RATE)
                    lapRepository.getLaps(currentSessionKey, currentDriverNumber) // Use stored values
                        .onEach { _laps.value = it }
                        .launchIn(this) // Important: Use 'this' for coroutine scope
                }
            }
            pitStopsPollingJob?.cancel()
            pitStopsPollingJob = viewModelScope.launch {
                while(true){
                    delay(Constants.POLLING_RATE)
                    pitStopRepository.getPitStops(currentSessionKey, currentDriverNumber)
                        .onEach { _pitStops.value = it }
                        .launchIn(this)
                }
            }
        }
    }

    fun stopPolling() {
        lapsPollingJob?.cancel()
        lapsPollingJob = null // Set to null after canceling
        pitStopsPollingJob?.cancel()
        pitStopsPollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling() // Cancel polling when ViewModel is cleared
    }
}