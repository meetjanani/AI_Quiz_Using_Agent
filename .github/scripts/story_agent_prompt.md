# Android Project — Story Implementation Agent Prompt

This file is the **architecture specification** read by Copilot Agent when implementing
any Jira story or bug in this project. It is intentionally generic and applies to every
feature, not just shopping or quiz.

---

## Project Identity

- **Package:** `com.example.dummy_quiz_using_agent`
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material3
- **State:** `StateFlow` / `MutableStateFlow`
- **Architecture:** MVVM (Model → Service → Repository → ViewModel → UI)
- **Async:** Kotlin coroutines (`viewModelScope.launch`)
- **Navigation:** Jetpack Navigation Compose (`NavHost` in `MainActivity.kt`)
- **Min SDK:** 23+, compiled for Android 12+ Splash Screen API compatibility

---

## Layer Rules (follow exactly for every feature)

### 1. Model layer — `app/src/main/java/.../model/`

Create a new `<Feature>Models.kt` file (e.g., `SplashModels.kt`) containing:

```kotlin
// Domain data classes
data class MyFeatureData(...)

// UiState sealed interface — ALWAYS follow this exact shape:
sealed interface MyFeatureUiState {
    data object Loading : MyFeatureUiState           // or data class with message
    data class Success(val data: MyFeatureData) : MyFeatureUiState
    data class Error(val message: String, val canRetry: Boolean) : MyFeatureUiState
    // Add Input state if the screen has user inputs
}
```

Rules:
- Use `sealed interface`, never `sealed class`
- Loading state has no data (or just a string message)
- Error state always includes `canRetry: Boolean`
- Never put Android imports in model layer

---

### 2. Service / data layer — `app/src/main/java/.../data/`

Create `<Feature>Service.kt` for API calls, business logic, or SDK operations.

```kotlin
class MyFeatureService(private val apiKey: String = "") {
    suspend fun doWork(...): MyResult {
        // suspend fun, runs in coroutine
    }
}

// Sealed exception class (same pattern as GeminiServiceException)
sealed class MyFeatureException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data object MissingConfig : MyFeatureException("...")
    class Network(message: String, cause: Throwable? = null) : MyFeatureException(message, cause)
    class Unknown(message: String, cause: Throwable? = null) : MyFeatureException(message, cause)
}
```

Rules:
- One service per feature
- All network/IO in suspend functions
- Use `mapException()` pattern to convert exceptions to domain types
- No ViewModel references inside service

---

### 3. Repository layer — `app/src/main/java/.../repository/`

Create `<Feature>Repository.kt`:

```kotlin
// Interface (enables testing)
interface MyFeatureRepository {
    suspend fun fetchData(...): MyFeatureResult
}

// Default implementation
class DefaultMyFeatureRepository(
    private val service: MyFeatureService
) : MyFeatureRepository {
    override suspend fun fetchData(...): MyFeatureResult {
        return try {
            MyFeatureResult.Success(service.doWork(...))
        } catch (e: MyFeatureException) {
            MyFeatureResult.Failure(message = e.message ?: DEFAULT_ERROR, canRetry = true)
        }
    }
    private companion object { private const val DEFAULT_ERROR = "..." }
}

// Result sealed interface (always in same file)
sealed interface MyFeatureResult {
    data class Success(val data: MyFeatureData) : MyFeatureResult
    data class Failure(val message: String, val canRetry: Boolean) : MyFeatureResult
}
```

---

### 4. ViewModel layer — `app/src/main/java/.../viewmodel/`

Create `<Feature>ViewModel.kt`:

```kotlin
class MyFeatureViewModel(
    private val repository: MyFeatureRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyFeatureUiState>(MyFeatureUiState.Loading)
    val uiState: StateFlow<MyFeatureUiState> = _uiState.asStateFlow()

    fun loadData() {
        _uiState.value = MyFeatureUiState.Loading
        viewModelScope.launch {
            when (val result = repository.fetchData()) {
                is MyFeatureResult.Success -> _uiState.value = MyFeatureUiState.Success(result.data)
                is MyFeatureResult.Failure -> _uiState.value = MyFeatureUiState.Error(result.message, result.canRetry)
            }
        }
    }

    fun retry() { loadData() }

    companion object {
        fun provideFactory(repository: MyFeatureRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MyFeatureViewModel(repository) as T
            }
    }
}
```

Rules:
- Always expose `StateFlow`, never `LiveData`
- Always provide `provideFactory()` companion function
- `retry()` method required whenever `canRetry` is used
- No direct Context usage in ViewModel

---

### 5. Compose UI layer — `app/src/main/java/.../ui/`

Create `<Feature>Screen.kt`:

```kotlin
@Composable
fun MyFeatureScreen(
    viewModel: MyFeatureViewModel,
    onBack: () -> Unit          // navigation callback
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is MyFeatureUiState.Loading -> MyFeatureLoadingContent()
        is MyFeatureUiState.Success -> MyFeatureSuccessContent(state.data, onBack)
        is MyFeatureUiState.Error   -> MyFeatureErrorContent(state, viewModel::retry, onBack)
    }
}
```

Rules:
- One file per screen
- `collectAsStateWithLifecycle()` — not `collectAsState()`
- Each major state has its own private `@Composable` function
- Use `LazyColumn` for lists, `Column + verticalScroll` for short scrollable content
- Use `Card`, `OutlinedTextField`, `Button`, `OutlinedButton` from Material3
- All user-visible strings → `strings.xml` (never hardcoded)
- All spacing values in `dp` (use 8, 12, 16, 20, 24 dp)

---

### 6. Navigation — `MainActivity.kt`

Add to the `AppRoute` enum:
```kotlin
MY_FEATURE("my_feature")
```

Add to `AppNavHost` NavHost:
```kotlin
composable(AppRoute.MY_FEATURE.route) {
    BackHandler { navController.popBackStack() }
    MyFeatureScreen(
        viewModel = myFeatureViewModel,
        onBack = { navController.popBackStack() }
    )
}
```

Add ViewModel + repository lazy init in `MainActivity`:
```kotlin
private val myFeatureRepository by lazy {
    DefaultMyFeatureRepository(service = MyFeatureService())
}
private val myFeatureViewModel: MyFeatureViewModel by viewModels {
    MyFeatureViewModel.provideFactory(myFeatureRepository)
}
```

Add navigation button to `HomeScreen.kt`:
```kotlin
OutlinedButton(onClick = onOpenMyFeature, modifier = Modifier.fillMaxWidth()) {
    Text(text = stringResource(R.string.home_open_my_feature))
}
```

---

### 7. Resources — `app/src/main/res/values/strings.xml`

Add all new strings here. Never hardcode UI text in Kotlin/Compose files.

```xml
<string name="my_feature_title">Feature Title</string>
<string name="my_feature_subtitle">Short description</string>
<!-- etc. -->
```

---

## Checklist for every implementation

Before finishing, verify:
- [ ] `<Feature>Models.kt` with UiState sealed interface
- [ ] `<Feature>Service.kt` with suspend functions and sealed exception
- [ ] `<Feature>Repository.kt` with interface + default impl + result sealed interface
- [ ] `<Feature>ViewModel.kt` with StateFlow + provideFactory
- [ ] `<Feature>Screen.kt` with per-state composables
- [ ] `AppRoute` enum updated in `MainActivity.kt`
- [ ] `AppNavHost` wired in `MainActivity.kt`
- [ ] ViewModel lazy init in `MainActivity`
- [ ] Navigation button added to `HomeScreen.kt`
- [ ] All strings added to `strings.xml`
- [ ] No hardcoded strings in Kotlin files
- [ ] No magic numbers — use named constants or `dp` values

---

## Reference implementations (already in this project)

Study these files before implementing:

| Layer | Reference file |
|-------|---------------|
| Model + UiState | `model/ShoppingModels.kt` |
| Service | `data/ShoppingDecisionService.kt` |
| Repository | `repository/ShoppingRepository.kt` |
| ViewModel | `viewmodel/ShoppingViewModel.kt` |
| Compose Screen | `ui/SmartShoppingScreen.kt` |
| Navigation | `MainActivity.kt` |
| Home nav entry | `ui/HomeScreen.kt` |
| Strings | `res/values/strings.xml` |

---

## Special cases

### Splash Screen (Android API)
- Use `androidx.core.splashscreen` library
- Implement as theme-based for Android 12+ compatibility
- ViewModel handles initialization logic (auth check, config load)
- Navigate to Home or Login on completion
- Do NOT block the main thread

### Bug fix stories
- Identify the affected layer(s) from the description
- Fix only the described behavior; do not refactor unrelated code
- Add a unit test if the bug is in a repository or ViewModel

### UI-only stories
- Skip Service and Repository if no data fetching is needed
- UiState can be a simple `data class` instead of sealed interface

