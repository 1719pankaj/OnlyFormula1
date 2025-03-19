package com.example.of1.ui.main.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.of1.data.model.Session
import com.example.of1.data.repository.SessionRepository
import com.example.of1.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val repository: SessionRepository) : ViewModel() {

    private val _sessions = MutableStateFlow<Resource<List<Session>>>(Resource.Loading())
    val sessions: StateFlow<Resource<List<Session>>> = _sessions

    //Added init block to call for sessions as soon as the fragment is created.
    init {
        getSessions("Belgium", "Sprint", 2023)
    }
    fun getSessions(countryName: String, sessionName: String, year: Int) {
        viewModelScope.launch {
            repository.getSessions(countryName, sessionName, year)
                .onEach { result ->
                    _sessions.value = result
                }
                .launchIn(viewModelScope)
        }
    }
}