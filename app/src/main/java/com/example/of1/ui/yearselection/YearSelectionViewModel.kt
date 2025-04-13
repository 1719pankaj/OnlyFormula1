package com.example.of1.ui.yearselection // Renamed package

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class YearSelectionViewModel @Inject constructor() : ViewModel() { // Inject nothing

    private val _years = MutableStateFlow<List<String>>(emptyList())
    val years: StateFlow<List<String>> = _years

    init {
        generateYears()
    }

    private fun generateYears() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val yearList = (currentYear downTo 1950).map { it.toString() }
        _years.value = yearList
    }
}