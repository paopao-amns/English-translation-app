package com.paopao.englearn.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paopao.englearn.data.local.AppDatabase
import com.paopao.englearn.data.local.entity.VocabBookEntity
import com.paopao.englearn.data.repository.VocabBookRepository
import com.paopao.englearn.domain.model.WordResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class VocabularyUiState(
    val words: List<VocabBookEntity> = emptyList(),
    val isExporting: Boolean = false
)

class VocabularyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = VocabBookRepository(db.vocabBookDao())

    private val _uiState = MutableStateFlow(VocabularyUiState())
    val uiState: StateFlow<VocabularyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAll().collect { words ->
                _uiState.update { it.copy(words = words) }
            }
        }
    }

    fun removeWord(word: String) {
        viewModelScope.launch {
            repository.removeWord(word)
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                // TODO: Phase 8 polish — handle export properly
            } finally {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }

    fun exportAnki() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                // TODO: Phase 8 polish
            } finally {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }
}
