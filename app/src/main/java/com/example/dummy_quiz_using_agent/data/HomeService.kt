package com.example.dummy_quiz_using_agent.data

import android.content.Context
import com.example.dummy_quiz_using_agent.model.HomeStats
import kotlinx.coroutines.delay
import java.io.IOException

class HomeService(private val context: Context) {

    suspend fun loadHomeStats(): HomeStats {
        return try {
            // Keep this tiny to meet KAN-2 performance (< 1s after splash).
            delay(120)

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastScore = prefs.getInt(KEY_LAST_QUIZ_SCORE, -1).takeIf { it in 0..100 }
            val recentHint = prefs.getString(KEY_RECENT_SHOPPING_QUERY, null)?.takeIf { it.isNotBlank() }

            HomeStats(
                lastQuizScorePercent = lastScore,
                recentShoppingHint = recentHint,
                quickTip = DEFAULT_TIP
            )
        } catch (e: IOException) {
            throw HomeServiceException.Network("Unable to load home data.", e)
        } catch (e: Exception) {
            throw HomeServiceException.Unknown("Unexpected home data error.", e)
        }
    }

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_LAST_QUIZ_SCORE = "last_quiz_score"
        private const val KEY_RECENT_SHOPPING_QUERY = "recent_shopping_query"
        private const val DEFAULT_TIP = "Try one quiz and one product analysis every day to improve faster."
    }
}

sealed class HomeServiceException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Network(message: String, cause: Throwable? = null) : HomeServiceException(message, cause)
    class Unknown(message: String, cause: Throwable? = null) : HomeServiceException(message, cause)
}

