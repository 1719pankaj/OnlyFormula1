package com.example.of1.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.of1.data.model.Result
import com.example.of1.data.repository.ResultRepository
import com.example.of1.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val repository: ResultRepository
) : ViewModel() {

    private val _results = MutableStateFlow<Resource<List<Result>>>(Resource.Loading())
    val results: StateFlow<Resource<List<Result>>> = _results

    fun getResults(season: String, round: String) {
        repository.getResults(season, round)
            .onEach {
                _results.value = it
            }.launchIn(viewModelScope)
    }
}