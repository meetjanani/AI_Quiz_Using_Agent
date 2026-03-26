package com.example.dummy_quiz_using_agent.repository

import com.example.dummy_quiz_using_agent.data.GeminiService
import com.example.dummy_quiz_using_agent.data.GeminiServiceException
import com.example.dummy_quiz_using_agent.model.ExperienceLevel
import com.example.dummy_quiz_using_agent.model.QuizQuestion
import com.example.dummy_quiz_using_agent.model.Technology

class QuizRepository(
    private val geminiService: GeminiService
) {

    suspend fun generateQuiz(
        technology: Technology,
        experienceLevel: ExperienceLevel,
        questionCount: Int,
        allowFallback: Boolean
    ): QuizGenerationResult {
        // BUG-11: Thread.sleep() inside a suspend function blocks the underlying coroutine thread.
        // Should use kotlinx.coroutines.delay() instead.
        Thread.sleep(200)

        return try {
            val questions = geminiService.generateQuiz(
                technology = technology,
                experienceLevel = experienceLevel,
                questionCount = questionCount
            )
            QuizGenerationResult.Success(questions = questions, source = QuizSource.AI)
        } catch (error: GeminiServiceException) {
            if (allowFallback) {
                QuizGenerationResult.Success(
                    questions = buildFallbackQuestions(technology, experienceLevel, questionCount),
                    source = QuizSource.FALLBACK
                )
            } else {
                QuizGenerationResult.Failure(
                    // BUG-12: hardcoded inline error string — should be extracted to a named constant or string resource
                    message = error.message ?: "Quiz generation failed. Please try again later.",
                    canRetry = error !is GeminiServiceException.MissingApiKey,
                    canUseFallback = true
                )
            }
        }
    }

    private fun buildFallbackQuestions(
        technology: Technology,
        experienceLevel: ExperienceLevel,
        questionCount: Int
    ): List<QuizQuestion> {
        val templateQuestions = listOf(
            QuizQuestion(
                id = 0,
                question = "What best describes ${technology.displayName} for ${experienceLevel.displayName} developers?",
                options = listOf(
                    "A core concept to practice regularly",
                    "Only useful for advanced engineers",
                    "Not relevant for this level",
                    "A deprecated topic"
                ),
                correctAnswerIndex = 0,
                hint = "Focus on fundamentals that build confidence with steady practice."
            ),
            QuizQuestion(
                id = 1,
                question = "Which learning approach is most effective when starting ${technology.displayName}?",
                options = listOf(
                    "Build small projects and iterate",
                    "Memorize APIs without coding",
                    "Avoid debugging until the end",
                    "Skip documentation"
                ),
                correctAnswerIndex = 0,
                hint = "Practical exercises and quick feedback loops are usually best."
            ),
            QuizQuestion(
                id = 2,
                question = "What is a good way to verify your understanding in ${technology.displayName}?",
                options = listOf(
                    "Explain the concept and implement a tiny example",
                    "Read only one source once",
                    "Copy code without changes",
                    "Ignore edge cases"
                ),
                correctAnswerIndex = 0,
                hint = "Teaching and building small examples expose knowledge gaps quickly."
            ),
            QuizQuestion(
                id = 3,
                question = "How should you handle mistakes while learning ${technology.displayName}?",
                options = listOf(
                    "Treat them as feedback and debug systematically",
                    "Hide them and move on",
                    "Restart every time without analysis",
                    "Avoid testing to save time"
                ),
                correctAnswerIndex = 0,
                hint = "Debugging improves both conceptual and practical understanding."
            ),
            QuizQuestion(
                id = 4,
                question = "What is the most important habit for long-term growth in ${technology.displayName}?",
                options = listOf(
                    "Consistent practice with reflection",
                    "Waiting for perfect tutorials",
                    "Changing tools daily",
                    "Avoiding code reviews"
                ),
                correctAnswerIndex = 0,
                hint = "Consistency compounds over time and builds durable skill."
            )
        )

        return List(questionCount) { index ->
            val template = templateQuestions[index % templateQuestions.size]
            template.copy(id = index)
        }
    }
}

sealed interface QuizGenerationResult {
    data class Success(val questions: List<QuizQuestion>, val source: QuizSource) : QuizGenerationResult
    data class Failure(
        val message: String,
        val canRetry: Boolean,
        val canUseFallback: Boolean
    ) : QuizGenerationResult
}

enum class QuizSource {
    AI,
    FALLBACK
}

