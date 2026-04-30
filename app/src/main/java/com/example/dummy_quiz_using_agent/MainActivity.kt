package com.example.dummy_quiz_using_agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dummy_quiz_using_agent.data.GeminiService
import com.example.dummy_quiz_using_agent.data.HomeService
import com.example.dummy_quiz_using_agent.data.ShoppingDecisionService
import com.example.dummy_quiz_using_agent.data.SplashInitService
import com.example.dummy_quiz_using_agent.repository.DefaultHomeRepository
import com.example.dummy_quiz_using_agent.repository.DefaultShoppingRepository
import com.example.dummy_quiz_using_agent.repository.DefaultSplashRepository
import com.example.dummy_quiz_using_agent.repository.QuizRepository
import com.example.dummy_quiz_using_agent.ui.HomeScreen
import com.example.dummy_quiz_using_agent.ui.QuizScreen
import com.example.dummy_quiz_using_agent.ui.SmartShoppingScreen
import com.example.dummy_quiz_using_agent.ui.SplashScreen
import com.example.dummy_quiz_using_agent.ui.theme.Dummy_Quiz_Using_AgentTheme
import com.example.dummy_quiz_using_agent.viewmodel.HomeViewModel
import com.example.dummy_quiz_using_agent.viewmodel.QuizViewModel
import com.example.dummy_quiz_using_agent.viewmodel.ShoppingViewModel
import com.example.dummy_quiz_using_agent.viewmodel.SplashViewModel

class MainActivity : ComponentActivity() {

    // ── Quiz ──────────────────────────────────────────────────────────────────
    private val repository: QuizRepository by lazy {
        QuizRepository(geminiService = GeminiService(apiKey = BuildConfig.GEMINI_API_KEY))
    }
    private val viewModel: QuizViewModel by viewModels {
        QuizViewModel.provideFactory(repository)
    }

    // ── Shopping ──────────────────────────────────────────────────────────────
    private val shoppingRepository: DefaultShoppingRepository by lazy {
        DefaultShoppingRepository(service = ShoppingDecisionService(apiKey = BuildConfig.GEMINI_API_KEY))
    }
    private val shoppingViewModel: ShoppingViewModel by viewModels {
        ShoppingViewModel.provideFactory(shoppingRepository)
    }

    // KAN-2: Home UI/UX enhancement state stack
    private val homeRepository: DefaultHomeRepository by lazy {
        DefaultHomeRepository(service = HomeService(applicationContext))
    }
    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModel.provideFactory(homeRepository)
    }

    // ── Splash (KAN-1) ────────────────────────────────────────────────────────
    private val splashRepository: DefaultSplashRepository by lazy {
        DefaultSplashRepository(service = SplashInitService(applicationContext))
    }
    private val splashViewModel: SplashViewModel by viewModels {
        SplashViewModel.provideFactory(splashRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // KAN-1: Install Android 12+ Splash Screen API before super.onCreate
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Dummy_Quiz_Using_AgentTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(
                        quizViewModel = viewModel,
                        shoppingViewModel = shoppingViewModel,
                        splashViewModel = splashViewModel,
                        homeViewModel = homeViewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(
    quizViewModel: QuizViewModel,
    shoppingViewModel: ShoppingViewModel,
    splashViewModel: SplashViewModel,
    homeViewModel: HomeViewModel
) {
    val navController = rememberNavController()

    // KAN-1: App starts on SPLASH route, not HOME
    NavHost(navController = navController, startDestination = AppRoute.SPLASH.route) {

        composable(AppRoute.SPLASH.route) {
            SplashScreen(
                viewModel = splashViewModel,
                onNavigateToHome = {
                    navController.navigate(AppRoute.HOME.route) {
                        popUpTo(AppRoute.SPLASH.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    // KAN-1: Navigate to login/onboarding (AC4)
                    // For now routes to HOME; replace with LOGIN route when that screen exists.
                    navController.navigate(AppRoute.HOME.route) {
                        popUpTo(AppRoute.SPLASH.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoute.HOME.route) {
            HomeScreen(
                viewModel = homeViewModel,
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
    SPLASH("splash"),   // KAN-1: new start destination
    HOME("home"),
    QUIZ("quiz"),
    SHOPPING("shopping")
}
// ./jira-sync
// ./jira-sync --implement KAN-3

/*
@workspace Implement Jira Story KAN-2 — Android Mobile App – Home Screen UI/UX Enhancement
Full requirements are in docs/jira/current_story.md.
Architecture rules are in .github/scripts/story_agent_prompt.md.
Please implement all required files end-to-end following the existing MVVM + Compose + StateFlow architecture.
* */