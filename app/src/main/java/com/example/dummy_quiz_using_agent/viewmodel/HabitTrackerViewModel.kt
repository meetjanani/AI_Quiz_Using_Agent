package com.example.dummy_quiz_using_agent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dummy_quiz_using_agent.model.HabitTrackerData
import com.example.dummy_quiz_using_agent.model.HabitTrackerUiState
import com.example.dummy_quiz_using_agent.repository.HabitTrackerRepository
import com.example.dummy_quiz_using_agent.repository.HabitTrackerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HabitTrackerViewModel(
    private val repository: HabitTrackerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HabitTrackerUiState>(HabitTrackerUiState.Loading)
    val uiState: StateFlow<HabitTrackerUiState> = _uiState.asStateFlow()

    private var lastLoadedData: HabitTrackerData? = null

    init {
        loadHabits()
    }

    fun onHabitNameChanged(name: String) {
        _uiState.update { state ->
            val success = state as? HabitTrackerUiState.Success ?: return@update state
            success.copy(
                pendingHabitName = name,
                inputError = null
            )
        }
    }

    fun loadHabits() {
        _uiState.value = HabitTrackerUiState.Loading
        viewModelScope.launch {
            when (val result = repository.loadHabits()) {
                is HabitTrackerResult.Success -> {
                    lastLoadedData = result.data
                    _uiState.value = HabitTrackerUiState.Success(data = result.data)
                }

                is HabitTrackerResult.Failure -> {
                    _uiState.value = HabitTrackerUiState.Error(
                        message = result.message,
                        canRetry = result.canRetry
                    )
                }
            }
        }
    }

    fun addHabit() {
        val currentState = _uiState.value as? HabitTrackerUiState.Success ?: return
        val name = currentState.pendingHabitName.trim()

        viewModelScope.launch {
            when (val result = repository.addHabit(name)) {
                is HabitTrackerResult.Success -> {
                    lastLoadedData = result.data
                    _uiState.value = currentState.copy(
                        data = result.data,
                        pendingHabitName = "",
                        inputError = null
                    )
                }

                is HabitTrackerResult.Failure -> {
                    if (result.canRetry) {
                        _uiState.value = HabitTrackerUiState.Error(
                            message = result.message,
                            canRetry = true
                        )
                    } else {
                        _uiState.value = currentState.copy(
                            pendingHabitName = currentState.pendingHabitName,
                            inputError = result.message
                        )
                    }
                }
            }
        }
    }

    fun toggleHabit(habitId: String) {
        val currentState = _uiState.value as? HabitTrackerUiState.Success ?: return

        viewModelScope.launch {
            when (val result = repository.toggleHabit(habitId)) {
                is HabitTrackerResult.Success -> {
                    lastLoadedData = result.data
                    _uiState.value = currentState.copy(data = result.data)
                }

                is HabitTrackerResult.Failure -> {
                    _uiState.value = HabitTrackerUiState.Error(
                        message = result.message,
                        canRetry = result.canRetry
                    )
                }
            }
        }
    }

    fun resetDay() {
        val currentState = _uiState.value as? HabitTrackerUiState.Success ?: return

        viewModelScope.launch {
            when (val result = repository.resetDay()) {
                is HabitTrackerResult.Success -> {
                    lastLoadedData = result.data
                    _uiState.value = currentState.copy(data = result.data)
                }

                is HabitTrackerResult.Failure -> {
                    _uiState.value = HabitTrackerUiState.Error(
                        message = result.message,
                        canRetry = result.canRetry
                    )
                }
            }
        }
    }

    fun retry() {
        if (_uiState.value is HabitTrackerUiState.Error && lastLoadedData != null) {
            _uiState.value = HabitTrackerUiState.Success(data = lastLoadedData!!)
            return
        }
        loadHabits()
    }

    companion object {
        fun provideFactory(repository: HabitTrackerRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(HabitTrackerViewModel::class.java)) {
                        return HabitTrackerViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

