package com.example.dummy_quiz_using_agent.model

/** KAN-2: Home screen stats/insights shown in footer cards. */
data class HomeStats(
    val lastQuizScorePercent: Int?,
    val recentShoppingHint: String?,
    val quickTip: String
)

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(val stats: HomeStats) : HomeUiState

    data class Error(
        val message: String,
        val canRetry: Boolean,
        val fallbackStats: HomeStats
    ) : HomeUiState
}

