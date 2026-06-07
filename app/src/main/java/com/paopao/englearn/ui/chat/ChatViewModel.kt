package com.paopao.englearn.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paopao.englearn.EngLearnApp
import com.paopao.englearn.data.local.AppDatabase
import com.paopao.englearn.data.repository.ChatRepository
import com.paopao.englearn.domain.model.ChatMessage
import com.paopao.englearn.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val sessionId: Long = 0,
    val sentenceText: String = "",
    val analysisJson: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = ""
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = (application as EngLearnApp).settingsDataStore
    private val db = AppDatabase.getInstance(application)
    private val chatRepository = ChatRepository(settingsDataStore, db.chatMessageDao())

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /**
     * Initialize chat with a sentence analysis session.
     * Loads existing messages from Room.
     */
    fun initSession(sessionId: Long, sentenceText: String, analysisJson: String) {
        _uiState.update {
            it.copy(sessionId = sessionId, sentenceText = sentenceText, analysisJson = analysisJson)
        }
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            val messages = chatRepository.getMessages(_uiState.value.sessionId)
            _uiState.update { it.copy(messages = messages) }
        }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendQuestion() {
        val state = _uiState.value
        val question = state.inputText.trim()
        if (question.isBlank()) return

        _uiState.update { it.copy(inputText = "", isLoading = true, error = null) }

        viewModelScope.launch {
            when (val result = chatRepository.askQuestion(
                sessionId = state.sessionId,
                sentenceText = state.sentenceText,
                analysisJson = state.analysisJson,
                question = question
            )) {
                is Result.Success -> {
                    loadMessages()  // Reload all messages
                    _uiState.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clearSession(_uiState.value.sessionId)
            _uiState.update { it.copy(messages = emptyList(), error = null) }
        }
    }
}
