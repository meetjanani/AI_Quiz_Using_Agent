# Quiz_generation_Agent_Created_By_Meet.md

## Purpose
Use this file as the single instruction source for recreating an Android **Jetpack Compose quiz app** that generates quizzes by **technology** and **experience level** using an **LLM/Gemini backend**.

## App Blueprint
- Build an Android app in **Kotlin** using **Jetpack Compose** and **Material 3**.
- Default package can be `com.example.quizapp`, but keep package naming easy to replace.
- Use **Gradle Kotlin DSL** and a **version catalog** in `gradle/libs.versions.toml`.
- Target this baseline unless the new project says otherwise: **minSdk 24**, **targetSdk 36**, **compileSdk 36**, **Java 11**.

## Required Project Structure
Create these files/packages:
- `app/src/main/java/<package>/MainActivity.kt`
- `app/src/main/java/<package>/model/Models.kt`
- `app/src/main/java/<package>/repository/QuizRepository.kt`
- `app/src/main/java/<package>/viewmodel/QuizViewModel.kt`
- `app/src/main/java/<package>/ui/QuizScreen.kt`
- `app/src/main/java/<package>/ui/theme/Theme.kt`
- `app/src/main/java/<package>/ui/theme/Type.kt`
- `app/src/main/java/<package>/data/GeminiService.kt` when LLM-backed generation is required.

## Architecture Contract
Use **MVVM + Repository + Service**:
- `Models.kt`: define `ExperienceLevel`, `Technology`, `QuizQuestion`, and `QuizUiState`.
- `QuizUiState` must support at least: `Setup`, `Loading`, `Error`, `InProgress`, `Results`.
- `QuizViewModel` owns all UI state using **StateFlow**.
- `QuizRepository` is the single source of truth for quiz generation.
- `GeminiService` (or a pluggable LLM service) handles prompt building, API calls, response parsing, and error mapping.

## Quiz Behavior
The app flow must be:
1. **Setup**: user selects `Technology` and `ExperienceLevel`.
2. **Loading**: show progress while quiz content is generated.
3. **InProgress**: display quiz questions with answer selection.
4. **Results**: show score and only incorrectly answered questions.
5. **Error**: show actionable retry/back UI when API/network/model/quota issues occur.

## UI Requirements
Implement the whole app as a **single Compose screen routed by `QuizUiState`**.
- `SetupContent`: dropdown/selectors for technology and experience level, plus a “Generate Quiz” button.
- `LoadingContent`: centered progress indicator and status text.
- `InProgressContent`: question list or question-by-question flow with radio buttons, submit button, and optional previous/next navigation.
- `ResultsContent`: score, percentage, incorrect answers, correct answers, hints, restart button.
- Use **MaterialTheme**, `Surface`, responsive spacing, and keep text/resources clean and readable.

## LLM / Gemini Integration Rules
If quiz generation is dynamic:
- Use `com.google.ai.client.generativeai` by default.
- Read `GEMINI_API_KEY` from `local.properties` and inject it using `buildConfigField` into `BuildConfig`.
- Add `INTERNET` permission in `AndroidManifest.xml`.
- Build prompts that explicitly request **strict JSON output** for quiz questions.
- Parse model output into `QuizQuestion` safely.
- Handle failures for: missing API key, DNS/network issues, invalid model, timeouts, quota exhaustion, truncated `MAX_TOKENS` responses.
- Prefer retry/fallback behavior over hard failure.

## Prompting Rules for Quiz Generation
When generating quiz questions, instruct the LLM to return:
- exact number of questions requested
- exactly 4 options per question
- integer `correctAnswerIndex`
- a short educational `hint`
- **raw JSON only** with no markdown fences
If responses may be large, allow a compact retry prompt or smaller batches.

## Fallback Expectations
Always make the app resilient:
- If AI generation fails, support either a local fallback question set or a clear retryable error state.
- If quota is exhausted, surface a user-friendly explanation and do not crash.
- If model output is truncated, retry with a shorter prompt or smaller batch size.

## Build Configuration Rules
Keep build setup aligned with this pattern:
- Enable `compose = true` and `buildConfig = true` in `app/build.gradle.kts`.
- Keep Compose, Lifecycle, Activity Compose, Coroutines, and Gemini versions in `gradle/libs.versions.toml`.
- Use the Kotlin Compose plugin in the module plugins block.
- Add lint workarounds only if needed for known Compose lint issues.

## Coding Conventions
- Kotlin official style, 4-space indentation, no wildcard imports.
- Prefer `when`, data classes, sealed interfaces, and immutable UI state.
- Keep strings/resource use organized if the new project introduces XML resources.
- Avoid logging secrets; never print API keys.

## Minimum Quality Bar
Whenever recreating this project:
- produce complete runnable code, not placeholders
- wire all files together so the app builds immediately
- run a build before finishing
- preserve the app’s repeatable pattern: **tech + level → AI-generated quiz → results with hints**

## Validation Checklist
Before considering the work complete:
- `./gradlew build` succeeds
- quiz generation path is wired from UI → ViewModel → Repository → GeminiService
- loading/error/results states render correctly
- API key configuration is documented in `local.properties`
- the project can be reused as a template for future quiz-generator apps

