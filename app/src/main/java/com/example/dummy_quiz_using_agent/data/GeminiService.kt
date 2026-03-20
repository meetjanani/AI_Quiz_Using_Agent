package com.example.dummy_quiz_using_agent.data

import com.example.dummy_quiz_using_agent.model.ExperienceLevel
import com.example.dummy_quiz_using_agent.model.QuizQuestion
import com.example.dummy_quiz_using_agent.model.Technology
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.json.JSONArray
import org.json.JSONObject

class GeminiService(
    private val apiKey: String,
    private val modelName: String = "gemini-2.5-flash"
) {

    suspend fun generateQuiz(
        technology: Technology,
        experienceLevel: ExperienceLevel,
        questionCount: Int
    ): List<QuizQuestion> {
        if (apiKey.isBlank()) {
            throw GeminiServiceException.MissingApiKey()
        }

        val model = GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.4f
                maxOutputTokens = 2048
            }
        )

        var requestedCount = questionCount
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val prompt = buildPrompt(technology, experienceLevel, requestedCount)
                val response = model.generateContent(prompt)
                val rawText = response.text?.trim().orEmpty()
                val questions = parseResponse(rawText, expectedMinimumCount = requestedCount)
                return questions.take(questionCount)
            } catch (throwable: Throwable) {
                val mapped = mapException(throwable)
                val shouldRetry = mapped is GeminiServiceException.ResponseFormat && attempt < MAX_ATTEMPTS - 1
                if (!shouldRetry) {
                    throw mapped
                }
                requestedCount = maxOf(3, requestedCount - 2)
            }
        }

        throw GeminiServiceException.Unknown("Unable to generate quiz right now.")
    }

    private fun buildPrompt(
        technology: Technology,
        experienceLevel: ExperienceLevel,
        questionCount: Int
    ): String {
        return """
            Generate exactly $questionCount multiple-choice quiz questions.
            Topic: ${technology.promptTopic}
            Experience Level: ${experienceLevel.displayName}

            Strict output contract:
            - Return raw JSON only. No markdown fences. No explanations.
            - Return a JSON array of objects.
            - Each object must contain:
              - "question": string
              - "options": array of exactly 4 strings
              - "correctAnswerIndex": integer 0..3
              - "hint": short educational hint
            - Keep each question clear and concise.
        """.trimIndent()
    }

    private fun parseResponse(rawText: String, expectedMinimumCount: Int): List<QuizQuestion> {
        if (rawText.isBlank()) {
            throw GeminiServiceException.ResponseFormat("Received an empty response from the model.")
        }

        val cleanedJson = rawText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val questionArray = try {
            if (cleanedJson.startsWith("[")) {
                JSONArray(cleanedJson)
            } else {
                val wrapped = JSONObject(cleanedJson)
                wrapped.optJSONArray("questions")
                    ?: throw GeminiServiceException.ResponseFormat("Response is not a valid quiz JSON array.")
            }
        } catch (e: Exception) {
            throw GeminiServiceException.ResponseFormat("Could not parse model response as JSON.", e)
        }

        val parsedQuestions = buildList {
            for (index in 0 until questionArray.length()) {
                val questionObject = questionArray.optJSONObject(index) ?: continue
                val optionsArray = questionObject.optJSONArray("options") ?: JSONArray()
                val options = (0 until optionsArray.length()).mapNotNull { optionIndex ->
                    optionsArray.optString(optionIndex).takeIf { it.isNotBlank() }
                }
                val correctIndex = questionObject.optInt("correctAnswerIndex", -1)
                val questionText = questionObject.optString("question")
                val hint = questionObject.optString("hint")

                if (questionText.isBlank() || options.size != 4 || correctIndex !in 0..3 || hint.isBlank()) {
                    continue
                }

                add(
                    QuizQuestion(
                        id = size,
                        question = questionText,
                        options = options,
                        correctAnswerIndex = correctIndex,
                        hint = hint
                    )
                )
            }
        }

        if (parsedQuestions.size < expectedMinimumCount) {
            throw GeminiServiceException.ResponseFormat(
                "Model returned only ${parsedQuestions.size} valid questions."
            )
        }

        return parsedQuestions
    }

    private fun mapException(throwable: Throwable): GeminiServiceException {
        if (throwable is GeminiServiceException) {
            return throwable
        }

        val message = throwable.message.orEmpty().lowercase()
        return when {
            throwable is UnknownHostException || message.contains("unable to resolve host") -> {
                GeminiServiceException.Network("No internet connection. Check your network and retry.")
            }
            throwable is SocketTimeoutException || message.contains("timeout") -> {
                GeminiServiceException.Timeout("The request timed out. Please retry.")
            }
            throwable is IOException -> {
                GeminiServiceException.Network("Network error while contacting Gemini.")
            }
            message.contains("quota") || message.contains("429") || message.contains("resource exhausted") -> {
                GeminiServiceException.QuotaExceeded()
            }
            message.contains("404") || message.contains("model") && message.contains("not") -> {
                GeminiServiceException.InvalidModel("Gemini model '$modelName' is unavailable.")
            }
            else -> GeminiServiceException.Unknown("Unexpected error while generating quiz.", throwable)
        }
    }

    companion object {
        private const val MAX_ATTEMPTS = 2
    }
}

sealed class GeminiServiceException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class MissingApiKey : GeminiServiceException(
        "Missing API key. Add GEMINI_API_KEY to local.properties and rebuild the app."
    )

    class QuotaExceeded : GeminiServiceException(
        "Gemini quota is exhausted. Try later or use the sample quiz."
    )

    class Network(message: String, cause: Throwable? = null) : GeminiServiceException(message, cause)

    class Timeout(message: String, cause: Throwable? = null) : GeminiServiceException(message, cause)

    class InvalidModel(message: String, cause: Throwable? = null) : GeminiServiceException(message, cause)

    class ResponseFormat(message: String, cause: Throwable? = null) : GeminiServiceException(message, cause)

    class Unknown(message: String, cause: Throwable? = null) : GeminiServiceException(message, cause)
}

