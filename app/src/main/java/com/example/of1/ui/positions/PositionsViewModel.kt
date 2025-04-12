package com.example.of1.ui.positions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.of1.data.model.Position
import com.example.of1.data.model.openf1.OF1Driver
import com.example.of1.data.model.openf1.OpenF1IntervalResponse
import com.example.of1.data.model.openf1.OpenF1RaceControlResponse // Import RC Response
import com.example.of1.data.model.openf1.TeamRadio
import com.example.of1.data.repository.DriverRepository
import com.example.of1.data.repository.IntervalRepository
import com.example.of1.data.repository.PositionRepository
import com.example.of1.data.repository.RaceControlRepository // Import RC Repo
import com.example.of1.data.repository.TeamRadioRepository
import com.example.of1.utils.Constants
import com.example.of1.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job // Import Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PositionsViewModel @Inject constructor(
    private val positionRepository: PositionRepository,
    private val driverRepository: DriverRepository,
    private val teamRadioRepository: TeamRadioRepository,
    private val intervalRepository: IntervalRepository,
    private val raceControlRepository: RaceControlRepository // Inject RC Repo
) : ViewModel() {

    // --- Internal StateFlows for data sources ---
    private val _positions = MutableStateFlow<Resource<List<Position>>>(Resource.Loading())
    val positions: StateFlow<Resource<List<Position>>> = _positions

    private val _drivers = MutableStateFlow<Resource<List<OF1Driver>>>(Resource.Loading())
    val drivers: StateFlow<Resource<List<OF1Driver>>> = _drivers

    private val _teamRadioRaw = MutableStateFlow<Resource<List<TeamRadio>>>(Resource.Loading())
    private val _intervals = MutableStateFlow<Resource<Map<Int, OpenF1IntervalResponse>>>(Resource.Loading())
    val intervals: StateFlow<Resource<Map<Int, OpenF1IntervalResponse>>> = _intervals

    // --- StateFlow for Race Control UI Messages ---
    private val _displayedRaceControlMessages = MutableStateFlow<List<UiRaceControlMessage>>(emptyList())
    val displayedRaceControlMessages: StateFlow<List<UiRaceControlMessage>> = _displayedRaceControlMessages

    // --- Derived StateFlow for Latest Radio ---
    private val latestTeamRadioPerDriver: StateFlow<Map<Int, TeamRadio?>> = _teamRadioRaw
        .filterIsInstance<Resource.Success<List<TeamRadio>>>()
        .map { successResource ->
            successResource.data
                ?.groupBy { it.driverNumber }
                ?.mapValues { entry -> entry.value.maxByOrNull { it.date } }
                ?: emptyMap()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- Combine for UI Adapter (Positions - unchanged) ---
    val uiPositions: StateFlow<List<UiPosition>> = combine(
        _positions.filterIsInstance<Resource.Success<List<Position>>>().map { it.data ?: emptyList() },
        _drivers.filterIsInstance<Resource.Success<List<OF1Driver>>>().map { it.data ?: emptyList() },
        latestTeamRadioPerDriver,
        _intervals.filterIsInstance<Resource.Success<Map<Int, OpenF1IntervalResponse>>>().map { it.data ?: emptyMap() }
    ) { positionList, driverList, radioMap, intervalMap ->
        // ... (mapping logic remains the same) ...
        Log.d("PositionsViewModel", "Combining: Positions=${positionList.size}, Drivers=${driverList.size}, Intervals=${intervalMap.size}, Radio=${radioMap.size}")
        val driverMap = driverList.associateBy { it.driverNumber }
        positionList.map { position ->
            UiPosition(
                position = position,
                driver = driverMap[position.driverNumber],
                latestRadio = radioMap[position.driverNumber],
                intervalData = intervalMap[position.driverNumber]
            )
        }
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- Polling and Lifecycle ---
    private var positionPollingJob: Job? = null
    private var teamRadioPollingJob: Job? = null
    private var intervalPollingJob: Job? = null
    private var raceControlPollingJob: Job? = null // Job for RC polling
    private var messageDismissalJob: Job? = null // Job for dismissing RC messages
    private var isLive = false
    private var currentSessionKey: Int = -1
    private var currentMeetingKey: Int = -1

    private val RC_MESSAGE_DURATION_MS = 25000L // 25 seconds

    fun getPositions(meetingKey: Int, sessionKey: Int, isLive: Boolean) {
        // Only proceed if not already initialized or if session key changes
        if (this.currentSessionKey == sessionKey) return
        Log.d("PositionsViewModel", "Initializing for session $sessionKey, isLive: $isLive")

        this.isLive = isLive
        this.currentSessionKey = sessionKey
        this.currentMeetingKey = meetingKey

        // Reset state for the new session
        stopPolling() // Stop any previous polling
        _positions.value = Resource.Loading()
        _drivers.value = Resource.Loading()
        _teamRadioRaw.value = Resource.Loading()
        _intervals.value = Resource.Loading()
        _displayedRaceControlMessages.value = emptyList() // Clear RC messages
        raceControlRepository.clearTimestampCache(sessionKey) // Clear timestamp for this session

        // Initial fetches
        viewModelScope.launch {
            positionRepository.getPositions(meetingKey, sessionKey)
                .onEach { _positions.value = it }
                .launchIn(viewModelScope)
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
        viewModelScope.launch {
            // Pass isLive to interval repository
            intervalRepository.getLatestIntervals(sessionKey, isLive)
                .onEach { _intervals.value = it }
                .launchIn(viewModelScope)
        }

        // Start polling ONLY if live
        if (isLive) {
            startPolling()
        } else {
            Log.d("PositionsViewModel", "Session $sessionKey is not live, not starting polling.")
        }
    }

    fun startPolling() {
        // Ensure this is only called when isLive is true and keys are set
        if (!isLive || currentSessionKey == -1 || currentMeetingKey == -1) {
            Log.w("PositionsViewModel", "Attempted to start polling when not live or not initialized.")
            return
        }

        // --- Start Position, Interval, Team Radio Polling (if not already running) ---
        if (positionPollingJob == null || !positionPollingJob!!.isActive) {
            positionPollingJob = viewModelScope.launch {
                while (true) {
                    delay(Constants.POLLING_RATE)
                    positionRepository.getPositions(currentMeetingKey, currentSessionKey).launchIn(this) // No need for .onEach here
                }
            }
        }
        if (intervalPollingJob == null || !intervalPollingJob!!.isActive) {
            intervalPollingJob = viewModelScope.launch {
                while(true) {
                    delay(Constants.POLLING_RATE)
                    intervalRepository.getLatestIntervals(currentSessionKey, true).launchIn(this) // Pass true for isLive
                }
            }
        }
        if (teamRadioPollingJob == null || !teamRadioPollingJob!!.isActive) {
            teamRadioPollingJob = viewModelScope.launch {
                while (true) {
                    delay(Constants.POLLING_RATE)
                    teamRadioRepository.getTeamRadio(currentSessionKey).launchIn(this)
                }
            }
        }

        // --- Start Race Control Polling and Processing ---
        if (raceControlPollingJob == null || !raceControlPollingJob!!.isActive) {
            raceControlPollingJob = viewModelScope.launch {
                raceControlRepository.getMessages(currentSessionKey)
                    .onEach { resource ->
                        if (resource is Resource.Success) {
                            processNewRaceControlMessages(resource.data ?: emptyList())
                        } else if (resource is Resource.Error) {
                            Log.e("PositionsViewModel", "Error polling race control: ${resource.message}")
                            // Optionally show error via a separate state flow?
                        }
                    }
                    .launchIn(this) // Collect within the polling job's scope

                // Add the polling delay *after* the collection setup
                while (true) {
                    delay(Constants.POLLING_RATE) // Poll interval
                    // Re-trigger the flow collection by re-launching the repository call
                    raceControlRepository.getMessages(currentSessionKey).launchIn(this) // Re-fetch
                }
            }
        }


        // --- Start Message Dismissal Job ---
        if (messageDismissalJob == null || !messageDismissalJob!!.isActive) {
            messageDismissalJob = viewModelScope.launch {
                while (true) {
                    delay(1000) // Check every second
                    dismissExpiredMessages()
                }
            }
        }
        Log.d("PositionsViewModel", "Polling started/resumed for session $currentSessionKey.")
    }

    // --- Message Processing Logic ---
    private fun processNewRaceControlMessages(newApiMessages: List<OpenF1RaceControlResponse>) {
        if (newApiMessages.isEmpty()) return

        val currentTime = System.currentTimeMillis()
        val currentDisplayed = _displayedRaceControlMessages.value.toMutableList()
        var listChanged = false

        // 1. Check for Clearing Messages and update persistent ones
        val clearingFlags = listOf("GREEN", "CLEAR") // Add other potential clearing flags/messages?
        val isClearingMessageReceived = newApiMessages.any { msg ->
            (msg.category == "Flag" && clearingFlags.contains(msg.flag?.uppercase())) ||
                    msg.message?.contains("TRACK CLEAR", ignoreCase = true) == true ||
                    msg.message?.contains("SAFETY CAR IN THIS LAP", ignoreCase = true) == true ||
                    msg.message?.contains("VSC ENDING", ignoreCase = true) == true
        }

        if (isClearingMessageReceived) {
            currentDisplayed.forEachIndexed { index, msg ->
                if (msg.isPersistent && msg.displayUntilMillis == null) {
                    // Set expiry for persistent messages
                    currentDisplayed[index] = msg.copy(displayUntilMillis = currentTime + RC_MESSAGE_DURATION_MS)
                    listChanged = true
                    Log.d("PositionsViewModel", "Setting expiry for persistent message: ${msg.message}")
                }
            }
        }

        // 2. Add New Messages
        newApiMessages.forEach { apiMsg ->
            val id = apiMsg.date // Use date as unique ID
            // Avoid adding duplicates if polling fetches overlapping data
            if (currentDisplayed.none { it.id == id }) {
                val messageText = apiMsg.message ?: apiMsg.category ?: "Unknown Event" // Fallback message
                val isPersistent = determinePersistence(apiMsg)
                val displayUntil = if (isPersistent) null else currentTime + RC_MESSAGE_DURATION_MS

                val uiMsg = UiRaceControlMessage(
                    id = id,
                    message = messageText,
                    category = apiMsg.category,
                    flag = apiMsg.flag,
                    scope = apiMsg.scope,
                    isPersistent = isPersistent,
                    displayUntilMillis = displayUntil,
                    creationTimeMillis = currentTime
                )
                currentDisplayed.add(uiMsg)
                listChanged = true
                Log.d("PositionsViewModel", "Adding new RC message: ${uiMsg.message} (Persistent: $isPersistent)")
            }
        }

        // 3. Update StateFlow if changes occurred
        if (listChanged) {
            // Sort by creation time before updating state? Or keep API order? Keep API order for now.
            _displayedRaceControlMessages.value = currentDisplayed
        }
    }

    // --- Helper to determine message persistence ---
    private fun determinePersistence(msg: OpenF1RaceControlResponse): Boolean {
        return when (msg.category?.uppercase()) {
            "FLAG" -> msg.flag?.uppercase() in listOf("RED", "YELLOW", "BLACK AND WHITE") // Keep B/W flag persistent? Maybe not. Let's stick to Yellow/Red. Revisit if needed.
            "SAFETYCAR" -> msg.message?.contains("DEPLOYED", ignoreCase = true) == true
            "VIRTUALSAFETYCAR" -> msg.message?.contains("DEPLOYED", ignoreCase = true) == true // Assuming similar pattern for VSC
            // Add other categories/messages that should persist if necessary
            else -> false
        }
    }


    // --- Message Dismissal Logic ---
    private fun dismissExpiredMessages() {
        val currentTime = System.currentTimeMillis()
        val currentMessages = _displayedRaceControlMessages.value
        val messagesToKeep = currentMessages.filter {
            it.displayUntilMillis == null || it.displayUntilMillis > currentTime
        }

        if (messagesToKeep.size != currentMessages.size) {
            Log.d("PositionsViewModel", "Dismissing ${currentMessages.size - messagesToKeep.size} expired RC messages.")
            _displayedRaceControlMessages.value = messagesToKeep
        }
    }

    fun stopPolling() {
        Log.d("PositionsViewModel", "Stopping polling and message dismissal...")
        positionPollingJob?.cancel(); positionPollingJob = null
        teamRadioPollingJob?.cancel(); teamRadioPollingJob = null
        intervalPollingJob?.cancel(); intervalPollingJob = null
        raceControlPollingJob?.cancel(); raceControlPollingJob = null
        messageDismissalJob?.cancel(); messageDismissalJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
        // Clear timestamp cache for the session when ViewModel is destroyed
        if (currentSessionKey != -1) {
            raceControlRepository.clearTimestampCache(currentSessionKey)
            intervalRepository.clearTimestampCache() // Clear interval cache too
        }
    }
}