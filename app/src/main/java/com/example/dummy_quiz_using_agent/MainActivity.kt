package com.example.dummy_quiz_using_agent

import android.os.Bundle
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
            geminiService = GeminiService(apiKey = "wrong API KEy")
        )
    }

    private val viewModel: QuizViewModel by viewModels {
        QuizViewModel.provideFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Dummy_Quiz_Using_AgentTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    QuizScreen(viewModel = viewModel)
                }
            }
        }
    }
}
