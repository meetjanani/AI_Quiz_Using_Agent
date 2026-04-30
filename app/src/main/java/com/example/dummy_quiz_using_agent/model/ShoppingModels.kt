package com.example.dummy_quiz_using_agent.model

enum class ShoppingPreference(
    val displayName: String,
    val promptHint: String
) {
    BATTERY_LIFE(
        displayName = "Battery life",
        promptHint = "Prioritize products with strong battery performance and standby efficiency."
    ),
    CAMERA_QUALITY(
        displayName = "Camera quality",
        promptHint = "Prioritize camera consistency, low-light quality, and realistic color output."
    ),
    VALUE_FOR_MONEY(
        displayName = "Value for money",
        promptHint = "Prioritize practical features at a fair price instead of premium extras."
    ),
    PERFORMANCE(
        displayName = "Performance",
        promptHint = "Prioritize smooth day-to-day performance and long-term responsiveness."
    )
}

enum class FakeReviewRiskLevel {
    LOW,
    MEDIUM,
    HIGH;

    companion object {
        fun fromRaw(raw: String): FakeReviewRiskLevel {
            return when (raw.trim().uppercase()) {
                "LOW" -> LOW
                "HIGH" -> HIGH
                else -> MEDIUM
            }
        }
    }
}

data class AlternativeProduct(
    val name: String,
    val reason: String
)

data class ProductInsight(
    val productTitle: String,
    val reviewSummary: String,
    val fakeReviewRiskLevel: FakeReviewRiskLevel,
    val fakeReviewRiskScore: Int,
    val fakeReviewSignals: List<String>,
    val alternatives: List<AlternativeProduct>,
    val personalizedSuggestion: String
)

sealed interface ShoppingUiState {
    data class Input(
        val productLink: String = "",
        val selectedPreference: ShoppingPreference = ShoppingPreference.BATTERY_LIFE,
        val validationError: String? = null
    ) : ShoppingUiState

    data class Loading(val message: String = "Analyzing product reviews...") : ShoppingUiState

    data class Result(val insight: ProductInsight) : ShoppingUiState

    data class Error(
        val message: String,
        val canRetry: Boolean
    ) : ShoppingUiState
}

