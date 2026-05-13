package com.example.dummy_quiz_using_agent.data

import android.content.Context
import com.example.dummy_quiz_using_agent.R
import com.example.dummy_quiz_using_agent.model.Habit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

class HabitTrackerService(private val context: Context) {

    suspend fun loadHabits(): List<Habit> = withContext(Dispatchers.IO) {
        val existing = readHabitsFromStorage()
        if (existing.isNotEmpty()) {
            return@withContext existing
        }

        val defaults = DEFAULT_HABITS.map { name ->
            Habit(
                id = UUID.randomUUID().toString(),
                name = name,
                isCompleted = false
            )
        }
        writeHabitsToStorage(defaults)
        defaults
    }

    suspend fun addHabit(name: String): List<Habit> = withContext(Dispatchers.IO) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            throw HabitTrackerException.Validation(context.getString(R.string.habit_tracker_error_empty))
        }

        val currentHabits = readHabitsFromStorageOrDefault()
        if (currentHabits.size >= MAX_HABITS) {
            throw HabitTrackerException.Validation(context.getString(R.string.habit_tracker_error_max_limit))
        }

        val isDuplicate = currentHabits.any { it.name.equals(normalizedName, ignoreCase = true) }
        if (isDuplicate) {
            throw HabitTrackerException.Validation(context.getString(R.string.habit_tracker_error_duplicate))
        }

        val updatedHabits = currentHabits + Habit(
            id = UUID.randomUUID().toString(),
            name = normalizedName,
            isCompleted = false
        )
        writeHabitsToStorage(updatedHabits)
        updatedHabits
    }

    suspend fun toggleHabit(habitId: String): List<Habit> = withContext(Dispatchers.IO) {
        val currentHabits = readHabitsFromStorageOrDefault()
        val index = currentHabits.indexOfFirst { it.id == habitId }
        if (index == -1) {
            throw HabitTrackerException.Validation(context.getString(R.string.habit_tracker_error_not_found))
        }

        val updatedHabits = currentHabits.toMutableList().apply {
            val target = this[index]
            this[index] = target.copy(isCompleted = !target.isCompleted)
        }

        writeHabitsToStorage(updatedHabits)
        updatedHabits
    }

    suspend fun resetDay(): List<Habit> = withContext(Dispatchers.IO) {
        val currentHabits = readHabitsFromStorageOrDefault()
        val updatedHabits = currentHabits.map { it.copy(isCompleted = false) }
        writeHabitsToStorage(updatedHabits)
        updatedHabits
    }

    private fun readHabitsFromStorageOrDefault(): List<Habit> {
        val habits = readHabitsFromStorage()
        if (habits.isNotEmpty()) {
            return habits
        }

        val defaults = DEFAULT_HABITS.map { name ->
            Habit(
                id = UUID.randomUUID().toString(),
                name = name,
                isCompleted = false
            )
        }
        writeHabitsToStorage(defaults)
        return defaults
    }

    private fun readHabitsFromStorage(): List<Habit> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val rawJson = prefs.getString(KEY_HABITS_JSON, null) ?: return emptyList()
            if (rawJson.isBlank()) return emptyList()

            val array = JSONArray(rawJson)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val id = item.optString(JSON_KEY_ID)
                    val name = item.optString(JSON_KEY_NAME)
                    if (id.isBlank() || name.isBlank()) continue
                    add(
                        Habit(
                            id = id,
                            name = name,
                            isCompleted = item.optBoolean(JSON_KEY_COMPLETED, false)
                        )
                    )
                }
            }
        } catch (e: JSONException) {
            throw HabitTrackerException.Storage("Failed to read habits.", e)
        } catch (e: Exception) {
            throw HabitTrackerException.Unknown("Unexpected habit storage error.", e)
        }
    }

    private fun writeHabitsToStorage(habits: List<Habit>) {
        try {
            val array = JSONArray()
            habits.forEach { habit ->
                array.put(
                    JSONObject().apply {
                        put(JSON_KEY_ID, habit.id)
                        put(JSON_KEY_NAME, habit.name)
                        put(JSON_KEY_COMPLETED, habit.isCompleted)
                    }
                )
            }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.edit().putString(KEY_HABITS_JSON, array.toString()).commit()
            if (!saved) {
                throw HabitTrackerException.Storage("Failed to persist habits.")
            }
        } catch (e: HabitTrackerException) {
            throw e
        } catch (e: Exception) {
            throw HabitTrackerException.Unknown("Unexpected habit write error.", e)
        }
    }

    companion object {
        const val MAX_HABITS = 5

        private const val PREFS_NAME = "app_prefs"
        private const val KEY_HABITS_JSON = "habit_tracker_items_json"

        private const val JSON_KEY_ID = "id"
        private const val JSON_KEY_NAME = "name"
        private const val JSON_KEY_COMPLETED = "isCompleted"
    }

    private val DEFAULT_HABITS: List<String>
        get() = listOf(
            context.getString(R.string.habit_tracker_default_habit_1),
            context.getString(R.string.habit_tracker_default_habit_2),
            context.getString(R.string.habit_tracker_default_habit_3)
        )
}

sealed class HabitTrackerException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Validation(message: String) : HabitTrackerException(message)
    class Storage(message: String, cause: Throwable? = null) : HabitTrackerException(message, cause)
    class Unknown(message: String, cause: Throwable? = null) : HabitTrackerException(message, cause)
}

