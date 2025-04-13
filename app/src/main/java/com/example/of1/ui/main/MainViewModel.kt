package com.example.of1.ui.main // Adjust package if needed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.of1.data.model.Race
import com.example.of1.data.model.Session
import com.example.of1.data.repository.RaceRepository
import com.example.of1.data.repository.SessionRepository
import com.example.of1.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// Event for navigating from main list item click
data class MainNavigationEvent(
    val meetingKey: Int,
    val sessionKey: Int,
    val sessionType: String,
    val isLive: Boolean
)

// Event for navigating from the dialog
data class DialogNavigationEvent(
    val year: String,
    val dateStart: String,
    val dateEnd: String
)

// Sealed class to represent the state of the bridging process for a session
sealed class BridgingState {
    object Idle : BridgingState()
    object Loading : BridgingState()
    data class Error(val message: String) : BridgingState()
    object Success : BridgingState() // Indicates bridging completed, navigation should occur
}


@HiltViewModel
class MainViewModel @Inject constructor(
    private val raceRepository: RaceRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    // --- State for Main Race List ---
    private val _races = MutableStateFlow<Resource<List<Race>>>(Resource.Loading(true))

    val displayedRaces: StateFlow<List<Race>> = _races
        .filterIsInstance<Resource.Success<List<Race>>>()
        .map { successResource -> successResource.data ?: emptyList() } // Keep chronological order
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoading: StateFlow<Boolean> = _races
        .map { it is Resource.Loading && it.isLoading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val errorMessage: StateFlow<String?> = _races
        .map { if (it is Resource.Error) it.message else null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _mainNavigationEvent = MutableSharedFlow<MainNavigationEvent>()
    val mainNavigationEvent: SharedFlow<MainNavigationEvent> = _mainNavigationEvent.asSharedFlow()

    private val _bridgingState = MutableStateFlow<Map<String, BridgingState>>(emptyMap())
    val bridgingState: StateFlow<Map<String, BridgingState>> = _bridgingState.asStateFlow()

    private var currentYear: String = ""

    // --- State for Dialog ---
    private val _yearsForDialog = MutableStateFlow<List<String>>(emptyList())
    val yearsForDialog: StateFlow<List<String>> = _yearsForDialog.asStateFlow()

    private val _dialogRaces = MutableStateFlow<Resource<List<Race>>>(Resource.Success(emptyList())) // Initial state not loading
    val dialogRaces: StateFlow<Resource<List<Race>>> = _dialogRaces.asStateFlow()

    private val _dialogNavigationEvent = MutableSharedFlow<DialogNavigationEvent>()
    val dialogNavigationEvent: SharedFlow<DialogNavigationEvent> = _dialogNavigationEvent.asSharedFlow()


    init {
        val calendar = Calendar.getInstance()
        currentYear = calendar.get(Calendar.YEAR).toString()
        generateYearsForDialog()
        fetchRacesForCurrentSeason()
    }

    private fun generateYearsForDialog() {
        val current = Calendar.getInstance().get(Calendar.YEAR)
        _yearsForDialog.value = (current downTo 1950).map { it.toString() }
    }

    fun fetchRacesForCurrentSeason() {
        viewModelScope.launch {
            _races.value = Resource.Loading(true) // Ensure loading is set
            raceRepository.getRaces(currentYear)
                .catch { e ->
                    Log.e("MainViewModel", "Error fetching current season races", e)
                    _races.value = Resource.Error("Failed to fetch races: ${e.localizedMessage}")
                }
                .collect { resource -> _races.value = resource }
        }
    }

    // --- Function for Dialog Race Fetching ---
    fun fetchRacesForYearDialog(year: String) {
        viewModelScope.launch {
            // Use the specific StateFlow for the dialog
            raceRepository.getRaces(year)
                .onStart { _dialogRaces.value = Resource.Loading(true) }
                .catch { e ->
                    Log.e("MainViewModel", "Error fetching dialog races for $year", e)
                    _dialogRaces.value = Resource.Error("Failed to fetch races: ${e.localizedMessage}")
                }
                .collect { resource ->
                    // Emit success/error to the dialog state flow
                    _dialogRaces.value = if (resource is Resource.Success) {
                        Resource.Success(resource.data ?: emptyList())
                    } else {
                        resource // Pass Loading or Error through
                    }
                }
        }
    }

    // --- Function for preparing Dialog Navigation ---
    fun prepareDialogNavigation(race: Race) {
        viewModelScope.launch {
            val (startDate, endDate) = calculateDateRange(race)
            if (startDate != null && endDate != null) {
                _dialogNavigationEvent.emit(
                    DialogNavigationEvent(
                        year = race.season,
                        dateStart = startDate,
                        dateEnd = endDate
                    )
                )
            } else {
                // Handle error - maybe emit an error event? For now, just log.
                Log.e("MainViewModel", "Could not calculate date range for dialog navigation for ${race.raceName}")
                // Optionally, update a dialog error state here
            }
        }
    }


    // --- Bridging Logic for Main List Item Click ---
    fun findSessionAndPrepareNavigation(race: Race, sessionName: String, sessionDate: String, sessionTime: String?) {
        val bridgingKey = "${race.round}_${sessionName}"
        Log.d("MainViewModel", "Attempting main list bridging for key: $bridgingKey")
        _bridgingState.update { currentMap -> currentMap.toMutableMap().apply { this[bridgingKey] = BridgingState.Loading } }

        val (startDateString, endDateString) = calculateDateRange(race)
        if (startDateString == null || endDateString == null) {
            Log.e("MainViewModel", "Could not calculate date range for main list bridging, round ${race.round}")
            _bridgingState.update { currentMap -> currentMap.toMutableMap().apply { this[bridgingKey] = BridgingState.Error("Date error") } }
            return
        }

        viewModelScope.launch {
            sessionRepository.getSessionsByDate(race.season.toInt(), startDateString, endDateString)
                .collect { sessionResource ->
                    when (sessionResource) {
                        is Resource.Success -> {
                            val openF1Sessions = sessionResource.data ?: emptyList()
                            val targetSession = findMatchingOpenF1Session(openF1Sessions, sessionName, sessionDate, sessionTime)
                            if (targetSession != null) {
                                val isLive = isSessionLive(targetSession.dateStart, targetSession.dateEnd)
                                _mainNavigationEvent.emit( // Emit to the correct flow
                                    MainNavigationEvent(
                                        meetingKey = targetSession.meetingKey,
                                        sessionKey = targetSession.sessionKey,
                                        sessionType = targetSession.sessionName,
                                        isLive = isLive
                                    )
                                )
                                _bridgingState.update { currentMap -> currentMap.toMutableMap().apply { this[bridgingKey] = BridgingState.Success } }
                            } else {
                                Log.w("MainViewModel", "Bridge failed (main list): No matching OpenF1 session for '$sessionName' on $sessionDate.")
                                _bridgingState.update { currentMap -> currentMap.toMutableMap().apply { this[bridgingKey] = BridgingState.Error("Session not found") } }
                            }
                        }
                        is Resource.Error -> {
                            Log.e("MainViewModel", "Bridge failed (main list): Error fetching OpenF1 sessions: ${sessionResource.message}")
                            _bridgingState.update { currentMap -> currentMap.toMutableMap().apply { this[bridgingKey] = BridgingState.Error(sessionResource.message ?: "API Error") } }
                        }
                        is Resource.Loading -> {
                            Log.d("MainViewModel", "Bridge in progress (main list): Loading OpenF1 sessions...")
                        }
                    }
                }
        }
    }

    // --- Helper Functions (calculateDateRange, findMatchingOpenF1Session, mapJolpicaToOpenF1SessionName, isSessionLive, clearBridgingState) remain the same ---
    private fun calculateDateRange(race: Race): Pair<String?, String?> {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US) // OpenF1 uses this format
        outputFormat.timeZone = TimeZone.getTimeZone("UTC")

        val earliestDate = listOfNotNull(
            race.firstPractice?.date, race.secondPractice?.date, race.thirdPractice?.date,
            race.qualifying?.date, race.sprint?.date, race.date
        ).minOrNull() ?: race.date

        val raceDateStr = race.date

        try {
            val startCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val earliestParsed = inputFormat.parse(earliestDate)
            if(earliestParsed != null) {
                startCal.time = earliestParsed
            } else {
                return Pair(null, null) // Cannot parse earliest date
            }


            val endCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val raceParsed = inputFormat.parse(raceDateStr)
            if(raceParsed != null) {
                endCal.time = raceParsed
                endCal.add(Calendar.DAY_OF_MONTH, 1) // Day after the race
            } else {
                return Pair(null, null) // Cannot parse race date
            }


            return Pair(outputFormat.format(startCal.time), outputFormat.format(endCal.time))

        } catch (e: ParseException) {
            Log.e("MainViewModel", "Error parsing dates for range calculation", e)
            return Pair(null, null)
        }
    }

    private fun findMatchingOpenF1Session(
        sessions: List<Session>,
        jolpicaSessionName: String,
        jolpicaDate: String,
        jolpicaTime: String?
    ): Session? {
        val targetOpenF1Name = mapJolpicaToOpenF1SessionName(jolpicaSessionName)
        if (targetOpenF1Name == null) {
            Log.w("MainViewModel", "No mapping found for Jolpica session name: $jolpicaSessionName")
            return null
        }

        Log.d("MainViewModel", "Searching for OpenF1 session matching name '$targetOpenF1Name' around date '$jolpicaDate'")

        val potentialMatches = sessions.filter { it.sessionName.equals(targetOpenF1Name, ignoreCase = true) }

        if (potentialMatches.size == 1) {
            Log.d("MainViewModel", "Found unique match by name: ${potentialMatches.first().sessionKey}")
            return potentialMatches.first()
        } else if (potentialMatches.size > 1) {
            Log.w("MainViewModel", "Multiple OpenF1 sessions found matching name '$targetOpenF1Name'. Attempting date filtering.")
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val targetDateCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            try {
                inputFormat.parse(jolpicaDate)?.let { targetDateCal.time = it } ?: return null
            } catch (e: ParseException) {
                Log.e("MainViewModel", "Failed to parse jolpicaDate: $jolpicaDate", e)
                return null
            }

            val dateFilteredMatches = potentialMatches.filter { session ->
                session.dateStart?.let { dateStartStr ->
                    try {
                        val openF1DateStr = dateStartStr.substringBefore('T')
                        val openF1Date = inputFormat.parse(openF1DateStr)
                        val openF1Cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        if(openF1Date != null) {
                            openF1Cal.time = openF1Date
                            openF1Cal.get(Calendar.YEAR) == targetDateCal.get(Calendar.YEAR) &&
                                    openF1Cal.get(Calendar.MONTH) == targetDateCal.get(Calendar.MONTH) &&
                                    openF1Cal.get(Calendar.DAY_OF_MONTH) == targetDateCal.get(Calendar.DAY_OF_MONTH)
                        } else false
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Error comparing dates for session ${session.sessionKey}", e)
                        false
                    }
                } ?: false
            }

            if (dateFilteredMatches.size == 1) {
                Log.d("MainViewModel", "Found unique match by name and date: ${dateFilteredMatches.first().sessionKey}")
                return dateFilteredMatches.first()
            } else {
                Log.w("MainViewModel", "Could not find a unique match even after date filtering (${dateFilteredMatches.size} matches). Returning null.")
                return null
            }
        } else {
            Log.w("MainViewModel", "No OpenF1 sessions found matching name '$targetOpenF1Name'.")
            return null // No matches by name
        }
    }

    private fun mapJolpicaToOpenF1SessionName(jolpicaName: String): String? {
        // Map based on common patterns - NEEDS VERIFICATION WITH ACTUAL OpenF1 DATA
        return when (jolpicaName.lowercase(Locale.ROOT).replace("practice", "").trim()) {
            "1" -> "Practice 1"
            "2" -> "Practice 2"
            "3" -> "Practice 3"
            "qualifying" -> "Qualifying"
            "sprint qualifying", "sprint shootout" -> "Sprint Shootout" // Verify exact OpenF1 name
            "sprint" -> "Sprint"
            "race" -> "Race"
            else -> {
                // Handle cases where Jolpica name is already close (e.g., "Race", "Sprint")
                when (jolpicaName.lowercase(Locale.ROOT)){
                    "race" -> "Race"
                    "sprint" -> "Sprint"
                    "qualifying" -> "Qualifying"
                    else -> null // Fallback if no direct match or pattern match
                }
            }
        }
    }


    private fun isSessionLive(startDateStr: String?, endDateStr: String?): Boolean {
        if (startDateStr == null || endDateStr == null) return false

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC") // OpenF1 uses UTC

        try {
            val startCleaned = startDateStr.replace(".\\d+".toRegex(), "").replace("+00:00", "")
            val endCleaned = endDateStr.replace(".\\d+".toRegex(), "").replace("+00:00", "")

            val startDate = dateFormat.parse(startCleaned)
            val endDate = dateFormat.parse(endCleaned)
            val now = Date() // Current time

            return if (startDate != null && endDate != null) {
                now.after(startDate) && now.before(endDate)
            } else {
                false
            }
        } catch (e: ParseException) {
            Log.e("MainViewModel", "Error parsing session dates ('$startDateStr', '$endDateStr') for liveness check", e)
            return false
        }
    }

    fun clearBridgingState(key: String) {
        _bridgingState.update { currentMap ->
            if (currentMap.containsKey(key)) {
                currentMap.toMutableMap().apply { remove(key) }
            } else {
                currentMap // No change needed
            }
        }
    }
}