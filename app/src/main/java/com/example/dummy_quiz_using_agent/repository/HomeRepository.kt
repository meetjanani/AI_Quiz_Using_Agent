package com.example.dummy_quiz_using_agent.repository

import com.example.dummy_quiz_using_agent.data.HomeService
import com.example.dummy_quiz_using_agent.data.HomeServiceException
import com.example.dummy_quiz_using_agent.model.HomeStats

interface HomeRepository {
    suspend fun loadHome(): HomeResult
}

class DefaultHomeRepository(
    private val service: HomeService
) : HomeRepository {

    override suspend fun loadHome(): HomeResult {
        return try {
            HomeResult.Success(service.loadHomeStats())
        } catch (e: HomeServiceException) {
            HomeResult.Failure(
                message = e.message ?: DEFAULT_ERROR_MESSAGE,
                canRetry = true
            )
        }
    }

    private companion object {
        private const val DEFAULT_ERROR_MESSAGE = "Could not load home insights."
    }
}

sealed interface HomeResult {
    data class Success(val data: HomeStats) : HomeResult
    data class Failure(
        val message: String,
        val canRetry: Boolean
    ) : HomeResult
}

