package com.example.dummy_quiz_using_agent.repository

import com.example.dummy_quiz_using_agent.data.ShoppingDecisionException
import com.example.dummy_quiz_using_agent.data.ShoppingDecisionService
import com.example.dummy_quiz_using_agent.model.ProductInsight
import com.example.dummy_quiz_using_agent.model.ShoppingPreference

interface ShoppingRepository {
    suspend fun analyzeProduct(
        productLink: String,
        preference: ShoppingPreference
    ): ShoppingDecisionResult
}

class DefaultShoppingRepository(
    private val service: ShoppingDecisionService
) : ShoppingRepository {

    override suspend fun analyzeProduct(
        productLink: String,
        preference: ShoppingPreference
    ): ShoppingDecisionResult {
        return try {
            val insight = service.analyzeProduct(
                productLink = productLink,
                preference = preference
            )
            ShoppingDecisionResult.Success(insight)
        } catch (error: ShoppingDecisionException) {
            ShoppingDecisionResult.Failure(
                message = error.message ?: DEFAULT_ERROR_MESSAGE,
                canRetry = error !is ShoppingDecisionException.MissingApiKey
            )
        }
    }

    private companion object {
        private const val DEFAULT_ERROR_MESSAGE =
            "Could not analyze product right now. Please try again."
    }
}

sealed interface ShoppingDecisionResult {
    data class Success(val insight: ProductInsight) : ShoppingDecisionResult

    data class Failure(
        val message: String,
        val canRetry: Boolean
    ) : ShoppingDecisionResult
}

