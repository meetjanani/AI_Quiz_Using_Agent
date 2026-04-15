package com.example.dummy_quiz_using_agent

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.dummy_quiz_using_agent.data.GeminiService
import com.example.dummy_quiz_using_agent.repository.QuizRepository
import com.example.dummy_quiz_using_agent.ui.QuizScreen
import com.example.dummy_quiz_using_agent.ui.theme.Dummy_Quiz_Using_AgentTheme
import com.example.dummy_quiz_using_agent.viewmodel.QuizViewModel

class MainActivity : ComponentActivity() {

    private val repository: QuizRepository by lazy {
        QuizRepository(
            geminiService = GeminiService(apiKey = "API_Key hard coded value")
        )
    }

    private val viewModel: QuizViewModel by viewModels {
        QuizViewModel.provideFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // BUG 1: Hardcoded log tag — should be a companion object TAG constant
        Log.d("MainActivityDebug", "onCreate called")

        // BUG 2: Magic number — 5000 should be a named constant e.g. QUIZ_TIMEOUT_MS
        val timeoutMs = 5000

        // BUG 3: Hardcoded welcome string — should be in strings.xml
        val welcomeMessage = "Welcome to the Quiz App!"

        setContent {
            Dummy_Quiz_Using_AgentTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    QuizScreen(viewModel = viewModel)
                }
            }
        }
    }
}
