package com.example.dummy_quiz_using_agent.model

data class Habit(
    val id: String,
    val name: String,
    val isCompleted: Boolean
)

data class HabitTrackerData(
    val habits: List<Habit>,
    val maxHabits: Int
) {
    val completedCount: Int
        get() = habits.count { it.isCompleted }

    val totalCount: Int
        get() = habits.size

    val progress: Float
        get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()
}

sealed interface HabitTrackerUiState {
    data object Loading : HabitTrackerUiState

    data class Success(
        val data: HabitTrackerData,
        val pendingHabitName: String = "",
        val inputError: String? = null
    ) : HabitTrackerUiState

    data class Error(
        val message: String,
        val canRetry: Boolean
    ) : HabitTrackerUiState
}

