package com.example.dummy_quiz_using_agent.model

enum class ExperienceLevel(val displayName: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced")
}

enum class Technology(val displayName: String, val promptTopic: String) {
    ANDROID("Android", "Android app development with Kotlin and Jetpack Compose"),
    KOTLIN("Kotlin", "Kotlin language fundamentals and best practices"),
    JAVA("Java", "Java programming fundamentals and modern Java usage"),
    PYTHON("Python", "Python programming concepts and practical use"),
    WEB("Web Development", "HTML, CSS, JavaScript, and frontend basics")
}

data class QuizQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val hint: String
)

data class AnswerReview(
    val question: QuizQuestion,
    val selectedAnswerIndex: Int?
)

sealed interface QuizUiState {
    data class Setup(
        val selectedTechnology: Technology = Technology.ANDROID,
        val selectedExperienceLevel: ExperienceLevel = ExperienceLevel.BEGINNER,
        val questionCount: Int = 5
    ) : QuizUiState

    data class Loading(val message: String = "Generating quiz...") : QuizUiState

    data class Error(
        val message: String,
        val canRetry: Boolean,
        val canUseFallback: Boolean
    ) : QuizUiState

    data class InProgress(
        val technology: Technology,
        val experienceLevel: ExperienceLevel,
        val questions: List<QuizQuestion>,
        val selectedAnswers: Map<Int, Int> = emptyMap(),
        val currentQuestionIndex: Int = 0,
        val isFallbackQuiz: Boolean = false
    ) : QuizUiState

    data class Results(
        val technology: Technology,
        val experienceLevel: ExperienceLevel,
        val totalQuestions: Int,
        val correctAnswers: Int,
        val incorrectAnswers: List<AnswerReview>,
        val isFallbackQuiz: Boolean
    ) : QuizUiState
}

