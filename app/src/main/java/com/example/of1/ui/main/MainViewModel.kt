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

// Data class to hold navigation parameters
data class NavigationEvent(
    val meetingKey: Int,
    val sessionKey: Int,
    val sessionType: String,
    val isLive: Boolean
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

    // Internal StateFlow holding the Resource from the repository
    private val _races = MutableStateFlow<Resource<List<Race>>>(Resource.Loading(true)) // Start explicitly loading

    // --- StateFlows exposed to the UI ---

    // 1. StateFlow for the actual list data (only updates on Success)
    val displayedRaces: StateFlow<List<Race>> = _races
        .filterIsInstance<Resource.Success<List<Race>>>() // Only react to Success
        .map { successResource ->
            successResource.data ?: emptyList()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList() // Start with an empty list
        )

    // 2. StateFlow for the loading state
    val isLoading: StateFlow<Boolean> = _races
        .map { it is Resource.Loading && it.isLoading } // Map Resource state to boolean
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true // Initially loading
        )

    // 3. StateFlow for error messages
    val errorMessage: StateFlow<String?> = _races
        .map { if (it is Resource.Error) it.message else null } // Extract message on Error
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null // Initially no error
        )

    // For single navigation events
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    // To track the state of bridging for each session click
    private val _bridgingState = MutableStateFlow<Map<String, BridgingState>>(emptyMap())
    val bridgingState: StateFlow<Map<String, BridgingState>> = _bridgingState.asStateFlow()

    private var currentYear: String = ""

    init {
        val calendar = Calendar.getInstance()
        currentYear = calendar.get(Calendar.YEAR).toString()
        fetchRacesForCurrentSeason()
    }

    fun fetchRacesForCurrentSeason() {
        viewModelScope.launch {
            // Set initial loading state explicitly if needed (though _races starts as Loading)
            // _races.value = Resource.Loading(true)
            raceRepository.getRaces(currentYear)
                .catch { e ->
                    Log.e("MainViewModel", "Error fetching races flow", e)
                    // Ensure error state is emitted
                    _races.value = Resource.Error("Failed to fetch races: ${e.localizedMessage}")
                }
                .collect { resource ->
                    // Simply assign the collected resource to the internal StateFlow
                    _races.value = resource
                }
        }
    }

    // --- Bridging Logic (findSessionAndPrepareNavigation, helpers) remains the same ---
    fun findSessionAndPrepareNavigation(race: Race, sessionName: String, sessionDate: String, sessionTime: String?) {
        val bridgingKey = "${race.round}_${sessionName}"
        Log.d("MainViewModel", "Attempting bridging for key: $bridgingKey")

        // --- 1. Set Bridging State to Loading ---
        _bridgingState.update { currentMap ->
            currentMap.toMutableMap().apply { this[bridgingKey] = BridgingState.Loading }
        }

        // --- 2. Calculate Date Range ---
        val (startDateString, endDateString) = calculateDateRange(race)
        if (startDateString == null || endDateString == null) {
            Log.e("MainViewModel", "Could not calculate date range for race round ${race.round}")
            _bridgingState.update { currentMap ->
                currentMap.toMutableMap().apply { this[bridgingKey] = BridgingState.Error("Date calculation error") }
            }
            return
        }
        Log.d("MainViewModel", "Date range for OpenF1 lookup: $startDateString to $endDateString")

        // --- 3. Fetch Sessions from OpenF1 Repository ---
        viewModelScope.launch {
            sessionRepository.getSessionsByDate(race.season.toInt(), startDateString, endDateString)
                .collect { sessionResource ->
                    when (sessionResource) {
                        is Resource.Success -> {
                            val openF1Sessions = sessionResource.data ?: emptyList()
                            Log.d("MainViewModel", "Received ${openF1Sessions.size} OpenF1 sessions for the date range.")
                            // --- 4. Filter and Find Matching Session ---
                            val targetSession = findMatchingOpenF1Session(openF1Sessions, sessionName, sessionDate, sessionTime)

                            if (targetSession != null) {
                                Log.i("MainViewModel", "Bridge successful! Found OpenF1 session: Key=${targetSession.sessionKey}, Name=${targetSession.sessionName}")
                                // --- 5. Determine Liveness ---
                                val isLive = isSessionLive(targetSession.dateStart, targetSession.dateEnd)
                                Log.d("MainViewModel", "Session Liveness: $isLive")

                                // --- 6. Emit Navigation Event ---
                                _navigationEvent.emit(
                                    NavigationEvent(
                                        meetingKey = targetSession.meetingKey,
                                        sessionKey = targetSession.sessionKey,
                                        // Use the *OpenF1* sessionName as the sessionType for PositionsFragment
                                        sessionType = targetSession.sessionName,
                                        isLive = isLive
                                    )
                                )
                                // Update state to success (optional, navigation event might suffice)
                                _bridgingState.update { currentMap ->
                                    currentMap.toMutableMap().apply { this[bridgingKey] = BridgingState.Success }
                                }
                            } else {
                                Log.w("MainViewModel", "Bridge failed: Could not find matching OpenF1 session for '$sessionName' on $sessionDate.")
                                // --- 4b. No Match Found ---
                                _bridgingState.update { currentMap ->
                                    currentMap.toMutableMap().apply { this[bridgingKey] = BridgingState.Error("Session not found in OpenF1 data") }
                                }
                            }
                        }
                        is Resource.Error -> {
                            Log.e("MainViewModel", "Bridge failed: Error fetching OpenF1 sessions: ${sessionResource.message}")
                            // --- 3b. OpenF1 Fetch Error ---
                            _bridgingState.update { currentMap ->
                                currentMap.toMutableMap().apply { this[bridgingKey] = BridgingState.Error(sessionResource.message ?: "API Error") }
                            }
                        }
                        is Resource.Loading -> {
                            // State is already Loading, no action needed here unless refining intermediate states
                            Log.d("MainViewModel", "Bridge in progress: Still loading OpenF1 sessions...")
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
            // Handle potential milliseconds and timezone offset variations from OpenF1
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