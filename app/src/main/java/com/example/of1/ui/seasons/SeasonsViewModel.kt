package com.example.of1.ui.seasons.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.of1.data.model.Season
import com.example.of1.data.repository.SeasonRepository
import com.example.of1.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class SeasonsViewModel @Inject constructor(private val repository: SeasonRepository) : ViewModel() {

    private val _seasons = MutableStateFlow<Resource<List<Season>>>(Resource.Loading())
    val seasons: StateFlow<Resource<List<Season>>> = _seasons

    init {
        getSeasons() // Fetch seasons when ViewModel is created
    }

    internal fun getSeasons() {
        repository.getSeasons()
            .onEach { result ->
                _seasons.value = result
            }
            .launchIn(viewModelScope)
    }
}