package com.example.of1.ui.races.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.of1.data.model.Race
import com.example.of1.data.repository.RaceRepository
import com.example.of1.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class RacesViewModel @Inject constructor(
    private val repository: RaceRepository
) : ViewModel() {
    private val _races = MutableStateFlow<Resource<List<Race>>>(Resource.Loading())
    val races: StateFlow<Resource<List<Race>>> = _races

    fun getRaces(season: String) {
        repository.getRaces(season)
            .onEach {
                _races.value = it
            }.launchIn(viewModelScope)
    }
}