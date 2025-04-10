package com.example.of1.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.of1.data.model.Session
//import com.example.of1.data.model.openf1.OpenF1SessionResponse // Removed as unused now
import com.example.of1.data.repository.SessionRepository
import com.example.of1.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val repository: SessionRepository
) : ViewModel() {
    private val _sessions = MutableStateFlow<Resource<List<Session>>>(Resource.Loading())
    val sessions : StateFlow<Resource<List<Session>>> = _sessions

    // Function to fetch sessions by meeting key (still potentially useful)
    fun getSessions(meetingKey: Int){
        repository.getSessionsByMeetingKey(meetingKey)
            .onEach {
                _sessions.value = it
            }.launchIn(viewModelScope)
    }

    // New function to fetch sessions by date range
    fun fetchSessionsByDate(year: Int, dateStart: String, dateEnd: String) {
        repository.getSessionsByDate(year, dateStart, dateEnd)
            .onEach {
                _sessions.value = it // Update the same StateFlow
            }.launchIn(viewModelScope)
    }
}