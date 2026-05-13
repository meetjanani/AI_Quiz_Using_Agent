# AGENTS.md

## Purpose
- This repo is an Android Jetpack Compose app with two AI user flows: Quiz generation and Smart Shopping insights (`README.md`).
- Primary code lives in `app/src/main/java/com/example/dummy_quiz_using_agent`; architecture is MVVM + Repository + Service + `StateFlow`.

## Big-picture architecture
- Navigation is centralized in `app/src/main/java/com/example/dummy_quiz_using_agent/MainActivity.kt` via `NavHost` routes: `splash -> home -> quiz/shopping`.
- Dependency wiring is manual in `MainActivity.kt` (lazy repositories/services + `viewModels { provideFactory(...) }`), not DI framework-based.
- Each feature follows layer split:
  - Model: `model/*Models.kt` sealed UI state + domain models.
  - Service: `data/*Service.kt` prompt/API/parsing + feature-specific exceptions.
  - Repository: `repository/*Repository.kt` maps service exceptions into `Success/Failure` result types.
  - ViewModel: `viewmodel/*ViewModel.kt` owns `MutableStateFlow` and exposes `StateFlow`.
  - UI: `ui/*Screen.kt` renders by `when(uiState)` with per-state composables.

## Critical runtime flows
- Quiz flow: `QuizScreen` -> `QuizViewModel.generateQuiz()` -> `QuizRepository.generateQuiz()` -> `GeminiService.generateQuiz()` -> parsed JSON `QuizQuestion` list.
- Quiz fallback is intentional: repository can return local template questions when Gemini fails and fallback is allowed (`QuizSource.FALLBACK`).
- Shopping flow: `SmartShoppingScreen` input -> `ShoppingViewModel.analyzeProduct()` validates Amazon/Flipkart URLs -> `DefaultShoppingRepository` -> `ShoppingDecisionService` JSON parsing to `ProductInsight`.
- Splash flow: `SplashViewModel.initialize()` wraps init in `withTimeoutOrNull(3000)`; `SplashInitService` enforces minimum 1.5s display and auth-token gate via `SharedPreferences`.
- Home flow reads lightweight stats from same `app_prefs` store (`last_quiz_score`, `recent_shopping_query`) through `HomeService`.

## External integrations and config
- Gemini key is loaded from `local.properties` (`GEMINI_API_KEY`) and injected as `BuildConfig.GEMINI_API_KEY` in `app/build.gradle.kts`.
- Gemini SDK: `com.google.ai.client.generativeai`; current model defaults differ by feature:
  - Quiz: `gemini-2.5-flash` in `data/GeminiService.kt`.
  - Shopping: `gemini-3.1-flash-lite-preview` in `data/ShoppingDecisionService.kt`.
- `INTERNET` permission is required and declared in `app/src/main/AndroidManifest.xml`.

## Developer workflows that matter
- Build: `./gradlew build`
- Unit tests: `./gradlew :app:testDebugUnitTest`
- Lint (matches CI context collection): `./gradlew :app:lintDebug --console=plain`
- Coverage report (custom task in module build file): `./gradlew :app:jacocoTestReport`
- Jira story context sync helper: `./jira-sync` (bulk) or `./jira-sync --implement KAN-1` (single-story prompt generation).

## Project-specific conventions for agents
- Keep feature additions consistent with existing file-per-layer pattern (`<Feature>Models.kt`, `<Feature>Service.kt`, `<Feature>Repository.kt`, `<Feature>ViewModel.kt`, `<Feature>Screen.kt`).
- Use `sealed interface` UI states and include retry-aware error states (`canRetry`) like existing features.
- Keep ViewModels free of `Context`; platform access stays in services (for example `HomeService`, `SplashInitService`).
- Use `collectAsStateWithLifecycle()` in composables (seen in all screens).
- Prefer `strings.xml` for new user-facing text; some older quiz UI strings are still hardcoded, so avoid broad refactors unless requested.
- When touching coverage-sensitive business logic (`repository/`, `viewmodel/`), mirror current test style in `app/src/test` (MockK + coroutine test dispatcher).

## AI review automation context
- PR AI review pipeline is defined in `.github/workflows/ai-pr-reviewer.yml` and orchestrated by `.github/scripts/run_agent.py`.
- Local artifacts consumed by reviewer tooling include `pr_diff.patch`, optional `lint_output.txt`, and optional `recent_pr_comments.txt` (`README.md`).

