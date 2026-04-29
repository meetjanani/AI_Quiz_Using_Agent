package com.example.dummy_quiz_using_agent.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.dummy_quiz_using_agent.R

@Composable
fun HomeScreen(
    onOpenQuiz: () -> Unit,
    onOpenShoppingAgent: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = onOpenQuiz,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.home_open_quiz))
        }

        OutlinedButton(
            onClick = onOpenShoppingAgent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.home_open_shopping_agent))
        }
    }
}

