package com.example.dummy_quiz_using_agent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dummy_quiz_using_agent.model.SplashUiState
import com.example.dummy_quiz_using_agent.repository.DefaultSplashRepository
import com.example.dummy_quiz_using_agent.repository.SplashRepository
import com.example.dummy_quiz_using_agent.repository.SplashResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * KAN-1: Android Mobile App Splash Screen
 *
 * Drives initialization and exposes [SplashUiState] to the Compose UI.
 * Enforces the 3-second max timeout from acceptance criteria (AC2).
 */
class SplashViewModel(
    private val repository: SplashRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Initializing)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        initialize()
    }

    fun initialize() {
        _uiState.value = SplashUiState.Initializing
        viewModelScope.launch {
            // Cap the entire initialization at MAX_TIMEOUT_MS (AC2: max 3 s)
            val result = withTimeoutOrNull(MAX_TIMEOUT_MS) {
                repository.initialize()
            }

            _uiState.value = when {
                result == null -> SplashUiState.Error(
                    message = "Initialization timed out. Please retry.",
                    canRetry = true
                )
                result is SplashResult.Success -> SplashUiState.Ready(
                    isLoggedIn = result.data.isLoggedIn
                )
                result is SplashResult.Failure -> SplashUiState.Error(
                    message = result.message,
                    canRetry = result.canRetry
                )
                else -> SplashUiState.Error("Unexpected error during initialization.", canRetry = true)
            }
        }
    }

    fun retry() {
        initialize()
    }

    companion object {
        private const val MAX_TIMEOUT_MS = 3_000L  // AC2: maximum 3 seconds

        fun provideFactory(repository: SplashRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
                        return SplashViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}

