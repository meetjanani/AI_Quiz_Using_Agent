package com.example.dummy_quiz_using_agent.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dummy_quiz_using_agent.R
import com.example.dummy_quiz_using_agent.model.Habit
import com.example.dummy_quiz_using_agent.model.HabitTrackerUiState
import com.example.dummy_quiz_using_agent.viewmodel.HabitTrackerViewModel

@Composable
fun HabitTrackerScreen(
    viewModel: HabitTrackerViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        val errorState = uiState as? HabitTrackerUiState.Error ?: return@LaunchedEffect
        snackBarHostState.showSnackbar(errorState.message)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                HabitTrackerUiState.Loading -> HabitTrackerLoadingContent(onBack = onBack)

                is HabitTrackerUiState.Success -> HabitTrackerSuccessContent(
                    state = state,
                    onHabitNameChanged = viewModel::onHabitNameChanged,
                    onAddHabit = viewModel::addHabit,
                    onToggleHabit = viewModel::toggleHabit,
                    onResetDay = viewModel::resetDay,
                    onBack = onBack
                )

                is HabitTrackerUiState.Error -> HabitTrackerErrorContent(
                    state = state,
                    onRetry = viewModel::retry,
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
private fun HabitTrackerLoadingContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResource(R.string.habit_tracker_loading))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.habit_tracker_back_home))
        }
    }
}

@Composable
private fun HabitTrackerSuccessContent(
    state: HabitTrackerUiState.Success,
    onHabitNameChanged: (String) -> Unit,
    onAddHabit: () -> Unit,
    onToggleHabit: (String) -> Unit,
    onResetDay: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.habit_tracker_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = state.pendingHabitName,
            onValueChange = onHabitNameChanged,
            label = { Text(stringResource(R.string.habit_tracker_input_label)) },
            placeholder = { Text(stringResource(R.string.habit_tracker_input_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = state.inputError != null
        )

        if (state.inputError != null) {
            Text(
                text = state.inputError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onAddHabit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.habit_tracker_add_button))
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.habit_tracker_progress,
                        state.data.completedCount,
                        state.data.totalCount
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
                LinearProgressIndicator(
                    progress = { state.data.progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.data.habits.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.habit_tracker_empty),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(
                    items = state.data.habits,
                    key = { it.id }
                ) { habit ->
                    HabitRow(
                        habit = habit,
                        onToggleHabit = { onToggleHabit(habit.id) }
                    )
                }
            }
        }

        OutlinedButton(onClick = onResetDay, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.habit_tracker_reset_button))
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.habit_tracker_back_home))
        }
    }
}

@Composable
private fun HabitRow(
    habit: Habit,
    onToggleHabit: () -> Unit
) {
    val checkboxContentDescription = if (habit.isCompleted) {
        stringResource(R.string.habit_tracker_cd_completed, habit.name)
    } else {
        stringResource(R.string.habit_tracker_cd_not_completed, habit.name)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Checkbox(
                checked = habit.isCompleted,
                onCheckedChange = { onToggleHabit() },
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = checkboxContentDescription
                    }
            )
        }
    }
}

@Composable
private fun HabitTrackerErrorContent(
    state: HabitTrackerUiState.Error,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.habit_tracker_error_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = state.message)
        Spacer(modifier = Modifier.height(16.dp))

        if (state.canRetry) {
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.habit_tracker_retry))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.habit_tracker_back_home))
        }
    }
}

