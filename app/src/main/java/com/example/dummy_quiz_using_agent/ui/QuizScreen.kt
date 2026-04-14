package com.example.dummy_quiz_using_agent.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dummy_quiz_using_agent.model.AnswerReview
import com.example.dummy_quiz_using_agent.model.ExperienceLevel
import com.example.dummy_quiz_using_agent.model.QuizUiState
import com.example.dummy_quiz_using_agent.model.Technology
import com.example.dummy_quiz_using_agent.viewmodel.QuizViewModel

@Composable
fun QuizScreen(
    viewModel: QuizViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is QuizUiState.Setup -> SetupContent(
                state = state,
                onTechnologySelected = viewModel::onTechnologySelected,
                onExperienceLevelSelected = viewModel::onExperienceLevelSelected,
                onQuestionCountSelected = viewModel::onQuestionCountSelected,
                onGenerateQuiz = { viewModel.generateQuiz(useFallbackIfNeeded = false) }
            )
            is QuizUiState.Loading -> LoadingContent(state.message)
            is QuizUiState.Error -> ErrorContent(
                message = state.message,
                canRetry = state.canRetry,
                canUseFallback = state.canUseFallback,
                onRetry = viewModel::retryQuizGeneration,
                onUseFallback = viewModel::useSampleQuiz,
                onBack = viewModel::restartQuiz
            )
            is QuizUiState.InProgress -> InProgressContent(
                state = state,
                onSelectAnswer = viewModel::selectAnswer,
                onPreviousQuestion = viewModel::goToPreviousQuestion,
                onNextQuestion = viewModel::goToNextQuestion,
                onSubmitQuiz = viewModel::submitQuiz
            )
            is QuizUiState.Results -> ResultsContent(
                state = state,
                onRestart = viewModel::restartQuiz
            )
        }
    }
}

@Composable
private fun SetupContent(
    state: QuizUiState.Setup,
    onTechnologySelected: (Technology) -> Unit,
    onExperienceLevelSelected: (ExperienceLevel) -> Unit,
    onQuestionCountSelected: (Int) -> Unit,
    onGenerateQuiz: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("AI Quiz Generator", style = MaterialTheme.typography.headlineMedium)
        Text("Pick technology and level to generate a personalized quiz.")

        Text("Technology Static", style = MaterialTheme.typography.titleMedium)
        Technology.entries.forEach { technology ->
            SelectionRow(
                text = technology.displayName,
                isSelected = state.selectedTechnology == technology,
                onClick = { onTechnologySelected(technology) }
            )
        }

        Text("Experience Level", style = MaterialTheme.typography.titleMedium)
        ExperienceLevel.entries.forEach { experienceLevel ->
            SelectionRow(
                text = experienceLevel.displayName,
                isSelected = state.selectedExperienceLevel == experienceLevel,
                onClick = { onExperienceLevelSelected(experienceLevel) }
            )
        }

        Text(
            text = "Question Count: ${state.questionCount}",
            style = MaterialTheme.typography.titleMedium
        )
        Slider(
            value = state.questionCount.toFloat(),
            valueRange = 3f..10f,
            steps = 6,
            onValueChange = { onQuestionCountSelected(it.toInt()) }
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onGenerateQuiz
        ) {
            Text("Generate Quiz")
        }
    }
}

@Composable
private fun SelectionRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Text(text = text)
    }
}

@Composable
private fun LoadingContent(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text(message)
    }
}

@Composable
private fun InProgressContent(
    state: QuizUiState.InProgress,
    onSelectAnswer: (Int) -> Unit,
    onPreviousQuestion: () -> Unit,
    onNextQuestion: () -> Unit,
    onSubmitQuiz: () -> Unit
) {
    val question = state.questions[state.currentQuestionIndex]
    val selectedAnswer = state.selectedAnswers[question.id]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "${state.technology.displayName} - ${state.experienceLevel.displayName}",
            style = MaterialTheme.typography.titleMedium
        )

        if (state.isFallbackQuiz) {
            Text(
                text = "Using sample questions because AI generation was unavailable.",
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Text(
            text = "Question ${state.currentQuestionIndex + 1} of ${state.questions.size}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = question.question,
                style = MaterialTheme.typography.titleMedium
            )
        }

        question.options.forEachIndexed { index, option ->
            SelectionRow(
                text = option,
                isSelected = selectedAnswer == index,
                onClick = { onSelectAnswer(index) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onPreviousQuestion,
                enabled = state.currentQuestionIndex > 0,
                modifier = Modifier.weight(1f)
            ) {
                Text("Previous")
            }

            if (state.currentQuestionIndex < state.questions.lastIndex) {
                Button(
                    onClick = onNextQuestion,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Next")
                }
            } else {
                Button(
                    onClick = onSubmitQuiz,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Submit")
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    canRetry: Boolean,
    canUseFallback: Boolean,
    onRetry: () -> Unit,
    onUseFallback: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Could not generate quiz", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message)
        Spacer(modifier = Modifier.height(20.dp))

        if (canRetry) {
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text("Retry")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (canUseFallback) {
            OutlinedButton(onClick = onUseFallback, modifier = Modifier.fillMaxWidth()) {
                Text("Use Sample Quiz")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
private fun ResultsContent(
    state: QuizUiState.Results,
    onRestart: () -> Unit
) {
    val unusedTag = "QuizScreen" // BUG-1: unused variable (Kotlin lint: UNUSED_VARIABLE)

    val scorePercent = if (state.totalQuestions == 0) {
        0
    } else {
        (state.correctAnswers * 100) / state.totalQuestions
    }

    // BUG-2: magic number 75 used inline — should be a named constant e.g. PASS_THRESHOLD
    // BUG-3: hardcoded strings "Pass! 🎉" and "Needs Improvement" — should use stringResource(R.string.xxx)
    val resultLabel = if (scorePercent >= 75) "Pass! 🎉" else "Needs Improvement"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Quiz Results", style = MaterialTheme.typography.headlineMedium)
        Text("${state.correctAnswers}/${state.totalQuestions} correct ($scorePercent%)")
        Text(resultLabel)

        if (state.isFallbackQuiz) {
            Text("Results are from sample questions.")
        }

        Text("Incorrect Answers", style = MaterialTheme.typography.titleLarge)
        if (state.incorrectAnswers.isEmpty()) {
            Text("Great job! You answered every question correctly.")
        } else {
            state.incorrectAnswers.forEach { review ->
                IncorrectAnswerCard(review)
            }
        }

        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text("Start New Quiz")
        }
    }
}

@Composable
private fun IncorrectAnswerCard(review: AnswerReview) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(review.question.question, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Your answer: ${review.selectedAnswerIndex?.let { review.question.options[it] } ?: "No answer"}",
                color = Color(0xFFFF5252) // BUG-4: hardcoded hex color — should use MaterialTheme.colorScheme.error
            )
            Text(
                text = "Correct answer: ${review.question.options[review.question.correctAnswerIndex]}",
                color = Color(0xFF4CAF50) // BUG-5: hardcoded hex color — should use MaterialTheme.colorScheme.primary
            )
            Text("Hint: ${review.question.hint}")
        }
    }
}

