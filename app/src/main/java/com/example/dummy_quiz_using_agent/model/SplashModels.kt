package com.example.dummy_quiz_using_agent.model

/**
 * KAN-1: Android Mobile App Splash Screen
 * Represents the outcome of the splash initialization tasks.
 */
data class SplashInitResult(
    val isLoggedIn: Boolean
)

/**
 * UiState for the splash screen — follows sealed interface pattern used across this project.
 */
sealed interface SplashUiState {
    /** Initialization still in progress — keep splash visible. */
    data object Initializing : SplashUiState

    /** Initialization completed. Navigate based on [isLoggedIn]. */
    data class Ready(val isLoggedIn: Boolean) : SplashUiState

    /** Something went wrong during initialization. */
    data class Error(val message: String, val canRetry: Boolean = true) : SplashUiState
}

