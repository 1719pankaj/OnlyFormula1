package com.example.of1.ui.laps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.of1.data.model.openf1.Lap
import com.example.of1.data.repository.LapRepository
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
class LapsViewModel @Inject constructor(
    private val repository: LapRepository
) : ViewModel() {

    private val _laps = MutableStateFlow<Resource<List<Lap>>>(Resource.Loading())
    val laps: StateFlow<Resource<List<Lap>>> = _laps

    private var pollingJob: kotlinx.coroutines.Job? = null

    fun getLaps(sessionKey: Int, driverNumber: Int, isLive: Boolean) {
        viewModelScope.launch {
            if (isLive) {
                startPolling(sessionKey, driverNumber)
            } else {
                // If not live, just fetch once.
                repository.getLaps(sessionKey, driverNumber)
                    .onEach { _laps.value = it }
                    .launchIn(viewModelScope)
            }
        }
    }

    private fun startPolling(sessionKey: Int, driverNumber: Int) {
        pollingJob?.cancel() // Cancel any existing polling job
        pollingJob = viewModelScope.launch {
            while (true) {
                repository.getLaps(sessionKey, driverNumber)
                    .onEach { _laps.value = it }
                    .launchIn(this) // Use 'this' for the coroutine scope
                delay(Constants.POLLING_RATE)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel() // Cancel polling when ViewModel is cleared
    }
    fun stopPolling() {
        pollingJob?.cancel()
    }
}