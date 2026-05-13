package com.example.dummy_quiz_using_agent.viewmodel

import com.example.dummy_quiz_using_agent.data.HabitTrackerService
import com.example.dummy_quiz_using_agent.model.Habit
import com.example.dummy_quiz_using_agent.model.HabitTrackerData
import com.example.dummy_quiz_using_agent.model.HabitTrackerUiState
import com.example.dummy_quiz_using_agent.repository.HabitTrackerRepository
import com.example.dummy_quiz_using_agent.repository.HabitTrackerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HabitTrackerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeHabitTrackerRepository
    private lateinit var viewModel: HabitTrackerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeHabitTrackerRepository()
        viewModel = HabitTrackerViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadHabits success emits Success state`() = runTest {
        fakeRepository.loadResult = HabitTrackerResult.Success(sampleData)

        viewModel.loadHabits()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HabitTrackerUiState.Success)
        state as HabitTrackerUiState.Success
        assertEquals(2, state.data.totalCount)
        assertEquals(1, state.data.completedCount)
    }

    @Test
    fun `addHabit validation error stays in Success with input message`() = runTest {
        fakeRepository.loadResult = HabitTrackerResult.Success(sampleData)
        fakeRepository.addResult = HabitTrackerResult.Failure(
            message = "This habit already exists.",
            canRetry = false
        )

        viewModel.loadHabits()
        advanceUntilIdle()

        viewModel.onHabitNameChanged("Drink Water")
        viewModel.addHabit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HabitTrackerUiState.Success)
        state as HabitTrackerUiState.Success
        assertEquals("This habit already exists.", state.inputError)
    }

    @Test
    fun `toggleHabit success updates completion state`() = runTest {
        val toggledData = HabitTrackerData(
            habits = listOf(
                Habit(id = "1", name = "Drink Water", isCompleted = true),
                Habit(id = "2", name = "Walk", isCompleted = true)
            ),
            maxHabits = HabitTrackerService.MAX_HABITS
        )
        fakeRepository.loadResult = HabitTrackerResult.Success(sampleData)
        fakeRepository.toggleResult = HabitTrackerResult.Success(toggledData)

        viewModel.loadHabits()
        advanceUntilIdle()

        viewModel.toggleHabit("1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HabitTrackerUiState.Success)
        state as HabitTrackerUiState.Success
        assertEquals(2, state.data.completedCount)
    }

    private class FakeHabitTrackerRepository : HabitTrackerRepository {
        var loadResult: HabitTrackerResult = HabitTrackerResult.Success(sampleData)
        var addResult: HabitTrackerResult = HabitTrackerResult.Success(sampleData)
        var toggleResult: HabitTrackerResult = HabitTrackerResult.Success(sampleData)
        var resetResult: HabitTrackerResult = HabitTrackerResult.Success(sampleData)

        override suspend fun loadHabits(): HabitTrackerResult = loadResult

        override suspend fun addHabit(name: String): HabitTrackerResult = addResult

        override suspend fun toggleHabit(habitId: String): HabitTrackerResult = toggleResult

        override suspend fun resetDay(): HabitTrackerResult = resetResult
    }

    private companion object {
        val sampleData = HabitTrackerData(
            habits = listOf(
                Habit(id = "1", name = "Drink Water", isCompleted = false),
                Habit(id = "2", name = "Walk", isCompleted = true)
            ),
            maxHabits = HabitTrackerService.MAX_HABITS
        )
    }
}

