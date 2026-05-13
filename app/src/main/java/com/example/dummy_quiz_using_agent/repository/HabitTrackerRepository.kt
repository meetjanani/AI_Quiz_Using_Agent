package com.example.dummy_quiz_using_agent.repository

import com.example.dummy_quiz_using_agent.data.HabitTrackerException
import com.example.dummy_quiz_using_agent.data.HabitTrackerService
import com.example.dummy_quiz_using_agent.model.Habit
import com.example.dummy_quiz_using_agent.model.HabitTrackerData

interface HabitTrackerRepository {
    suspend fun loadHabits(): HabitTrackerResult
    suspend fun addHabit(name: String): HabitTrackerResult
    suspend fun toggleHabit(habitId: String): HabitTrackerResult
    suspend fun resetDay(): HabitTrackerResult
}

class DefaultHabitTrackerRepository(
    private val service: HabitTrackerService
) : HabitTrackerRepository {

    override suspend fun loadHabits(): HabitTrackerResult {
        return performRequest { service.loadHabits() }
    }

    override suspend fun addHabit(name: String): HabitTrackerResult {
        return performRequest { service.addHabit(name) }
    }

    override suspend fun toggleHabit(habitId: String): HabitTrackerResult {
        return performRequest { service.toggleHabit(habitId) }
    }

    override suspend fun resetDay(): HabitTrackerResult {
        return performRequest { service.resetDay() }
    }

    private suspend fun performRequest(
        block: suspend () -> List<Habit>
    ): HabitTrackerResult {
        return try {
            HabitTrackerResult.Success(
                data = HabitTrackerData(
                    habits = block(),
                    maxHabits = HabitTrackerService.MAX_HABITS
                )
            )
        } catch (error: HabitTrackerException.Validation) {
            HabitTrackerResult.Failure(
                message = error.message ?: DEFAULT_ERROR_MESSAGE,
                canRetry = false
            )
        } catch (error: HabitTrackerException) {
            HabitTrackerResult.Failure(
                message = error.message ?: DEFAULT_ERROR_MESSAGE,
                canRetry = true
            )
        }
    }

    private companion object {
        private const val DEFAULT_ERROR_MESSAGE =
            "Could not update habits right now. Please try again."
    }
}

sealed interface HabitTrackerResult {
    data class Success(val data: HabitTrackerData) : HabitTrackerResult

    data class Failure(
        val message: String,
        val canRetry: Boolean
    ) : HabitTrackerResult
}

