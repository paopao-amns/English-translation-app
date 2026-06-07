package com.paopao.englearn.ui.ocr

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paopao.englearn.data.repository.OcrRepository
import com.paopao.englearn.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OcrUiState(
    val recognizedText: String = "",
    val editableText: String = "",
    val isProcessing: Boolean = false,
    val error: String? = null,
    val mode: OcrMode = OcrMode.CHOOSE,
    // Mode 2 preload state (implemented in Phase 7)
    val isPreloading: Boolean = false,
    val preloadProgress: Float = 0f,
    val preloadTotal: Int = 0,
    val preloadCurrent: Int = 0
)

enum class OcrMode {
    CHOOSE,     // Show camera/gallery options
    CAMERA,     // Camera preview
    RESULT      // Show OCR result (editable)
}

class OcrViewModel(application: Application) : AndroidViewModel(application) {

    private val ocrRepository = OcrRepository()

    private val _uiState = MutableStateFlow(OcrUiState())
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    fun setMode(mode: OcrMode) {
        _uiState.update { it.copy(mode = mode, error = null) }
    }

    /**
     * Process a bitmap through ML Kit OCR.
     */
    fun processImage(bitmap: Bitmap) {
        _uiState.update { it.copy(isProcessing = true, error = null) }

        viewModelScope.launch {
            // Downscale for faster processing
            val scaled = withContext(Dispatchers.Default) {
                OcrRepository.downscaleBitmap(bitmap)
            }

            when (val result = ocrRepository.recognizeText(scaled)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            recognizedText = result.data,
                            editableText = result.data,
                            mode = OcrMode.RESULT
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isProcessing = false, error = result.message)
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun updateEditableText(text: String) {
        _uiState.update { it.copy(editableText = text) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
