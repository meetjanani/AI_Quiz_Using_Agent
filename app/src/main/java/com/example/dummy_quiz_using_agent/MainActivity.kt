package com.example.dummy_quiz_using_agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dummy_quiz_using_agent.data.GeminiService
import com.example.dummy_quiz_using_agent.data.ShoppingDecisionService
import com.example.dummy_quiz_using_agent.repository.DefaultShoppingRepository
import com.example.dummy_quiz_using_agent.repository.QuizRepository
import com.example.dummy_quiz_using_agent.ui.HomeScreen
import com.example.dummy_quiz_using_agent.ui.QuizScreen
import com.example.dummy_quiz_using_agent.ui.SmartShoppingScreen
import com.example.dummy_quiz_using_agent.ui.theme.Dummy_Quiz_Using_AgentTheme
import com.example.dummy_quiz_using_agent.viewmodel.QuizViewModel
import com.example.dummy_quiz_using_agent.viewmodel.ShoppingViewModel

class MainActivity : ComponentActivity() {

    private val repository: QuizRepository by lazy {
        QuizRepository(
            geminiService = GeminiService(apiKey = BuildConfig.GEMINI_API_KEY)
        )
    }

    private val shoppingRepository: DefaultShoppingRepository by lazy {
        DefaultShoppingRepository(
            service = ShoppingDecisionService(apiKey = BuildConfig.GEMINI_API_KEY)
        )
    }

    private val viewModel: QuizViewModel by viewModels {
        QuizViewModel.provideFactory(repository)
    }

    private val shoppingViewModel: ShoppingViewModel by viewModels {
        ShoppingViewModel.provideFactory(shoppingRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Dummy_Quiz_Using_AgentTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(
                        quizViewModel = viewModel,
                        shoppingViewModel = shoppingViewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(
    quizViewModel: QuizViewModel,
    shoppingViewModel: ShoppingViewModel
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AppRoute.HOME.route) {
        composable(AppRoute.HOME.route) {
            HomeScreen(
                onOpenQuiz = { navController.navigate(AppRoute.QUIZ.route) },
                onOpenShoppingAgent = { navController.navigate(AppRoute.SHOPPING.route) }
            )
        }

        composable(AppRoute.QUIZ.route) {
            BackHandler { navController.popBackStack() }
            QuizScreen(viewModel = quizViewModel)
        }

        composable(AppRoute.SHOPPING.route) {
            BackHandler { navController.popBackStack() }
            SmartShoppingScreen(
                viewModel = shoppingViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private enum class AppRoute(val route: String) {
    HOME("home"),
    QUIZ("quiz"),
    SHOPPING("shopping")
}
