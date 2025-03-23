package com.example.of1.ui.positions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.of1.data.model.Position
import com.example.of1.data.repository.PositionRepository
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
class PositionsViewModel @Inject constructor(
    private val repository: PositionRepository
) : ViewModel() {

    private val _positions = MutableStateFlow<Resource<List<Position>>>(Resource.Loading())
    val positions: StateFlow<Resource<List<Position>>> = _positions

    private var pollingJob: kotlinx.coroutines.Job? = null // Keep track of the polling job

    fun getPositions(meetingKey: Int, sessionKey: Int, isLive: Boolean) {
        viewModelScope.launch {
            if (isLive) {
                startPolling(meetingKey, sessionKey) // Start polling only if isLive is true
            } else {
                // If not live, just fetch once and don't start polling.
                repository.getPositions(meetingKey, sessionKey)
                    .onEach { _positions.value = it }
                    .launchIn(viewModelScope)
            }
        }
    }
    private fun startPolling(meetingKey: Int, sessionKey: Int){
        pollingJob?.cancel() // Cancel any existing polling job
        pollingJob = viewModelScope.launch {
            while (true){
                repository.getPositions(meetingKey, sessionKey)
                    .onEach {
                        _positions.value = it
                    }.launchIn(this)
                delay(Constants.POLLING_RATE)
            }
        }
    }
    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel() // VERY IMPORTANT: Cancel the polling when the ViewModel is cleared
    }
    fun stopPolling() {
        pollingJob?.cancel()
    }
}
