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

    // Flow for ALL radio messages in the session (fetched once)
    private val _teamRadioRaw = MutableStateFlow<Resource<List<TeamRadio>>>(Resource.Loading())
    // No need to expose _teamRadioRaw publicly

    // Polling jobs
    private var lapsPollingJob: kotlinx.coroutines.Job? = null
    private var pitStopsPollingJob: kotlinx.coroutines.Job? = null
    private var teamRadioPollingJob: kotlinx.coroutines.Job? = null

    // Properties to store context
    private var isLive = false
    private var currentSessionKey: Int = -1
    private var currentDriverNumber: Int = -1

    fun getLaps(sessionKey: Int, driverNumber: Int, isLive: Boolean) {
        this.isLive = isLive
        this.currentSessionKey = sessionKey
        this.currentDriverNumber = driverNumber

        // Initial fetches
        viewModelScope.launch {
            lapRepository.getLaps(sessionKey, driverNumber)
                .onEach { _laps.value = it }
                .launchIn(viewModelScope) // Use viewModelScope
        }
        viewModelScope.launch {
            pitStopRepository.getPitStops(sessionKey, driverNumber)
                .onEach { _pitStops.value = it }
                .launchIn(viewModelScope) // Use viewModelScope
        }
        // **FETCH TEAM RADIO HERE**
        viewModelScope.launch {
            teamRadioRepository.getTeamRadio(sessionKey) // Fetch for the whole session
                .onEach { _teamRadioRaw.value = it }
                .launchIn(viewModelScope) // Use viewModelScope
        }
    }

    // Separate startPolling function
    fun startPolling() {
        if (isLive && currentSessionKey != -1 && currentDriverNumber != -1) {
            Log.d("LapsViewModel", "Starting polling...")
            lapsPollingJob?.cancel()
            lapsPollingJob = viewModelScope.launch {
                while (true) {
                    delay(Constants.POLLING_RATE)
                    lapRepository.getLaps(currentSessionKey, currentDriverNumber)
                        .onEach { _laps.value = it }
                        .launchIn(this) // Use the coroutine scope of startPolling
                }
            }
            pitStopsPollingJob?.cancel()
            pitStopsPollingJob = viewModelScope.launch {
                while (true) {
                    delay(Constants.POLLING_RATE)
                    pitStopRepository.getPitStops(currentSessionKey, currentDriverNumber)
                        .onEach { _pitStops.value = it }
                        .launchIn(this)
                }
            }
            // Poll Team Radio
            teamRadioPollingJob?.cancel()
            teamRadioPollingJob = viewModelScope.launch {
                while (true) {
                    delay(Constants.POLLING_RATE) // Or maybe a different rate?
                    teamRadioRepository.getTeamRadio(currentSessionKey) // Poll for the whole session
                        .onEach { _teamRadioRaw.value = it }
                        .launchIn(this)
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

    // Add a local cache of the last valid data
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