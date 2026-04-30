package com.example.dummy_quiz_using_agent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dummy_quiz_using_agent.model.HomeStats
import com.example.dummy_quiz_using_agent.model.HomeUiState
import com.example.dummy_quiz_using_agent.repository.HomeRepository
import com.example.dummy_quiz_using_agent.repository.HomeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun loadHome() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = repository.loadHome()) {
                is HomeResult.Success -> HomeUiState.Success(result.data)
                is HomeResult.Failure -> HomeUiState.Error(
                    message = result.message,
                    canRetry = result.canRetry,
                    fallbackStats = defaultStats()
                )
            }
        }
    }

    fun retry() = loadHome()

    private fun defaultStats(): HomeStats {
        return HomeStats(
            lastQuizScorePercent = null,
            recentShoppingHint = null,
            quickTip = "Start with Quiz, then validate one real product in Shopping Agent."
        )
    }

    companion object {
        fun provideFactory(repository: HomeRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                        return HomeViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

