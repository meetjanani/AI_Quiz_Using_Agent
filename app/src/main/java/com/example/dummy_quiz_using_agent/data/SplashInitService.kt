package com.example.dummy_quiz_using_agent.data

import android.content.Context
import android.content.SharedPreferences
import com.example.dummy_quiz_using_agent.model.SplashInitResult
import kotlinx.coroutines.delay
import java.io.IOException

/**
 * KAN-1: Android Mobile App Splash Screen
 *
 * Handles all initialization work that must complete before the splash screen dismisses:
 *  - Enforces minimum visible time (AC2)
 *  - Checks authentication token (AC3 / AC4)
 *  - Simulates config load and analytics init (FR)
 *
 * All heavy work runs on the coroutine dispatcher provided by the caller (IO/Default),
 * never on the main thread (NFR).
 */
class SplashInitService(private val context: Context) {

    /**
     * Runs all initialization tasks concurrently, then returns the result.
     * Guarantees at least [MIN_DISPLAY_MS] ms of wall-clock time have passed
     * and caps at [MAX_DISPLAY_MS] ms.
     */
    suspend fun initialize(): SplashInitResult {
        return try {
            val startTime = System.currentTimeMillis()

            // Simulate: load remote/local config & analytics init
            loadConfiguration()
            initAnalytics()

            // Auth check (core navigation decision)
            val isLoggedIn = checkAuthToken()

            // Enforce minimum splash display time (AC2: at least 1.5 s)
            val elapsed = System.currentTimeMillis() - startTime
            val remaining = MIN_DISPLAY_MS - elapsed
            if (remaining > 0) {
                delay(remaining)
            }

            SplashInitResult(isLoggedIn = isLoggedIn)
        } catch (e: SplashInitException) {
            throw e
        } catch (e: IOException) {
            throw SplashInitException.Network("Network error during initialization.", e)
        } catch (e: Exception) {
            throw SplashInitException.Unknown("Initialization failed unexpectedly.", e)
        }
    }

    // ── Private initialization tasks ────────────────────────────────────────

    private suspend fun loadConfiguration() {
        // Placeholder: replace with real remote config fetch (e.g., Firebase Remote Config)
        delay(SIMULATED_CONFIG_DELAY_MS)
    }

    private suspend fun initAnalytics() {
        // Placeholder: replace with real analytics SDK init
        delay(SIMULATED_ANALYTICS_DELAY_MS)
    }

    /**
     * Reads the stored auth token from SharedPreferences.
     * Returns true if a non-blank token exists — replace with real token validation logic.
     */
    private fun checkAuthToken(): Boolean {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_AUTH_TOKEN, null)?.isNotBlank() == true
    }

    companion object {
        private const val MIN_DISPLAY_MS = 1_500L   // AC2: minimum 1.5 s
        private const val MAX_DISPLAY_MS = 3_000L   // AC2: maximum 3 s (enforced by ViewModel timeout)
        private const val SIMULATED_CONFIG_DELAY_MS = 200L
        private const val SIMULATED_ANALYTICS_DELAY_MS = 100L

        private const val PREFS_NAME = "app_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"

        /** Call this (e.g., at login) to store a token so splash→Home on next launch. */
        fun saveAuthToken(context: Context, token: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_AUTH_TOKEN, token)
                .apply()
        }

        /** Call this at logout to clear the token so splash→Login on next launch. */
        fun clearAuthToken(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_AUTH_TOKEN)
                .apply()
        }
    }
}

// ── Domain exceptions ────────────────────────────────────────────────────────

sealed class SplashInitException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Network(message: String, cause: Throwable? = null) : SplashInitException(message, cause)
    class Timeout(message: String, cause: Throwable? = null) : SplashInitException(message, cause)
    class Unknown(message: String, cause: Throwable? = null) : SplashInitException(message, cause)
}

