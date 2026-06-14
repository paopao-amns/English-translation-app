package com.paopao.englearn.ui.ocr

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paopao.englearn.EngLearnApp
import com.paopao.englearn.data.local.AppDatabase
import com.paopao.englearn.data.preferences.SettingsDataStore
import com.paopao.englearn.data.repository.PreloadRepository
import com.paopao.englearn.data.repository.SentenceRepository
import com.paopao.englearn.data.repository.WordRepository
import com.paopao.englearn.domain.model.PreloadStrategy
import com.paopao.englearn.util.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ReadingUiState(
    val articleId: String = UUID.randomUUID().toString(),
    val mode: ReadingMode = ReadingMode.ON_DEMAND,
    val isPreloading: Boolean = false,
    val preloadProgress: Float = 0f,
    val preloadMessage: String = "",
    val highlightedWords: Set<String> = emptySet(),
    val preloadError: String? = null
)

enum class ReadingMode(val label: String) {
    ON_DEMAND("按需查询"),
    PRELOAD("预加载")
}

class ReadingViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = (application as EngLearnApp).settingsDataStore
    private val db = AppDatabase.getInstance(application)
    private val wordRepo = WordRepository(settingsDataStore, db.wordCacheDao())
    private val sentenceRepo = SentenceRepository(settingsDataStore, db.sentenceCacheDao())
    private val preloadRepo = PreloadRepository(
        settingsDataStore, db.wordCacheDao(), db.sentenceCacheDao(), wordRepo, sentenceRepo
    )

    private val _uiState = MutableStateFlow(ReadingUiState())
    val uiState: StateFlow<ReadingUiState> = _uiState.asStateFlow()

    private var preloadJob: Job? = null

    fun setMode(mode: ReadingMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun startPreload(text: String) {
        preloadJob?.cancel()

        preloadJob = viewModelScope.launch {
            val settings = settingsDataStore.settingsFlow.first()
            val strategy = settings.preloadStrategy
            val articleId = _uiState.value.articleId

            _uiState.update {
                it.copy(isPreloading = true, preloadProgress = 0f, preloadError = null)
            }

            when (val result = preloadRepo.preload(text, articleId, strategy) { progress ->
                _uiState.update {
                    it.copy(
                        preloadMessage = progress.message,
                        preloadProgress = if (progress.total > 0)
                            progress.current.toFloat() / progress.total
                        else 0f
                    )
                }
            }) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isPreloading = false,
                            preloadProgress = 1f,
                            highlightedWords = result.data.first,
                            preloadMessage = "预加载完成！${result.data.first.size} 个词，${result.data.second.size} 个句子"
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isPreloading = false, preloadError = result.message)
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun cancelPreload() {
        preloadJob?.cancel()
        _uiState.update { it.copy(isPreloading = false) }
    }
}
