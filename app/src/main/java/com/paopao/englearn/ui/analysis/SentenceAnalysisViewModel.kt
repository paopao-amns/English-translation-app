package com.paopao.englearn.ui.analysis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paopao.englearn.EngLearnApp
import com.paopao.englearn.data.local.AppDatabase
import com.paopao.englearn.data.repository.SentenceRepository
import com.paopao.englearn.domain.model.SentenceAnalysis
import com.paopao.englearn.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SentenceAnalysisUiState(
    val sentence: String = "",
    val sentenceHash: String = "",
    val analysis: SentenceAnalysis? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class SentenceAnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = (application as EngLearnApp).settingsDataStore
    private val db = AppDatabase.getInstance(application)
    private val sentenceRepository = SentenceRepository(settingsDataStore, db.sentenceCacheDao())

    private val _uiState = MutableStateFlow(SentenceAnalysisUiState())
    val uiState: StateFlow<SentenceAnalysisUiState> = _uiState.asStateFlow()

    /**
     * Analyze a sentence. Called from manual input or OCR selection.
     */
    fun analyzeSentence(sentence: String) {
        if (sentence.isBlank()) return

        val hash = sentenceRepository.hashSentence(sentence)
        _uiState.update {
            it.copy(sentence = sentence, sentenceHash = hash, isLoading = true, error = null, analysis = null)
        }

        viewModelScope.launch {
            when (val result = sentenceRepository.analyze(sentence)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, analysis = result.data, error = null)
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

    fun clearAnalysis() {
        _uiState.update { it.copy(analysis = null, error = null) }
    }
}
