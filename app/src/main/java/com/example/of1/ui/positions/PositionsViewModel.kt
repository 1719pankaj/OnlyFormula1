package com.example.of1.ui.positions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.of1.data.model.Position
import com.example.of1.data.model.openf1.OF1Driver
import com.example.of1.data.model.openf1.TeamRadio
import com.example.of1.data.repository.DriverRepository
import com.example.of1.data.repository.PositionRepository
import com.example.of1.data.repository.TeamRadioRepository
import com.example.of1.utils.Constants
import com.example.of1.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.* // Import necessary operators
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PositionsViewModel @Inject constructor(
    private val positionRepository: PositionRepository,
    private val driverRepository: DriverRepository,
    private val teamRadioRepository: TeamRadioRepository
) : ViewModel() {

    // --- Internal StateFlows ---
    private val _positions = MutableStateFlow<Resource<List<Position>>>(Resource.Loading())
    val positions: StateFlow<Resource<List<Position>>> = _positions

    private val _drivers = MutableStateFlow<Resource<List<OF1Driver>>>(Resource.Loading())
    val drivers: StateFlow<Resource<List<OF1Driver>>> = _drivers

    private val _teamRadioRaw = MutableStateFlow<Resource<List<TeamRadio>>>(Resource.Loading())

    // --- Derived StateFlow for Latest Radio ---
    private val latestTeamRadioPerDriver: StateFlow<Map<Int, TeamRadio?>> = _teamRadioRaw
        .filterIsInstance<Resource.Success<List<TeamRadio>>>() // Only process success states
        .map { successResource ->
            successResource.data // Extract data or empty list
                ?.groupBy { it.driverNumber }
                ?.mapValues { entry -> entry.value.maxByOrNull { it.date } }
                ?: emptyMap()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- Combine for UI Adapter (Robust Version) ---
    val uiPositions: StateFlow<List<UiPosition>> = combine(
        // Filter for Success and map to data or empty list
        _positions.filterIsInstance<Resource.Success<List<Position>>>().map { it.data ?: emptyList() },
        _drivers.filterIsInstance<Resource.Success<List<OF1Driver>>>().map { it.data ?: emptyList() },
        latestTeamRadioPerDriver // Already a StateFlow<Map<...>>
    ) { positionList, driverList, radioMap ->
        // Combine logic remains the same, but now only runs when positions and drivers are successful
        Log.d("PositionsViewModel", "Combining: Positions=${positionList.size}, Drivers=${driverList.size}")
        val driverMap = driverList.associateBy { it.driverNumber }
        positionList.map { position ->
            UiPosition(
                position = position,
                driver = driverMap[position.driverNumber],
                latestRadio = radioMap[position.driverNumber]
            )
        }
        // Filter out emissions if the list content hasn't changed
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Polling and Lifecycle ---
    private var positionPollingJob: kotlinx.coroutines.Job? = null
    private var teamRadioPollingJob: kotlinx.coroutines.Job? = null
    private var isLive = false
    private var currentSessionKey: Int = -1
    private var currentMeetingKey: Int = -1

    fun getPositions(meetingKey: Int, sessionKey: Int, isLive: Boolean) {
        this.isLive = isLive
        this.currentSessionKey = sessionKey
        this.currentMeetingKey = meetingKey

        // Initial fetches
        viewModelScope.launch {
            positionRepository.getPositions(meetingKey, sessionKey)
                .onEach { _positions.value = it }
                .launchIn(viewModelScope) // Use viewModelScope
        }
        viewModelScope.launch {
            driverRepository.getDrivers(sessionKey, meetingKey)
                .onEach { _drivers.value = it }
                .launchIn(viewModelScope)
        }
        viewModelScope.launch {
            teamRadioRepository.getTeamRadio(sessionKey)
                .onEach { _teamRadioRaw.value = it }
                .launchIn(viewModelScope)
        }
    }

    fun startPolling() {
        if (isLive && currentSessionKey != -1 && currentMeetingKey != -1) {
            Log.d("PositionsViewModel", "Starting polling...")
            // Start polling for Positions
            positionPollingJob?.cancel()
            positionPollingJob = viewModelScope.launch {
                while (true) {
                    delay(Constants.POLLING_RATE)
                    positionRepository.getPositions(currentMeetingKey, currentSessionKey)
                        .onEach { _positions.value = it }
                        .launchIn(this) // Scope of the while loop
                }
            }
            // Start polling for Team Radio
            teamRadioPollingJob?.cancel()
            teamRadioPollingJob = viewModelScope.launch {
                while (true) {
                    delay(Constants.POLLING_RATE) // Use same rate or different?
                    teamRadioRepository.getTeamRadio(currentSessionKey)
                        .onEach { _teamRadioRaw.value = it }
                        .launchIn(this) // Scope of the while loop
                }
            }
        } else {
            Log.d("PositionsViewModel", "Not starting polling (isLive=$isLive, sessionKey=$currentSessionKey, meetingKey=$currentMeetingKey)")
        }
    }

    fun stopPolling() {
        Log.d("PositionsViewModel", "Stopping polling...")
        positionPollingJob?.cancel()
        positionPollingJob = null
        teamRadioPollingJob?.cancel()
        teamRadioPollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}