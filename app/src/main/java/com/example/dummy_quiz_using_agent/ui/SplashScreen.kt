package com.example.dummy_quiz_using_agent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dummy_quiz_using_agent.R
import com.example.dummy_quiz_using_agent.model.SplashUiState
import com.example.dummy_quiz_using_agent.viewmodel.SplashViewModel

/**
 * KAN-1: Android Mobile App Splash Screen
 *
 * Entry-point composable. Handles per-state rendering and fires navigation
 * callbacks when initialization is [SplashUiState.Ready] (AC3 / AC4).
 *
 * Note: The Android 12+ Splash Screen API theme-based icon is handled via
 * [Theme.Dummy_Quiz_Using_Agent.Starting] in themes.xml. This composable is
 * the Compose-layer that shows while in-app initialization completes.
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate as soon as Ready state is emitted (AC3 / AC4 / AC5)
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SplashUiState.Ready -> {
                if (state.isLoggedIn) onNavigateToHome() else onNavigateToLogin()
            }
            else -> Unit
        }
    }

    when (val state = uiState) {
        is SplashUiState.Initializing -> SplashInitializingContent()
        is SplashUiState.Ready        -> SplashInitializingContent() // brief, LaunchedEffect navigates away
        is SplashUiState.Error        -> SplashErrorContent(state, onRetry = viewModel::retry)
    }
}

// ── Initializing ─────────────────────────────────────────────────────────────

@Composable
private fun SplashInitializingContent() {
    val logoDescription = stringResource(R.string.splash_logo_content_description)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo placeholder (AC1: centered logo)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.extraLarge
                    )
                    .semantics { contentDescription = logoDescription },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.splash_logo_placeholder),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App name (AC1: brand guidelines)
            Text(
                text = stringResource(R.string.splash_app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Optional tagline (FR)
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        }
    }
}

// ── Error ─────────────────────────────────────────────────────────────────────

@Composable
private fun SplashErrorContent(
    state: SplashUiState.Error,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.splash_error_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            if (state.canRetry) {
                Button(onClick = onRetry) {
                    Text(text = stringResource(R.string.splash_retry))
                }
            }
        }
    }
}

