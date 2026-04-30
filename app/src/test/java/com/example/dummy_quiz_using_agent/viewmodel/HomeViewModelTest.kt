package com.example.dummy_quiz_using_agent.viewmodel

import com.example.dummy_quiz_using_agent.model.HomeStats
import com.example.dummy_quiz_using_agent.model.HomeUiState
import com.example.dummy_quiz_using_agent.repository.HomeRepository
import com.example.dummy_quiz_using_agent.repository.HomeResult
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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeHomeRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeHomeRepository()
        viewModel = HomeViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadHome success emits Success state`() = runTest {
        fakeRepository.result = HomeResult.Success(
            HomeStats(lastQuizScorePercent = 82, recentShoppingHint = "Pixel 9", quickTip = "Tip")
        )

        viewModel.loadHome()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Success)
        state as HomeUiState.Success
        assertEquals(82, state.stats.lastQuizScorePercent)
        assertEquals("Pixel 9", state.stats.recentShoppingHint)
    }

    @Test
    fun `loadHome failure emits Error with fallback stats`() = runTest {
        fakeRepository.result = HomeResult.Failure(
            message = "Home failed",
            canRetry = true
        )

        viewModel.loadHome()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Error)
        state as HomeUiState.Error
        assertEquals("Home failed", state.message)
        assertTrue(state.canRetry)
        assertTrue(state.fallbackStats.quickTip.isNotBlank())
    }

    @Test
    fun `retry reloads and resolves success`() = runTest {
        fakeRepository.result = HomeResult.Failure("Temporary", canRetry = true)
        viewModel.loadHome()
        advanceUntilIdle()

        fakeRepository.result = HomeResult.Success(
            HomeStats(lastQuizScorePercent = 90, recentShoppingHint = null, quickTip = "Recovered")
        )

        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Success)
        state as HomeUiState.Success
        assertEquals(90, state.stats.lastQuizScorePercent)
    }

    private class FakeHomeRepository : HomeRepository {
        var result: HomeResult = HomeResult.Success(
            HomeStats(lastQuizScorePercent = null, recentShoppingHint = null, quickTip = "Default")
        )

        override suspend fun loadHome(): HomeResult = result
    }
}

