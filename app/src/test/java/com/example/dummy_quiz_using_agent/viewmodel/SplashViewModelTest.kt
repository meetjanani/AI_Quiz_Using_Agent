package com.example.dummy_quiz_using_agent.viewmodel

import com.example.dummy_quiz_using_agent.model.SplashInitResult
import com.example.dummy_quiz_using_agent.model.SplashUiState
import com.example.dummy_quiz_using_agent.repository.SplashRepository
import com.example.dummy_quiz_using_agent.repository.SplashResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * KAN-1: Unit tests for SplashViewModel covering AC2, AC3, AC4 navigation logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SplashRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Initializing`() = runTest {
        coEvery { repository.initialize() } returns SplashResult.Success(SplashInitResult(isLoggedIn = false))
        val viewModel = SplashViewModel(repository)
        // The very first emission before coroutine runs is Initializing
        assertEquals(SplashUiState.Initializing, viewModel.uiState.value)
    }

    @Test
    fun `AC3 - logged in user transitions to Ready with isLoggedIn true`() = runTest {
        coEvery { repository.initialize() } returns SplashResult.Success(SplashInitResult(isLoggedIn = true))
        val viewModel = SplashViewModel(repository)

        // Advance coroutine
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SplashUiState.Ready)
        assertTrue((state as SplashUiState.Ready).isLoggedIn)
    }

    @Test
    fun `AC4 - guest user transitions to Ready with isLoggedIn false`() = runTest {
        coEvery { repository.initialize() } returns SplashResult.Success(SplashInitResult(isLoggedIn = false))
        val viewModel = SplashViewModel(repository)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SplashUiState.Ready)
        assertFalse((state as SplashUiState.Ready).isLoggedIn)
    }

    @Test
    fun `failure transitions to Error with canRetry true`() = runTest {
        coEvery { repository.initialize() } returns SplashResult.Failure(
            message = "Init failed",
            canRetry = true
        )
        val viewModel = SplashViewModel(repository)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SplashUiState.Error)
        assertEquals("Init failed", (state as SplashUiState.Error).message)
        assertTrue(state.canRetry)
    }

    @Test
    fun `retry resets to Initializing then resolves`() = runTest {
        coEvery { repository.initialize() } returns SplashResult.Success(SplashInitResult(isLoggedIn = true))
        val viewModel = SplashViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.retry()
        assertEquals(SplashUiState.Initializing, viewModel.uiState.value)

        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value is SplashUiState.Ready)
    }
}

