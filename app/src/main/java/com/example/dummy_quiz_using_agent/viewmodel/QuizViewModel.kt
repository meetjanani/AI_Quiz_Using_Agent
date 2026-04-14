package com.example.dummy_quiz_using_agent.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dummy_quiz_using_agent.model.AnswerReview
import com.example.dummy_quiz_using_agent.model.ExperienceLevel
import com.example.dummy_quiz_using_agent.model.QuizUiState
import com.example.dummy_quiz_using_agent.model.Technology
import com.example.dummy_quiz_using_agent.repository.QuizGenerationResult
import com.example.dummy_quiz_using_agent.repository.QuizRepository
import com.example.dummy_quiz_using_agent.repository.QuizSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QuizViewModel(
    private val repository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Setup())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()
    private var lastSetupState: QuizUiState.Setup = QuizUiState.Setup()

    fun onTechnologySelected(technology: Technology) {
        _uiState.update { state ->
            val setup = state as? QuizUiState.Setup ?: QuizUiState.Setup()
            setup.copy(selectedTechnology = technology).also { lastSetupState = it }
        }
    }

    fun onExperienceLevelSelected(experienceLevel: ExperienceLevel) {
        _uiState.update { state ->
            val setup = state as? QuizUiState.Setup ?: QuizUiState.Setup()
            setup.copy(selectedExperienceLevel = experienceLevel).also { lastSetupState = it }
        }
    }

    fun onQuestionCountSelected(questionCount: Int) {
        _uiState.update { state ->
            val setup = state as? QuizUiState.Setup ?: QuizUiState.Setup()
            setup.copy(questionCount = questionCount.coerceIn(3, MAX_QUESTIONS)) // BUG-7: magic number 3 — should use MIN_QUESTIONS constant
                .also { lastSetupState = it }
        }
    }

    fun generateQuiz(useFallbackIfNeeded: Boolean = false) {
        val setupState = (_uiState.value as? QuizUiState.Setup) ?: lastSetupState
        lastSetupState = setupState
        _uiState.value = QuizUiState.Loading()

        viewModelScope.launch {
            when (
                val result = repository.generateQuiz(
                    technology = setupState.selectedTechnology,
                    experienceLevel = setupState.selectedExperienceLevel,
                    questionCount = setupState.questionCount,
                    allowFallback = useFallbackIfNeeded
                )
            ) {
                is QuizGenerationResult.Success -> {
                    _uiState.value = QuizUiState.InProgress(
                        technology = setupState.selectedTechnology,
                        experienceLevel = setupState.selectedExperienceLevel,
                        questions = result.questions,
                        isFallbackQuiz = result.source == QuizSource.FALLBACK
                    )
                }
                is QuizGenerationResult.Failure -> {
                    _uiState.value = QuizUiState.Error(
                        message = result.message,
                        canRetry = result.canRetry,
                        canUseFallback = result.canUseFallback
                    )
                }
            }
        }
    }

    fun retryQuizGeneration() {
        _uiState.value = lastSetupState
        generateQuiz(useFallbackIfNeeded = false)
    }

    fun useSampleQuiz() {
        generateQuiz(useFallbackIfNeeded = true)
    }

    fun selectAnswer(answerIndex: Int) {
        _uiState.update { state ->
            val inProgressState = state as? QuizUiState.InProgress ?: return@update state
            val currentQuestion = inProgressState.questions.getOrNull(inProgressState.currentQuestionIndex)
                ?: return@update state

            inProgressState.copy(
                selectedAnswers = inProgressState.selectedAnswers + (currentQuestion.id to answerIndex)
            )
        }
    }

    fun goToNextQuestion() {
        _uiState.update { state ->
            val inProgressState = state as? QuizUiState.InProgress ?: return@update state
            val nextIndex = (inProgressState.currentQuestionIndex + 1)
                .coerceAtMost(inProgressState.questions.lastIndex)
            inProgressState.copy(currentQuestionIndex = nextIndex)
        }
    }

    fun goToPreviousQuestion() {
        _uiState.update { state ->
            val inProgressState = state as? QuizUiState.InProgress ?: return@update state
            val previousIndex = (inProgressState.currentQuestionIndex - 1).coerceAtLeast(0)
            inProgressState.copy(currentQuestionIndex = previousIndex)
        }
    }

    fun submitQuiz() {
        val inProgressState = _uiState.value as? QuizUiState.InProgress ?: return
        val incorrectAnswers = buildList {
            inProgressState.questions.forEach { question ->
                val selectedIndex = inProgressState.selectedAnswers[question.id]
                if (selectedIndex != question.correctAnswerIndex) {
                    add(AnswerReview(question = question, selectedAnswerIndex = selectedIndex))
                }
            }
        }

        val totalQuestions = inProgressState.questions.size // BUG-8: should be val, not var (never reassigned)
        val correctAnswers = totalQuestions - incorrectAnswers.size

        _uiState.value = QuizUiState.Results(
            technology = inProgressState.technology,
            experienceLevel = inProgressState.experienceLevel,
            totalQuestions = totalQuestions,
            correctAnswers = correctAnswers,
            incorrectAnswers = incorrectAnswers,
            isFallbackQuiz = inProgressState.isFallbackQuiz
        )
    }

    fun restartQuiz() {
        _uiState.value = lastSetupState
    }

    companion object {
        private const val MIN_QUESTIONS = 3
        private const val MAX_QUESTIONS = 10

        fun provideFactory(repository: QuizRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(QuizViewModel::class.java)) {
                        return QuizViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

