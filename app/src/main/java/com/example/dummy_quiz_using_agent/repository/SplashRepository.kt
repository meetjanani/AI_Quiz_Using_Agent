package com.example.dummy_quiz_using_agent.repository

import com.example.dummy_quiz_using_agent.data.SplashInitException
import com.example.dummy_quiz_using_agent.data.SplashInitService
import com.example.dummy_quiz_using_agent.model.SplashInitResult

// ── Repository interface ─────────────────────────────────────────────────────

interface SplashRepository {
    suspend fun initialize(): SplashResult
}

// ── Default implementation ───────────────────────────────────────────────────

class DefaultSplashRepository(
    private val service: SplashInitService
) : SplashRepository {

    override suspend fun initialize(): SplashResult {
        return try {
            val result = service.initialize()
            SplashResult.Success(result)
        } catch (e: SplashInitException) {
            SplashResult.Failure(
                message = e.message ?: DEFAULT_ERROR_MESSAGE,
                canRetry = true
            )
        }
    }

    private companion object {
        private const val DEFAULT_ERROR_MESSAGE =
            "App initialization failed. Please retry."
    }
}

// ── Result sealed interface ──────────────────────────────────────────────────

sealed interface SplashResult {
    data class Success(val data: SplashInitResult) : SplashResult
    data class Failure(val message: String, val canRetry: Boolean) : SplashResult
}

