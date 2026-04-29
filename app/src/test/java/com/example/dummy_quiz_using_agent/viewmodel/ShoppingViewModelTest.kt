package com.example.dummy_quiz_using_agent.viewmodel

import com.example.dummy_quiz_using_agent.model.AlternativeProduct
import com.example.dummy_quiz_using_agent.model.FakeReviewRiskLevel
import com.example.dummy_quiz_using_agent.model.ProductInsight
import com.example.dummy_quiz_using_agent.model.ShoppingPreference
import com.example.dummy_quiz_using_agent.model.ShoppingUiState
import com.example.dummy_quiz_using_agent.repository.ShoppingDecisionResult
import com.example.dummy_quiz_using_agent.repository.ShoppingRepository
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
class ShoppingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeShoppingRepository
    private lateinit var viewModel: ShoppingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeShoppingRepository()
        viewModel = ShoppingViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `analyzeProduct with invalid link shows validation error`() {
        viewModel.onProductLinkChanged("invalid-link")

        viewModel.analyzeProduct()

        val state = viewModel.uiState.value
        assertTrue(state is ShoppingUiState.Input)
        state as ShoppingUiState.Input
        assertEquals(
            "Paste a valid Amazon or Flipkart product URL to continue.",
            state.validationError
        )
    }

    @Test
    fun `analyzeProduct with valid link and successful repository emits Result`() = runTest {
        fakeRepository.result = ShoppingDecisionResult.Success(sampleInsight)
        viewModel.onProductLinkChanged("https://www.amazon.in/product")
        viewModel.onPreferenceChanged(ShoppingPreference.BATTERY_LIFE)

        viewModel.analyzeProduct()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ShoppingUiState.Result)
        state as ShoppingUiState.Result
        assertEquals("Sample Phone", state.insight.productTitle)
        assertEquals(ShoppingPreference.BATTERY_LIFE, fakeRepository.lastPreference)
    }

    @Test
    fun `analyzeProduct with repository failure emits Error`() = runTest {
        fakeRepository.result = ShoppingDecisionResult.Failure(
            message = "Network error",
            canRetry = true
        )
        viewModel.onProductLinkChanged("https://www.flipkart.com/product")

        viewModel.analyzeProduct()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ShoppingUiState.Error)
        state as ShoppingUiState.Error
        assertEquals("Network error", state.message)
        assertTrue(state.canRetry)
    }

    private class FakeShoppingRepository : ShoppingRepository {
        var result: ShoppingDecisionResult = ShoppingDecisionResult.Success(sampleInsight)
        var lastPreference: ShoppingPreference? = null

        override suspend fun analyzeProduct(
            productLink: String,
            preference: ShoppingPreference
        ): ShoppingDecisionResult {
            lastPreference = preference
            return result
        }
    }

    private companion object {
        val sampleInsight = ProductInsight(
            productTitle = "Sample Phone",
            reviewSummary = "Battery is good. Camera is average.",
            fakeReviewRiskLevel = FakeReviewRiskLevel.LOW,
            fakeReviewRiskScore = 22,
            fakeReviewSignals = listOf("Review language appears natural."),
            alternatives = listOf(
                AlternativeProduct(name = "Phone X", reason = "Better battery backup")
            ),
            personalizedSuggestion =
                "Since you prefer battery life, Phone X is the safer long-lasting option."
        )
    }
}

