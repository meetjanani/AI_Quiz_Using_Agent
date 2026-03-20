# Dummy Quiz Using Agent

Jetpack Compose quiz app that generates technology-specific quizzes by experience level using Gemini.

## Setup

1. Add your Gemini key to `local.properties`:

```properties
GEMINI_API_KEY=your_api_key_here
```

2. Build the app:

```bash
./gradlew build
```

## App flow

- Setup: choose technology, experience level, and question count.
- Loading: Gemini quiz generation runs.
- In Progress: answer generated questions.
- Results: score and incorrect answer review with hints.
- Error: retry, back, or fallback sample quiz.

