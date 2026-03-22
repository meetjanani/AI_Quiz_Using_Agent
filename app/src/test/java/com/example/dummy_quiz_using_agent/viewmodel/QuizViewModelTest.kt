package com.example.dummy_quiz_using_agent.viewmodel

import com.example.dummy_quiz_using_agent.model.AnswerReview
import com.example.dummy_quiz_using_agent.model.ExperienceLevel
import com.example.dummy_quiz_using_agent.model.QuizQuestion
import com.example.dummy_quiz_using_agent.model.QuizUiState
import com.example.dummy_quiz_using_agent.model.Technology
import com.example.dummy_quiz_using_agent.repository.QuizGenerationResult
import com.example.dummy_quiz_using_agent.repository.QuizRepository
import com.example.dummy_quiz_using_agent.repository.QuizSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {

    private lateinit var repository: QuizRepository
    private lateinit var viewModel: QuizViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val twoQuestions = listOf(
        QuizQuestion(
            id = 0, question = "Q1",
            options = listOf("A", "B", "C", "D"),
            correctAnswerIndex = 0, hint = "hint1"
        ),
        QuizQuestion(
            id = 1, question = "Q2",
            options = listOf("A", "B", "C", "D"),
            correctAnswerIndex = 1, hint = "hint2"
        )
    )

    private val successResult = QuizGenerationResult.Success(twoQuestions, QuizSource.AI)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = QuizViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial uiState is Setup with default values`() {
        val state = viewModel.uiState.value
        assertTrue(state is QuizUiState.Setup)
        state as QuizUiState.Setup
        assertEquals(Technology.ANDROID, state.selectedTechnology)
        assertEquals(ExperienceLevel.BEGINNER, state.selectedExperienceLevel)
        assertEquals(5, state.questionCount)
    }

    // ── Setup state mutations ─────────────────────────────────────────────────

    @Test
    fun `onTechnologySelected updates selected technology`() {
        viewModel.onTechnologySelected(Technology.KOTLIN)

        val state = viewModel.uiState.value as QuizUiState.Setup
        assertEquals(Technology.KOTLIN, state.selectedTechnology)
    }

    @Test
    fun `onExperienceLevelSelected updates selected experience level`() {
        viewModel.onExperienceLevelSelected(ExperienceLevel.ADVANCED)

        val state = viewModel.uiState.value as QuizUiState.Setup
        assertEquals(ExperienceLevel.ADVANCED, state.selectedExperienceLevel)
    }

    @Test
    fun `onQuestionCountSelected stores valid count`() {
        viewModel.onQuestionCountSelected(7)

        val state = viewModel.uiState.value as QuizUiState.Setup
        assertEquals(7, state.questionCount)
    }

    @Test
    fun `onQuestionCountSelected coerces count below minimum to 3`() {
        viewModel.onQuestionCountSelected(0)

        val state = viewModel.uiState.value as QuizUiState.Setup
        assertEquals(3, state.questionCount)
    }

    @Test
    fun `onQuestionCountSelected coerces count above maximum to 10`() {
        viewModel.onQuestionCountSelected(99)

        val state = viewModel.uiState.value as QuizUiState.Setup
        assertEquals(10, state.questionCount)
    }

    @Test
    fun `onQuestionCountSelected accepts boundary values 3 and 10`() {
        viewModel.onQuestionCountSelected(3)
        assertEquals(3, (viewModel.uiState.value as QuizUiState.Setup).questionCount)

        viewModel.onQuestionCountSelected(10)
        assertEquals(10, (viewModel.uiState.value as QuizUiState.Setup).questionCount)
    }

    // ── generateQuiz ──────────────────────────────────────────────────────────

    @Test
    fun `generateQuiz transitions to Loading immediately`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), any()) } returns successResult

        viewModel.generateQuiz()

        // Before coroutine runs: Loading
        assertTrue(viewModel.uiState.value is QuizUiState.Loading)
    }

    @Test
    fun `generateQuiz transitions to InProgress on Success`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), any()) } returns successResult

        viewModel.generateQuiz()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is QuizUiState.InProgress)
    }

    @Test
    fun `generateQuiz InProgress carries correct questions`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), any()) } returns successResult

        viewModel.generateQuiz()
        advanceUntilIdle()

        val state = viewModel.uiState.value as QuizUiState.InProgress
        assertEquals(twoQuestions, state.questions)
    }

    @Test
    fun `generateQuiz transitions to Error on Failure`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), any()) } returns
            QuizGenerationResult.Failure("API error", canRetry = true, canUseFallback = true)

        viewModel.generateQuiz()
        advanceUntilIdle()

        val state = viewModel.uiState.value as QuizUiState.Error
        assertEquals("API error", state.message)
        assertTrue(state.canRetry)
        assertTrue(state.canUseFallback)
    }

    @Test
    fun `generateQuiz passes allowFallback=false by default`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), allowFallback = false) } returns successResult

        viewModel.generateQuiz()
        advanceUntilIdle()

        coVerify { repository.generateQuiz(any(), any(), any(), allowFallback = false) }
    }

    @Test
    fun `useSampleQuiz passes allowFallback=true`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), allowFallback = true) } returns successResult

        viewModel.useSampleQuiz()
        advanceUntilIdle()

        coVerify { repository.generateQuiz(any(), any(), any(), allowFallback = true) }
    }

    @Test
    fun `retryQuizGeneration calls generateQuiz with allowFallback=false`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), any()) } returns successResult

        viewModel.retryQuizGeneration()
        advanceUntilIdle()

        coVerify { repository.generateQuiz(any(), any(), any(), allowFallback = false) }
    }

    // ── In-progress interactions ──────────────────────────────────────────────

    @Test
    fun `selectAnswer stores selected answer for current question`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), any()) } returns successResult

        viewModel.generateQuiz()
        advanceUntilIdle()

        viewModel.selectAnswer(2)

        val state = viewModel.uiState.value as QuizUiState.InProgress
        assertEquals(2, state.selectedAnswers[0])
    }

    @Test
    fun `selectAnswer for second question does not affect first question answer`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), any()) } returns successResult

        viewModel.generateQuiz()
        advanceUntilIdle()

        viewModel.selectAnswer(1)          // answer Q0
        viewModel.goToNextQuestion()
        viewModel.selectAnswer(3)          // answer Q1

        val state = viewModel.uiState.value as QuizUiState.InProgress
        assertEquals(1, state.selectedAnswers[0])
        assertEquals(3, state.selectedAnswers[1])
    }

    @Test
    fun `goToNextQuestion advances currentQuestionIndex by 1`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), any()) } returns successResult

        viewModel.generateQuiz()
        advanceUntilIdle()

        viewModel.goToNextQuestion()

        val state = viewModel.uiState.value as QuizUiState.InProgress
        assertEquals(1, state.currentQuestionIndex)
    }

    @Test
    fun `goToNextQuestion does not exceed last question index`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), any()) } returns successResult

        viewModel.generateQuiz()
        advanceUntilIdle()

        repeat(20) { viewModel.goToNextQuestion() }

        val state = viewModel.uiState.value as QuizUiState.InProgress
        assertEquals(twoQuestions.lastIndex, state.currentQuestionIndex)
    }

    @Test
    fun `goToPreviousQuestion does not go below index 0`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), any()) } returns successResult

        viewModel.generateQuiz()
        advanceUntilIdle()

        repeat(10) { viewModel.goToPreviousQuestion() }

        val state = viewModel.uiState.value as QuizUiState.InProgress
        assertEquals(0, state.currentQuestionIndex)
    }

    @Test
    fun `goToNextQuestion then goToPreviousQuestion returns to index 0`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), any()) } returns successResult

        viewModel.generateQuiz()
        advanceUntilIdle()

        viewModel.goToNextQuestion()
        viewModel.goToPreviousQuestion()

        val state = viewModel.uiState.value as QuizUiState.InProgress
        assertEquals(0, state.currentQuestionIndex)
    }

    // ── submitQuiz ────────────────────────────────────────────────────────────

    @Test
    fun `submitQuiz transitions state to Results`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), any()) } returns successResult

        viewModel.generateQuiz()
        advanceUntilIdle()
        viewModel.submitQuiz()

        assertTrue(viewModel.uiState.value is QuizUiState.Results)
    }

    @Test
    fun `submitQuiz counts all correct when all answers correct`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), any()) } returns successResult

        viewModel.generateQuiz()
        advanceUntilIdle()

        viewModel.selectAnswer(0) // Q0 correct = 0
        viewModel.goToNextQuestion()
        viewModel.selectAnswer(1) // Q1 correct = 1
        viewModel.submitQuiz()

        val results = viewModel.uiState.value as QuizUiState.Results
        assertEquals(2, results.totalQuestions)
        assertEquals(2, results.correctAnswers)
        assertTrue(results.incorrectAnswers.isEmpty())
    }

    @Test
    fun `submitQuiz counts incorrect answers properly`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), any()) } returns successResult

        viewModel.generateQuiz()
        advanceUntilIdle()

        viewModel.selectAnswer(3) // Q0 wrong (correct = 0)
        viewModel.goToNextQuestion()
        viewModel.selectAnswer(3) // Q1 wrong (correct = 1)
        viewModel.submitQuiz()

        val results = viewModel.uiState.value as QuizUiState.Results
        assertEquals(0, results.correctAnswers)
        assertEquals(2, results.incorrectAnswers.size)
    }

    @Test
    fun `submitQuiz includes unanswered questions as incorrect`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), any()) } returns successResult

        viewModel.generateQuiz()
        advanceUntilIdle()

        // Do not answer either question — both should appear as incorrect with null selection
        viewModel.submitQuiz()

        val results = viewModel.uiState.value as QuizUiState.Results
        assertEquals(0, results.correctAnswers)
        assertEquals(2, results.incorrectAnswers.size)
        results.incorrectAnswers.forEach { answerReview: AnswerReview ->
            assertNull(answerReview.selectedAnswerIndex)
        }
    }

    // ── restartQuiz ───────────────────────────────────────────────────────────

    @Test
    fun `restartQuiz returns to last Setup state`() = runTest {
        viewModel.onTechnologySelected(Technology.PYTHON)
        viewModel.onExperienceLevelSelected(ExperienceLevel.ADVANCED)

        coEvery { repository.generateQuiz(any(), any(), any(), any()) } returns successResult

        viewModel.generateQuiz()
        advanceUntilIdle()
        viewModel.restartQuiz()

        val state = viewModel.uiState.value as QuizUiState.Setup
        assertEquals(Technology.PYTHON, state.selectedTechnology)
        assertEquals(ExperienceLevel.ADVANCED, state.selectedExperienceLevel)
    }

    @Test
    fun `restartQuiz after submit returns to Setup`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), any()) } returns successResult

        viewModel.generateQuiz()
        advanceUntilIdle()
        viewModel.submitQuiz()
        viewModel.restartQuiz()

        assertTrue(viewModel.uiState.value is QuizUiState.Setup)
    }

    // ── isFallbackQuiz flag ───────────────────────────────────────────────────

    @Test
    fun `generateQuiz with fallback result marks isFallbackQuiz true`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), any()) } returns
            QuizGenerationResult.Success(twoQuestions, QuizSource.FALLBACK)

        viewModel.generateQuiz()
        advanceUntilIdle()

        val state = viewModel.uiState.value as QuizUiState.InProgress
        assertTrue(state.isFallbackQuiz)
    }

    @Test
    fun `generateQuiz with AI result marks isFallbackQuiz false`() = runTest {
        coEvery { repository.generateQuiz(any(), any(), any(), any()) } returns successResult

        viewModel.generateQuiz()
        advanceUntilIdle()

        val state = viewModel.uiState.value as QuizUiState.InProgress
        assertFalse(state.isFallbackQuiz)
    }
}

