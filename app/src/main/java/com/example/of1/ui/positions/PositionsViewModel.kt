package com.example.of1.ui.positions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.of1.data.model.Position
import com.example.of1.data.model.openf1.Lap // Import Lap model
import com.example.of1.data.model.openf1.OF1Driver
import com.example.of1.data.model.openf1.OpenF1IntervalResponse
import com.example.of1.data.model.openf1.OpenF1RaceControlResponse
import com.example.of1.data.model.openf1.TeamRadio
import com.example.of1.data.repository.DriverRepository
import com.example.of1.data.repository.IntervalRepository
import com.example.of1.data.repository.LapRepository // Import LapRepository
import com.example.of1.data.repository.PositionRepository
import com.example.of1.data.repository.RaceControlRepository
import com.example.of1.data.repository.TeamRadioRepository
import com.example.of1.utils.Constants
import com.example.of1.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

// Data class defined here includes the flag
data class UiPosition(
    val position: Position,
    val driver: OF1Driver?,
    val latestRadio: TeamRadio? = null,
    val intervalData: OpenF1IntervalResponse? = null,
    val fastestLapTime: Double? = null,
    val isOverallFastest: Boolean = false // Flag added
)


@HiltViewModel
class PositionsViewModel @Inject constructor(
    private val positionRepository: PositionRepository,
    private val driverRepository: DriverRepository,
    private val teamRadioRepository: TeamRadioRepository,
    private val intervalRepository: IntervalRepository,
    private val raceControlRepository: RaceControlRepository,
    private val lapRepository: LapRepository
) : ViewModel() {

    // --- Internal StateFlows for Resource states ---
    private val _positions = MutableStateFlow<Resource<List<Position>>>(Resource.Loading())
    val positions: StateFlow<Resource<List<Position>>> = _positions
    private val _drivers = MutableStateFlow<Resource<List<OF1Driver>>>(Resource.Loading())
    val drivers: StateFlow<Resource<List<OF1Driver>>> = _drivers
    private val _teamRadioRaw = MutableStateFlow<Resource<List<TeamRadio>>>(Resource.Loading())
    private val _intervals = MutableStateFlow<Resource<Map<Int, OpenF1IntervalResponse>>>(Resource.Loading())
    val intervals: StateFlow<Resource<Map<Int, OpenF1IntervalResponse>>> = _intervals
    private val _displayedRaceControlMessages = MutableStateFlow<List<UiRaceControlMessage>>(emptyList())
    val displayedRaceControlMessages: StateFlow<List<UiRaceControlMessage>> = _displayedRaceControlMessages
    private val _lapStatusText = MutableStateFlow("")
    val lapStatusText: StateFlow<String> = _lapStatusText
    private val _lapsPerDriver = MutableStateFlow<ConcurrentHashMap<Int, List<Lap>>>(ConcurrentHashMap())

    // --- StateFlows for Fastest Lap Data ---
    private val fastestLapsMap: StateFlow<Map<Int, Double?>> = _lapsPerDriver
        .map { lapsMap ->
            lapsMap.mapValues { (_, laps) ->
                laps.mapNotNull { it.lapDuration }.minOrNull()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- StateFlow for OVERALL Session Fastest Lap Info (Driver Number + Time) ---
    // Changed back to store Pair<Int?, Double?>?
    private val overallSessionFastestLapInfo: StateFlow<Pair<Int?, Double?>?> = _lapsPerDriver
        .map { lapsMap ->
             lapsMap.values // Get all lists of laps (List<List<Lap>>)
                .flatten() // Flatten into a single List<Lap>
                .filter { it.lapDuration != null && it.lapDuration > 0 } // Consider only valid, completed laps
                .minByOrNull { it.lapDuration!! } // Find the lap with the minimum duration
                ?.let { fastestLap ->
                    // Return a Pair of (driverNumber, lapTime)
                    Pair(fastestLap.driverNumber, fastestLap.lapDuration)
                } // Returns null if the list is empty or no valid laps found
                .also {
                    // if (it != null) Log.d("PositionsViewModel", "Overall fastest lap calculated: Driver ${it.first}, Time ${it.second}")
                    // else Log.d("PositionsViewModel", "Overall fastest lap calculation resulted in null")
                }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    // --- StateFlows holding LATEST SUCCESSFUL DATA ---
    private val latestSuccessPositions: StateFlow<List<Position>> = _positions
        .filterIsInstance<Resource.Success<List<Position>>>().map { it.data ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val latestSuccessDrivers: StateFlow<List<OF1Driver>> = _drivers
        .filterIsInstance<Resource.Success<List<OF1Driver>>>().map { it.data ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val latestSuccessTeamRadio: StateFlow<Map<Int, TeamRadio?>> = _teamRadioRaw
        .filterIsInstance<Resource.Success<List<TeamRadio>>>()
        .map { successResource ->
            successResource.data
                ?.groupBy { it.driverNumber }
                ?.mapValues { entry -> entry.value.maxByOrNull { it.date } }
                ?: emptyMap()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val latestSuccessIntervals: StateFlow<Map<Int, OpenF1IntervalResponse>> = _intervals
        .filterIsInstance<Resource.Success<Map<Int, OpenF1IntervalResponse>>>().map { it.data ?: emptyMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())


    // --- Combine for UI Adapter ---
    val uiPositions: StateFlow<List<UiPosition>> = combine(
        latestSuccessPositions,
        latestSuccessDrivers,
        latestSuccessTeamRadio,
        latestSuccessIntervals,
        fastestLapsMap,
        overallSessionFastestLapInfo // Use the Pair flow
    ) { array: Array<*> ->
        // Extract data from the array by index and cast safely
        @Suppress("UNCHECKED_CAST") val positionList = array[0] as? List<Position> ?: emptyList()
        @Suppress("UNCHECKED_CAST") val driverList = array[1] as? List<OF1Driver> ?: emptyList()
        @Suppress("UNCHECKED_CAST") val radioMap = array[2] as? Map<Int, TeamRadio?> ?: emptyMap()
        @Suppress("UNCHECKED_CAST") val intervalMap = array[3] as? Map<Int, OpenF1IntervalResponse> ?: emptyMap()
        @Suppress("UNCHECKED_CAST") val fLapsMap = array[4] as? Map<Int, Double?> ?: emptyMap()
        @Suppress("UNCHECKED_CAST") val overallFastestInfo = array[5] as? Pair<Int?, Double?> // Cast to Pair

        // Log.d("PositionsViewModel", "--- COMBINING UI POSITIONS ---")
        val driverMap = driverList.associateBy { it.driverNumber }

        positionList.map { position ->
            val driverNumber = position.driverNumber
            val showIntervals = currentSessionType == "Race" || currentSessionType == "Sprint"
            val driverFastestLap = fLapsMap[driverNumber] // Get this driver's fastest time from the map
            val interval = if (showIntervals) intervalMap[driverNumber] else null

            // Determine if this driver has the overall fastest lap
            // Check if overallFastestInfo is not null AND the driver number matches
            val isOverallFastestLap = overallFastestInfo != null &&
                                      overallFastestInfo.first == driverNumber // Compare driver numbers

            UiPosition(
                position = position,
                driver = driverMap[driverNumber],
                latestRadio = radioMap[driverNumber],
                intervalData = interval,
                 // Still provide the driver's own fastest lap, even if not overall fastest
                fastestLapTime = if (!showIntervals) driverFastestLap else null,
                isOverallFastest = isOverallFastestLap // Set the flag based on driver number match
            )
        }/*.also {
             Log.d("PositionsViewModel", "--- COMBINE COMPLETE - Result Size: ${it.size} ---")
        }*/
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- Polling and Lifecycle Variables ---
    private var positionPollingJob: Job? = null
    private var teamRadioPollingJob: Job? = null
    private var intervalPollingJob: Job? = null
    private var raceControlPollingJob: Job? = null
    private var messageDismissalJob: Job? = null
    private var lapPollingJob: Job? = null
    private val driverLapFetchJobs = mutableMapOf<Int, Job>()
    private var isLive = false
    private var currentSessionKey: Int = -1
    private var currentMeetingKey: Int = -1
    private var currentSessionType: String = "Unknown"
    private val RC_MESSAGE_DURATION_MS = 25000L

    // --- Methods (initializeData, isDataLoading, fetchInitialData, fetchLapsForDrivers, etc.) ---
    // --- No changes needed in these methods for this specific feature ---
    fun initializeData(meetingKey: Int, sessionKey: Int, sessionType: String, isLive: Boolean) {
        if (this.currentSessionKey == sessionKey && !isDataLoading()) {
             Log.d("PositionsViewModel", "Data for session $sessionKey already loaded/loading.")
             if (this.isLive != isLive) {
                 this.isLive = isLive
                 stopPolling()
                 if (isLive) startPolling()
             }
             if (this.currentSessionType != sessionType) this.currentSessionType = sessionType
             return
        }
        Log.d("PositionsViewModel", "Initializing for session $sessionKey, type: $sessionType, isLive: $isLive")

        this.isLive = isLive
        this.currentSessionKey = sessionKey
        this.currentMeetingKey = meetingKey
        this.currentSessionType = sessionType

        stopPolling()
        _positions.value = Resource.Loading()
        _drivers.value = Resource.Loading()
        _teamRadioRaw.value = Resource.Loading()
        _intervals.value = Resource.Loading()
        _displayedRaceControlMessages.value = emptyList()
        _lapStatusText.value = ""
        _lapsPerDriver.value = ConcurrentHashMap()

        fetchInitialData()

        if (isLive) {
            startPolling()
        } else {
             Log.d("PositionsViewModel", "Session $sessionKey is not live, not starting polling.")
        }
    }

    private fun isDataLoading(): Boolean {
        return (_positions.value is Resource.Loading && _positions.value.data == null) ||
               (_drivers.value is Resource.Loading && _drivers.value.data == null) ||
               (_intervals.value is Resource.Loading && isLive && _intervals.value.data == null)
    }

    private fun fetchInitialData() {
        val sessionKey = currentSessionKey
        val meetingKey = currentMeetingKey
        val isLive = this.isLive

        viewModelScope.launch { positionRepository.getPositions(meetingKey, sessionKey).collect { _positions.value = it } }
        viewModelScope.launch { teamRadioRepository.getTeamRadio(sessionKey).collect { _teamRadioRaw.value = it } }
        viewModelScope.launch { intervalRepository.getLatestIntervals(sessionKey, isLive).collect { _intervals.value = it } }
        viewModelScope.launch {
            driverRepository.getDrivers(sessionKey, meetingKey).collect { driverResource ->
                _drivers.value = driverResource
                if (driverResource is Resource.Success) {
                    val drivers = driverResource.data ?: emptyList()
                    // Fetch laps for ALL drivers now, as we need it for overall fastest comparison even in Race/Sprint
                    fetchLapsForDrivers(drivers)
                     if (!isLive) {
                         fetchHistoricalTotalLaps(sessionKey)
                     }
                }
            }
        }
        if (isLive) {
             fetchLiveLapCount(sessionKey)
             startRaceControlPolling(sessionKey)
             startMessageDismissalJob()
        }
    }

    private fun fetchLapsForDrivers(drivers: List<OF1Driver>) {
        val sessionKey = currentSessionKey
        if (sessionKey == -1) return

        drivers.forEach { driver ->
            val driverNum = driver.driverNumber
            if (driverLapFetchJobs[driverNum]?.isActive != true) {
                driverLapFetchJobs[driverNum] = viewModelScope.launch {
                    Log.d("PositionsViewModel", "Fetching laps for driver $driverNum")
                    try {
                        lapRepository.getLapsForDriver(sessionKey, driverNum)
                            .collect { lapResource ->
                                if (lapResource is Resource.Success) {
                                    val lapsData = lapResource.data ?: emptyList()
                                    val currentMap = _lapsPerDriver.value
                                    if (currentMap[driverNum] != lapsData) {
                                        val newMap = ConcurrentHashMap(currentMap)
                                        newMap[driverNum] = lapsData
                                        _lapsPerDriver.value = newMap
                                    }
                                } else if (lapResource is Resource.Error) {
                                    Log.e("PositionsViewModel", "Error fetching laps resource for driver $driverNum: ${lapResource.message}")
                                }
                            }
                    } catch (e: Exception) {
                         Log.e("PositionsViewModel", "Exception collecting laps for driver $driverNum", e)
                    }
                }
            }
        }
    }

    private fun fetchHistoricalTotalLaps(sessionKey: Int) {
         viewModelScope.launch {
             latestSuccessPositions.filter { it.isNotEmpty() }.first().let { positions ->
                 val p1DriverNumber = positions.first().driverNumber
                 Log.d("PositionsViewModel", "Fetching historical laps for P1 driver $p1DriverNumber")
                 lapRepository.getLapsForDriver(sessionKey, p1DriverNumber)
                     .filterIsInstance<Resource.Success<List<Lap>>>()
                     .collect { lapResource ->
                         lapResource.data?.let { p1Laps ->
                             val maxLap = p1Laps.maxOfOrNull { it.lapNumber } ?: 0
                             if (maxLap > 0) {
                                val newText = "Laps: $maxLap"
                                if (_lapStatusText.value != newText) {
                                     _lapStatusText.value = newText
                                     Log.d("PositionsViewModel", "Historical total laps updated: $maxLap")
                                }
                             }
                         }
                     }
             }
         }
    }

    private fun fetchLiveLapCount(sessionKey: Int) {
        lapPollingJob?.cancel()
        lapPollingJob = viewModelScope.launch {
            Log.d("PositionsViewModel", "Starting polling for live lap count")
            lapRepository.getCurrentLapNumber(sessionKey).collect { maxLap ->
                val newText = if (maxLap != null && maxLap > 0) "Lap $maxLap" else "Lap -"
                 if (_lapStatusText.value != newText) {
                    _lapStatusText.value = newText
                 }
            }
        }
    }


    // --- Polling Logic ---
    fun startPolling() {
        if (!isLive || currentSessionKey == -1) return
        Log.d("PositionsViewModel", "Starting polling for session $currentSessionKey.")

        if (positionPollingJob?.isActive != true) {
            positionPollingJob = viewModelScope.launch { pollData { positionRepository.getPositions(currentMeetingKey, currentSessionKey).collect { _positions.value = it } } }
        }
        if (intervalPollingJob?.isActive != true) {
             if (currentSessionType == "Race" || currentSessionType == "Sprint") {
                 intervalPollingJob = viewModelScope.launch { pollData { intervalRepository.getLatestIntervals(currentSessionKey, true).collect { _intervals.value = it } } }
             }
        }
        if (teamRadioPollingJob?.isActive != true) {
            teamRadioPollingJob = viewModelScope.launch { pollData { teamRadioRepository.getTeamRadio(currentSessionKey).collect { _teamRadioRaw.value = it } } }
        }
        // Poll Laps for all drivers if live, to keep fastest lap potentially updated
        // This might be heavy, consider alternatives if performance is an issue
        viewModelScope.launch {
            latestSuccessDrivers.filter { it.isNotEmpty() }.collect { drivers ->
                 Log.d("PositionsViewModel", "Polling: Triggering lap fetch for ${drivers.size} drivers")
                 fetchLapsForDrivers(drivers) // Re-trigger fetches based on current driver list
            }
        }


        startRaceControlPolling(currentSessionKey)
        startMessageDismissalJob()

        if (lapPollingJob?.isActive != true) {
             fetchLiveLapCount(currentSessionKey)
        }
    }

    // Helper for polling loops
    private suspend fun pollData(fetchAction: suspend () -> Unit) {
        while (true) {
            try {
                fetchAction()
            } catch (e: Exception) {
                 Log.e("PositionsViewModel", "Error during collect in pollData: ${e.message}")
            }
            delay(Constants.POLLING_RATE)
        }
    }


    // --- Message Processing & Dismissal Logic ---
    private fun startRaceControlPolling(sessionKey: Int) {
        if (raceControlPollingJob?.isActive != true) {
            raceControlPollingJob = viewModelScope.launch {
                pollData {
                    raceControlRepository.getMessages(sessionKey).collect { resource ->
                        if (resource is Resource.Success) {
                            processNewRaceControlMessages(resource.data ?: emptyList())
                        } else if (resource is Resource.Error) {
                            Log.e("PositionsViewModel", "Error polling RC: ${resource.message}")
                        }
                    }
                }
            }
        }
    }

    private fun processNewRaceControlMessages(newApiMessages: List<OpenF1RaceControlResponse>) {
        if (newApiMessages.isEmpty()) return

        val currentTime = System.currentTimeMillis()
        val updatedList = _displayedRaceControlMessages.updateAndGet { currentDisplayed ->
            val currentDisplayedCopy = currentDisplayed.toMutableList()
            var listChanged = false

            val clearingFlags = listOf("GREEN", "CLEAR")
            val clearingMessages = listOf("TRACK CLEAR", "SAFETY CAR IN THIS LAP", "VSC ENDING")
            val isClearingMessageReceived = newApiMessages.any { msg ->
                (msg.category == "Flag" && clearingFlags.contains(msg.flag?.uppercase())) ||
                        (msg.message != null && clearingMessages.any { clearMsg -> msg.message.contains(clearMsg, ignoreCase = true) })
            }

            if (isClearingMessageReceived) {
                currentDisplayedCopy.forEachIndexed { index, msg ->
                    if (msg.isPersistent && msg.displayUntilMillis == null) {
                        currentDisplayedCopy[index] = msg.copy(displayUntilMillis = currentTime + RC_MESSAGE_DURATION_MS)
                        listChanged = true
                    }
                }
            }

            newApiMessages.forEach { apiMsg ->
                val id = apiMsg.date
                if (currentDisplayedCopy.none { it.id == id }) {
                    val messageText = apiMsg.message ?: apiMsg.category ?: "Unknown Event"
                    val isPersistent = determinePersistence(apiMsg)
                    val displayUntil = if (isPersistent) null else currentTime + RC_MESSAGE_DURATION_MS
                    val uiMsg = UiRaceControlMessage(
                        id = id, message = messageText, category = apiMsg.category,
                        flag = apiMsg.flag, scope = apiMsg.scope, isPersistent = isPersistent,
                        displayUntilMillis = displayUntil, creationTimeMillis = currentTime
                    )
                    currentDisplayedCopy.add(uiMsg)
                    listChanged = true
                }
            }

            if (listChanged) currentDisplayedCopy.sortedBy { it.id } else currentDisplayed
        }

        if (updatedList.any { it.displayUntilMillis != null } && (messageDismissalJob == null || !messageDismissalJob!!.isActive)) {
            startMessageDismissalJob()
        }
    }

    private fun determinePersistence(msg: OpenF1RaceControlResponse): Boolean {
        val persistentFlags = listOf("RED", "YELLOW", "DOUBLE YELLOW")
        val persistentMessages = listOf("SAFETY CAR DEPLOYED", "VIRTUAL SAFETY CAR DEPLOYED")
        return (msg.category?.equals("Flag", ignoreCase = true) == true && persistentFlags.contains(msg.flag?.uppercase())) ||
                (msg.message != null && persistentMessages.any { persistMsg -> msg.message.contains(persistMsg, ignoreCase = true) })
    }

    private fun dismissExpiredMessages() {
        val currentTime = System.currentTimeMillis()
        _displayedRaceControlMessages.update { currentMessages ->
            val messagesToKeep = currentMessages.filter { msg ->
                msg.displayUntilMillis == null || msg.displayUntilMillis > currentTime
            }
            if (messagesToKeep.size != currentMessages.size) {
                val dismissedCount = currentMessages.size - messagesToKeep.size
                // Log.d("PositionsViewModel", "Dismissing $dismissedCount expired RC message(s).") // Optional log
            }
            if (messagesToKeep.none { it.displayUntilMillis != null }) {
                stopMessageDismissalJob()
            }
            if (messagesToKeep.size != currentMessages.size) messagesToKeep else currentMessages
        }
    }

    private fun startMessageDismissalJob() {
        if (messageDismissalJob?.isActive != true) {
            messageDismissalJob = viewModelScope.launch {
                Log.d("PositionsViewModel", "Starting message dismissal job.")
                while (true) {
                    delay(1000)
                    dismissExpiredMessages()
                }
            }
        }
    }

    private fun stopMessageDismissalJob() {
        if (messageDismissalJob?.isActive == true) {
            Log.d("PositionsViewModel", "Stopping message dismissal job.")
            messageDismissalJob?.cancel()
            messageDismissalJob = null
        }
    }

    fun stopPolling() {
        Log.d("PositionsViewModel", "Stopping polling for session $currentSessionKey...")
        positionPollingJob?.cancel(); positionPollingJob = null
        teamRadioPollingJob?.cancel(); teamRadioPollingJob = null
        intervalPollingJob?.cancel(); intervalPollingJob = null
        raceControlPollingJob?.cancel(); raceControlPollingJob = null
        lapPollingJob?.cancel(); lapPollingJob = null
        stopMessageDismissalJob()
        driverLapFetchJobs.values.forEach { it.cancel() }
        driverLapFetchJobs.clear()
    }


    override fun onCleared() {
        super.onCleared()
        stopPolling()
        Log.d("PositionsViewModel", "ViewModel cleared.")
    }
}