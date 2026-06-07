package com.paopao.englearn.ui.lookup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paopao.englearn.EngLearnApp
import com.paopao.englearn.data.local.AppDatabase
import com.paopao.englearn.data.repository.WordRepository
import com.paopao.englearn.domain.model.WordResult
import com.paopao.englearn.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WordLookupUiState(
    val word: String = "",
    val result: WordResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class WordLookupViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = (application as EngLearnApp).settingsDataStore
    private val db = AppDatabase.getInstance(application)
    private val wordRepository = WordRepository(settingsDataStore, db.wordCacheDao())

    private val _uiState = MutableStateFlow(WordLookupUiState())
    val uiState: StateFlow<WordLookupUiState> = _uiState.asStateFlow()

    /**
     * Look up a word. Called when the user taps a word or manually enters one.
     */
    fun lookupWord(word: String) {
        if (word.isBlank()) return

        _uiState.update { it.copy(word = word, isLoading = true, error = null, result = null) }

        viewModelScope.launch {
            when (val result = wordRepository.lookup(word)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, result = result.data, error = null)
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
                is Result.Loading -> { /* handled by isLoading flag */ }
            }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(result = null, error = null) }
    }
}
