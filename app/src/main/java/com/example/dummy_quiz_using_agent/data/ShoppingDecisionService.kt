package com.example.dummy_quiz_using_agent.data

import com.example.dummy_quiz_using_agent.model.AlternativeProduct
import com.example.dummy_quiz_using_agent.model.FakeReviewRiskLevel
import com.example.dummy_quiz_using_agent.model.ProductInsight
import com.example.dummy_quiz_using_agent.model.ShoppingPreference
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.json.JSONArray
import org.json.JSONObject

class ShoppingDecisionService(
    private val apiKey: String,
    private val modelName: String = "gemini-3.1-flash-lite-preview" // gemini-2.5-flash
) {

    suspend fun analyzeProduct(
        productLink: String,
        preference: ShoppingPreference
    ): ProductInsight {
        if (apiKey.isBlank()) {
            throw ShoppingDecisionException.MissingApiKey
        }

        val model = GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.2f
                maxOutputTokens = 4096
            }
        )

        try {
            val response = model.generateContent(buildPrompt(productLink, preference))
            val rawText = response.text?.trim().orEmpty()
            return parseResponse(rawText)
        } catch (throwable: Throwable) {
            throw mapException(throwable)
        }
    }

    private fun buildPrompt(productLink: String, preference: ShoppingPreference): String {
        return """
You are a Smart Shopping Decision Agent.
Analyze this product page URL and buyer sentiment: $productLink

User preference: ${preference.displayName}

Return raw JSON only. No markdown. Compact response.
{
  "productTitle": "product name",
  "reviewSummary": "1-2 sentences on common themes",
  "fakeReviewRisk": {
    "level": "LOW|MEDIUM|HIGH",
    "score": 0-100,
    "signals": ["reason1", "reason2"]
  },
  "alternatives": [
    {"name": "product", "reason": "why better"}
  ],
  "personalizedSuggestion": "1 sentence recommendation"
}

Constraints:
- Keep all text concise.
- Max 2-3 signals, max 2 alternatives.
- If page data unavailable, note uncertainty in reviewSummary.
        """.trimIndent()
    }

    private fun parseResponse(rawText: String): ProductInsight {
        if (rawText.isBlank()) {
            throw ShoppingDecisionException.ResponseFormat("Received empty response from model.")
        }

        val cleanedJson = rawText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val rootObject = try {
            JSONObject(cleanedJson)
        } catch (e: Exception) {
            throw ShoppingDecisionException.ResponseFormat("Could not parse model response as JSON.", e)
        }

        val productTitle = rootObject.optString("productTitle").ifBlank { "Unknown product" }
        val reviewSummary = rootObject.optString("reviewSummary").ifBlank {
            "Not enough review context was available to summarize confidently."
        }

        val fakeReviewObject = rootObject.optJSONObject("fakeReviewRisk") ?: JSONObject()
        val riskLevel = FakeReviewRiskLevel.fromRaw(fakeReviewObject.optString("level"))
        val riskScore = fakeReviewObject.optInt("score", 50).coerceIn(0, 100)
        val signalsArray = fakeReviewObject.optJSONArray("signals") ?: JSONArray()
        val signals = (0 until signalsArray.length()).mapNotNull { index ->
            signalsArray.optString(index).takeIf { it.isNotBlank() }
        }.ifEmpty {
            listOf("Insufficient evidence to strongly classify review authenticity.")
        }

        val alternativesArray = rootObject.optJSONArray("alternatives") ?: JSONArray()
        val alternatives = buildList {
            for (index in 0 until alternativesArray.length()) {
                val item = alternativesArray.optJSONObject(index) ?: continue
                val name = item.optString("name")
                val reason = item.optString("reason")
                if (name.isBlank() || reason.isBlank()) continue
                add(AlternativeProduct(name = name, reason = reason))
            }
        }

        val personalizedSuggestion = rootObject.optString("personalizedSuggestion").ifBlank {
            "Pick the option that best matches your preference after comparing verified buyer feedback."
        }

        return ProductInsight(
            productTitle = productTitle,
            reviewSummary = reviewSummary,
            fakeReviewRiskLevel = riskLevel,
            fakeReviewRiskScore = riskScore,
            fakeReviewSignals = signals,
            alternatives = alternatives,
            personalizedSuggestion = personalizedSuggestion
        )
    }

    private fun mapException(throwable: Throwable): ShoppingDecisionException {
        if (throwable is ShoppingDecisionException) {
            return throwable
        }

        val message = throwable.message.orEmpty().lowercase()
        return when {
            throwable is UnknownHostException || message.contains("unable to resolve host") -> {
                ShoppingDecisionException.Network(
                    "No internet connection. Check your network and retry."
                )
            }
            throwable is SocketTimeoutException || message.contains("timeout") -> {
                ShoppingDecisionException.Timeout("The request timed out. Please retry.")
            }
            throwable is IOException -> {
                ShoppingDecisionException.Network("Network error while contacting Gemini.")
            }
            message.contains("quota") || message.contains("429") ||
            message.contains("resource exhausted") -> {
                ShoppingDecisionException.QuotaExceeded
            }
            else -> ShoppingDecisionException.Unknown(
                "Unexpected error while analyzing product.",
                throwable
            )
        }
    }
}

sealed class ShoppingDecisionException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data object MissingApiKey : ShoppingDecisionException(
        "Missing API key. Add GEMINI_API_KEY to local.properties and rebuild the app."
    )

    data object QuotaExceeded : ShoppingDecisionException(
        "Gemini quota is exhausted. Try later."
    )

    class Network(message: String, cause: Throwable? = null) : ShoppingDecisionException(message, cause)

    class Timeout(message: String, cause: Throwable? = null) : ShoppingDecisionException(message, cause)

    class ServiceUnavailable(message: String, cause: Throwable? = null) : ShoppingDecisionException(message, cause)

    class ResponseFormat(message: String, cause: Throwable? = null) : ShoppingDecisionException(message, cause)

    class Unknown(message: String, cause: Throwable? = null) : ShoppingDecisionException(message, cause)
}

