package com.example.of1.ui.laps

import android.util.Log // Import Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.of1.data.model.openf1.Lap
import com.example.of1.data.model.openf1.PitStop
import com.example.of1.data.model.openf1.TeamRadio
import com.example.of1.data.repository.LapRepository
import com.example.of1.data.repository.PitStopRepository
import com.example.of1.data.repository.TeamRadioRepository
import com.example.of1.utils.Constants
import com.example.of1.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.* // Import operators
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LapsViewModel @Inject constructor(
    private val lapRepository: LapRepository,
    private val pitStopRepository: PitStopRepository,
    private val teamRadioRepository: TeamRadioRepository // Inject
) : ViewModel() {

    private val _laps = MutableStateFlow<Resource<List<Lap>>>(Resource.Loading())
    val laps: StateFlow<Resource<List<Lap>>> = _laps

    private val _pitStops = MutableStateFlow<Resource<List<PitStop>>>(Resource.Loading())
    val pitStops: StateFlow<Resource<List<PitStop>>> = _pitStops

    private val _teamRadioRaw = MutableStateFlow<Resource<List<TeamRadio>>>(Resource.Loading())

    private var lapsPollingJob: kotlinx.coroutines.Job? = null
    private var pitStopsPollingJob: kotlinx.coroutines.Job? = null
    private var teamRadioPollingJob: kotlinx.coroutines.Job? = null

    private var isLive = false
    private var currentSessionKey: Int = -1
    private var currentDriverNumber: Int = -1

    // RENAME function to initializeData for consistency
    fun initializeData(sessionKey: Int, driverNumber: Int, isLive: Boolean) {
        // Prevent re-initialization if same session/driver and already loading/loaded
        if (this.currentSessionKey == sessionKey && this.currentDriverNumber == driverNumber && _laps.value !is Resource.Loading) {
            Log.d("LapsViewModel", "Data for session $sessionKey, driver $driverNumber already loaded.")
            // Update live status if changed
            if (this.isLive != isLive) {
                this.isLive = isLive
                stopPolling()
                if (isLive) startPolling()
            }
            return
        }
        Log.d("LapsViewModel", "Initializing for session $sessionKey, driver $driverNumber, isLive: $isLive")

        this.isLive = isLive
        this.currentSessionKey = sessionKey
        this.currentDriverNumber = driverNumber

        // Reset state
        stopPolling()
        _laps.value = Resource.Loading()
        _pitStops.value = Resource.Loading()
        _teamRadioRaw.value = Resource.Loading()

        // Initial fetches
        viewModelScope.launch {
            // Use the RENAMED function here
            lapRepository.getLapsForDriver(sessionKey, driverNumber)
                .collect { _laps.value = it } // Use collect instead of onEach/launchIn for clarity
        }
        viewModelScope.launch {
            pitStopRepository.getPitStops(sessionKey, driverNumber)
                .collect { _pitStops.value = it }
        }
        viewModelScope.launch {
            teamRadioRepository.getTeamRadio(sessionKey) // Fetch for the whole session
                .collect { _teamRadioRaw.value = it }
        }

        // Start polling if live
        if (isLive) {
            startPolling()
        }
    }

    fun startPolling() {
        if (isLive && currentSessionKey != -1 && currentDriverNumber != -1) {
            Log.d("LapsViewModel", "Starting polling...")
            // Lap Polling
            if (lapsPollingJob?.isActive != true) {
                lapsPollingJob = viewModelScope.launch {
                    while (true) {
                        delay(Constants.POLLING_RATE)
                        // Use the RENAMED function here
                        lapRepository.getLapsForDriver(currentSessionKey, currentDriverNumber)
                            .collect { _laps.value = it } // Collect directly in polling loop
                    }
                }
            }
            // PitStop Polling
            if (pitStopsPollingJob?.isActive != true) {
                pitStopsPollingJob = viewModelScope.launch {
                    while (true) {
                        delay(Constants.POLLING_RATE)
                        pitStopRepository.getPitStops(currentSessionKey, currentDriverNumber)
                            .collect { _pitStops.value = it }
                    }
                }
            }
            // Team Radio Polling
            if (teamRadioPollingJob?.isActive != true) {
                teamRadioPollingJob = viewModelScope.launch {
                    while (true) {
                        delay(Constants.POLLING_RATE)
                        teamRadioRepository.getTeamRadio(currentSessionKey)
                            .collect { _teamRadioRaw.value = it }
                    }
                }
            }
        } else {
            Log.d("LapsViewModel", "Not starting polling (isLive=$isLive, sessionKey=$currentSessionKey, driverNumber=$currentDriverNumber)")
        }
    }

    fun stopPolling() {
        Log.d("LapsViewModel", "Stopping polling...")
        lapsPollingJob?.cancel(); lapsPollingJob = null
        pitStopsPollingJob?.cancel(); pitStopsPollingJob = null
        teamRadioPollingJob?.cancel(); teamRadioPollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }

    // --- Team Radio Filtering Logic (Unchanged) ---
    private var lastValidTeamRadioList: List<TeamRadio> = emptyList()

    val teamRadioForDriverList: StateFlow<List<TeamRadio>> = _teamRadioRaw
        .map { resource ->
            when (resource) {
                is Resource.Success -> {
                    val rawCount = resource.data?.size ?: 0
                    val filtered = resource.data?.filter { it.driverNumber == currentDriverNumber } ?: emptyList()
                    Log.d("LapsViewModel", "Team radio filter: $rawCount total messages, ${filtered.size} for driver $currentDriverNumber")
                    lastValidTeamRadioList = filtered // Cache the valid data
                    filtered
                }
                is Resource.Loading -> {
                    // Return cached data on Loading
                    Log.d("LapsViewModel", "Team radio loading, returning ${lastValidTeamRadioList.size} cached messages")
                    lastValidTeamRadioList
                }
                else -> {
                    // For error states, could choose to keep or clear cache
                    Log.d("LapsViewModel", "Team radio resource error: ${resource.javaClass.simpleName}")
                    lastValidTeamRadioList // Keep showing last valid data even on error
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}