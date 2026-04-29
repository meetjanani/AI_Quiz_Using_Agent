package com.example.dummy_quiz_using_agent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dummy_quiz_using_agent.model.ShoppingPreference
import com.example.dummy_quiz_using_agent.model.ShoppingUiState
import com.example.dummy_quiz_using_agent.repository.ShoppingDecisionResult
import com.example.dummy_quiz_using_agent.repository.ShoppingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShoppingViewModel(
    private val repository: ShoppingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShoppingUiState>(ShoppingUiState.Input())
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    private var lastInputState = ShoppingUiState.Input()

    fun onProductLinkChanged(link: String) {
        _uiState.update { state ->
            val input = (state as? ShoppingUiState.Input) ?: lastInputState
            input.copy(productLink = link, validationError = null).also { lastInputState = it }
        }
    }

    fun onPreferenceChanged(preference: ShoppingPreference) {
        _uiState.update { state ->
            val input = (state as? ShoppingUiState.Input) ?: lastInputState
            input.copy(selectedPreference = preference, validationError = null).also { lastInputState = it }
        }
    }

    fun analyzeProduct() {
        val currentInput = (_uiState.value as? ShoppingUiState.Input) ?: lastInputState
        val trimmedLink = currentInput.productLink.trim()

        if (!isSupportedProductLink(trimmedLink)) {
            _uiState.value = currentInput.copy(validationError = INVALID_LINK_MESSAGE)
            return
        }

        val validInput = currentInput.copy(productLink = trimmedLink, validationError = null)
        lastInputState = validInput
        _uiState.value = ShoppingUiState.Loading()

        viewModelScope.launch {
            when (
                val result = repository.analyzeProduct(
                    productLink = validInput.productLink,
                    preference = validInput.selectedPreference
                )
            ) {
                is ShoppingDecisionResult.Success -> {
                    _uiState.value = ShoppingUiState.Result(result.insight)
                }
                is ShoppingDecisionResult.Failure -> {
                    _uiState.value = ShoppingUiState.Error(
                        message = result.message,
                        canRetry = result.canRetry
                    )
                }
            }
        }
    }

    fun retry() {
        _uiState.value = lastInputState
        analyzeProduct()
    }

    fun startNewAnalysis() {
        _uiState.value = lastInputState.copy(productLink = "", validationError = null)
    }

    private fun isSupportedProductLink(link: String): Boolean {
        if (link.isBlank()) return false
        if (!link.startsWith("http://") && !link.startsWith("https://")) return false

        return link.contains("amazon.", ignoreCase = true) ||
            link.contains("flipkart.", ignoreCase = true)
    }

    companion object {
        private const val INVALID_LINK_MESSAGE =
            "Paste a valid Amazon or Flipkart product URL to continue."

        fun provideFactory(repository: ShoppingRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ShoppingViewModel::class.java)) {
                        return ShoppingViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

