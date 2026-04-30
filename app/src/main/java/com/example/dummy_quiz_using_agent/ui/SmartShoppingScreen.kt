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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dummy_quiz_using_agent.R
import com.example.dummy_quiz_using_agent.model.AlternativeProduct
import com.example.dummy_quiz_using_agent.model.ProductInsight
import com.example.dummy_quiz_using_agent.model.ShoppingPreference
import com.example.dummy_quiz_using_agent.model.ShoppingUiState
import com.example.dummy_quiz_using_agent.viewmodel.ShoppingViewModel

@Composable
fun SmartShoppingScreen(
    viewModel: ShoppingViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ShoppingUiState.Input -> ShoppingInputContent(
                state = state,
                onLinkChanged = viewModel::onProductLinkChanged,
                onPreferenceChanged = viewModel::onPreferenceChanged,
                onAnalyze = viewModel::analyzeProduct,
                onBack = onBack
            )

            is ShoppingUiState.Loading -> ShoppingLoadingContent(
                message = state.message,
                onBack = onBack
            )

            is ShoppingUiState.Result -> ShoppingResultContent(
                insight = state.insight,
                onAnalyzeAnother = viewModel::startNewAnalysis,
                onBack = onBack
            )

            is ShoppingUiState.Error -> ShoppingErrorContent(
                message = state.message,
                canRetry = state.canRetry,
                onRetry = viewModel::retry,
                onBack = onBack
            )
        }
    }
}

@Composable
private fun ShoppingInputContent(
    state: ShoppingUiState.Input,
    onLinkChanged: (String) -> Unit,
    onPreferenceChanged: (ShoppingPreference) -> Unit,
    onAnalyze: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.shopping_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.shopping_subtitle),
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = state.productLink,
            onValueChange = onLinkChanged,
            label = { Text(stringResource(R.string.shopping_product_link_label)) },
            placeholder = { Text(stringResource(R.string.shopping_product_link_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = state.validationError != null
        )

        if (state.validationError != null) {
            Text(
                text = state.validationError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(
            text = stringResource(R.string.shopping_preference_title),
            style = MaterialTheme.typography.titleMedium
        )

        ShoppingPreference.entries.forEach { preference ->
            PreferenceRow(
                title = preference.displayName,
                selected = state.selectedPreference == preference,
                onClick = { onPreferenceChanged(preference) }
            )
        }

        Button(
            onClick = onAnalyze,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.shopping_analyze_button))
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.shopping_back_to_home))
        }
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = title)
    }
}

@Composable
private fun ShoppingLoadingContent(
    message: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = message)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onBack) {
            Text(text = stringResource(R.string.shopping_back_to_home))
        }
    }
}

@Composable
private fun ShoppingResultContent(
    insight: ProductInsight,
    onAnalyzeAnother: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = insight.productTitle,
            style = MaterialTheme.typography.headlineSmall
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.shopping_summary_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(text = insight.reviewSummary)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.shopping_fake_review_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(
                        R.string.shopping_fake_review_level,
                        insight.fakeReviewRiskLevel.name,
                        insight.fakeReviewRiskScore
                    ),
                    fontWeight = FontWeight.SemiBold
                )
                insight.fakeReviewSignals.forEach { signal ->
                    Text(text = "- $signal")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.shopping_alternatives_title),
                    style = MaterialTheme.typography.titleMedium
                )
                if (insight.alternatives.isEmpty()) {
                    Text(text = stringResource(R.string.shopping_no_alternatives))
                } else {
                    insight.alternatives.forEach { alternative ->
                        AlternativeRow(alternative = alternative)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.shopping_personalized_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(text = insight.personalizedSuggestion)
            }
        }

        Button(onClick = onAnalyzeAnother, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.shopping_analyze_another))
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.shopping_back_to_home))
        }
    }
}

@Composable
private fun AlternativeRow(alternative: AlternativeProduct) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = alternative.name, fontWeight = FontWeight.SemiBold)
        Text(text = alternative.reason)
    }
}

@Composable
private fun ShoppingErrorContent(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.shopping_error_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message)
        Spacer(modifier = Modifier.height(16.dp))

        if (canRetry) {
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.shopping_retry))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.shopping_back_to_home))
        }
    }
}

