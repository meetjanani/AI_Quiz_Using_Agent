package com.example.dummy_quiz_using_agent.repository

import com.example.dummy_quiz_using_agent.data.GeminiService
import com.example.dummy_quiz_using_agent.data.GeminiServiceException
import com.example.dummy_quiz_using_agent.model.ExperienceLevel
import com.example.dummy_quiz_using_agent.model.QuizQuestion
import com.example.dummy_quiz_using_agent.model.Technology
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuizRepositoryTest {

    private lateinit var geminiService: GeminiService
    private lateinit var repository: QuizRepository

    private val fakeQuestions = List(5) { index ->
        QuizQuestion(
            id = index,
            question = "Question $index",
            options = listOf("A", "B", "C", "D"),
            correctAnswerIndex = index % 4,
            hint = "Hint $index"
        )
    }

    @Before
    fun setUp() {
        geminiService = mockk()
        repository = QuizRepository(geminiService)
    }

    // ── Success path ──────────────────────────────────────────────────────────

    @Test
    fun `generateQuiz returns Success with AI source on service success`() = runTest {
        coEvery { geminiService.generateQuiz(any(), any(), any()) } returns fakeQuestions

        val result = repository.generateQuiz(
            technology = Technology.KOTLIN,
            experienceLevel = ExperienceLevel.BEGINNER,
            questionCount = 5,
            allowFallback = false
        )

        assertTrue(result is QuizGenerationResult.Success)
        val success = result as QuizGenerationResult.Success
        assertEquals(QuizSource.AI, success.source)
        assertEquals(5, success.questions.size)
    }

    @Test
    fun `generateQuiz calls service with the exact requested question count`() = runTest {
        coEvery { geminiService.generateQuiz(any(), any(), any()) } returns fakeQuestions

        repository.generateQuiz(
            technology = Technology.ANDROID,
            experienceLevel = ExperienceLevel.INTERMEDIATE,
            questionCount = 3,
            allowFallback = false
        )

        // Repository delegates count responsibility to the service
        coVerify { geminiService.generateQuiz(any(), any(), 3) }
    }

    @Test
    fun `generateQuiz calls service with correct parameters`() = runTest {
        coEvery {
            geminiService.generateQuiz(Technology.PYTHON, ExperienceLevel.ADVANCED, 4)
        } returns fakeQuestions.take(4)

        repository.generateQuiz(
            technology = Technology.PYTHON,
            experienceLevel = ExperienceLevel.ADVANCED,
            questionCount = 4,
            allowFallback = false
        )

        coVerify { geminiService.generateQuiz(Technology.PYTHON, ExperienceLevel.ADVANCED, 4) }
    }

    // ── Fallback path ─────────────────────────────────────────────────────────

    @Test
    fun `generateQuiz returns fallback Success when Network error and allowFallback true`() = runTest {
        coEvery { geminiService.generateQuiz(any(), any(), any()) } throws
            GeminiServiceException.Network("network down")

        val result = repository.generateQuiz(
            technology = Technology.ANDROID,
            experienceLevel = ExperienceLevel.BEGINNER,
            questionCount = 3,
            allowFallback = true
        )

        assertTrue(result is QuizGenerationResult.Success)
        val success = result as QuizGenerationResult.Success
        assertEquals(QuizSource.FALLBACK, success.source)
        assertEquals(3, success.questions.size)
    }

    @Test
    fun `generateQuiz returns fallback Success when Timeout error and allowFallback true`() = runTest {
        coEvery { geminiService.generateQuiz(any(), any(), any()) } throws
            GeminiServiceException.Timeout("timeout")

        val result = repository.generateQuiz(
            technology = Technology.WEB,
            experienceLevel = ExperienceLevel.INTERMEDIATE,
            questionCount = 5,
            allowFallback = true
        )

        val success = result as QuizGenerationResult.Success
        assertEquals(QuizSource.FALLBACK, success.source)
    }

    @Test
    fun `fallback questions count matches exactly the requested count`() = runTest {
        coEvery { geminiService.generateQuiz(any(), any(), any()) } throws
            GeminiServiceException.Network("error")

        for (count in listOf(1, 3, 5, 7, 10)) {
            val result = repository.generateQuiz(
                technology = Technology.JAVA,
                experienceLevel = ExperienceLevel.ADVANCED,
                questionCount = count,
                allowFallback = true
            )
            val success = result as QuizGenerationResult.Success
            assertEquals("Expected $count fallback questions", count, success.questions.size)
        }
    }

    @Test
    fun `fallback questions have unique sequential ids`() = runTest {
        coEvery { geminiService.generateQuiz(any(), any(), any()) } throws
            GeminiServiceException.Network("error")

        val result = repository.generateQuiz(
            technology = Technology.ANDROID,
            experienceLevel = ExperienceLevel.BEGINNER,
            questionCount = 5,
            allowFallback = true
        )

        val success = result as QuizGenerationResult.Success
        val ids = success.questions.map { it.id }
        assertEquals(listOf(0, 1, 2, 3, 4), ids)
    }

    @Test
    fun `fallback questions each have exactly 4 options`() = runTest {
        coEvery { geminiService.generateQuiz(any(), any(), any()) } throws
            GeminiServiceException.Network("error")

        val result = repository.generateQuiz(
            technology = Technology.KOTLIN,
            experienceLevel = ExperienceLevel.INTERMEDIATE,
            questionCount = 4,
            allowFallback = true
        )

        val success = result as QuizGenerationResult.Success
        success.questions.forEach { question ->
            assertEquals(4, question.options.size)
        }
    }

    // ── Failure path ──────────────────────────────────────────────────────────

    @Test
    fun `generateQuiz returns Failure when service fails and allowFallback false`() = runTest {
        coEvery { geminiService.generateQuiz(any(), any(), any()) } throws
            GeminiServiceException.Network("network down")

        val result = repository.generateQuiz(
            technology = Technology.PYTHON,
            experienceLevel = ExperienceLevel.ADVANCED,
            questionCount = 5,
            allowFallback = false
        )

        assertTrue(result is QuizGenerationResult.Failure)
        val failure = result as QuizGenerationResult.Failure
        assertTrue(failure.canRetry)
        assertTrue(failure.canUseFallback)
    }

    @Test
    fun `generateQuiz returns Failure with canRetry false when MissingApiKey`() = runTest {
        coEvery { geminiService.generateQuiz(any(), any(), any()) } throws
            GeminiServiceException.MissingApiKey

        val result = repository.generateQuiz(
            technology = Technology.ANDROID,
            experienceLevel = ExperienceLevel.BEGINNER,
            questionCount = 3,
            allowFallback = false
        )

        assertTrue(result is QuizGenerationResult.Failure)
        val failure = result as QuizGenerationResult.Failure
        assertFalse("MissingApiKey should set canRetry=false", failure.canRetry)
        assertTrue(failure.canUseFallback)
    }

    @Test
    fun `generateQuiz returns Failure with non-empty message on error`() = runTest {
        coEvery { geminiService.generateQuiz(any(), any(), any()) } throws
            GeminiServiceException.QuotaExceeded

        val result = repository.generateQuiz(
            technology = Technology.WEB,
            experienceLevel = ExperienceLevel.BEGINNER,
            questionCount = 3,
            allowFallback = false
        )

        val failure = result as QuizGenerationResult.Failure
        assertTrue(failure.message.isNotBlank())
    }
}

