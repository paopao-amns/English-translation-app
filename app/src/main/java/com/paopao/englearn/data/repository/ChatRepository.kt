package com.paopao.englearn.data.repository

import com.paopao.englearn.data.local.dao.ChatMessageDao
import com.paopao.englearn.data.local.entity.ChatMessageEntity
import com.paopao.englearn.data.preferences.SettingsDataStore
import com.paopao.englearn.data.remote.*
import com.paopao.englearn.domain.model.ChatMessage
import com.paopao.englearn.util.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

/**
 * Repository for interactive Q&A after sentence analysis.
 * Manages chat sessions, message persistence, and API calls.
 */
class ChatRepository(
    private val settingsDataStore: SettingsDataStore,
    private val chatMessageDao: ChatMessageDao
) {
    companion object {
        private const val TIMEOUT_MS = 30_000L
        const val MAX_MESSAGES = 10
    }

    /**
     * Get all messages for a chat session.
     */
    suspend fun getMessages(sessionId: Long): List<ChatMessage> {
        return chatMessageDao.getMessagesList(sessionId).map { it.toDomain() }
    }

    /**
     * Send a follow-up question about an analyzed sentence.
     * Includes full context (sentence + analysis) so the AI can answer specifically.
     */
    suspend fun askQuestion(
        sessionId: Long,
        sentenceText: String,
        analysisJson: String,
        question: String
    ): Result<ChatMessage> {
        return try {
            val (apiService, apiConfig) = NetworkModule.createApiService(settingsDataStore)
            val settings = settingsDataStore.settingsFlow.first()

            // Build context: include previous messages
            val previousMessages = chatMessageDao.getMessagesList(sessionId)
            val contextBuilder = StringBuilder()
            contextBuilder.appendLine("原句：$sentenceText")
            contextBuilder.appendLine("句子分析结果：$analysisJson")
            if (previousMessages.isNotEmpty()) {
                contextBuilder.appendLine("之前的对话：")
                previousMessages.forEach { msg ->
                    contextBuilder.appendLine("${msg.role}: ${msg.content}")
                }
            }
            contextBuilder.appendLine("用户追问：$question")

            val request = ChatCompletionRequest(
                model = settings.modelName,
                messages = listOf(
                    ChatMessage(role = "system", content = PromptTemplates.QA_SYSTEM),
                    ChatMessage(role = "user", content = contextBuilder.toString())
                ),
                temperature = 0.5   // Slightly higher for more natural answers
            )

            val response = withTimeout(TIMEOUT_MS) {
                apiService.chatCompletion(apiConfig.path, request)
            }

            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                    ?: return Result.error("API 返回了空内容")

                // Save question
                val questionEntity = ChatMessageEntity(
                    sentenceCacheId = sessionId,
                    sentenceText = sentenceText,
                    analysisJson = analysisJson,
                    role = "user",
                    content = question
                )
                chatMessageDao.insert(questionEntity)

                // Save answer
                val answerEntity = ChatMessageEntity(
                    sentenceCacheId = sessionId,
                    sentenceText = sentenceText,
                    analysisJson = analysisJson,
                    role = "assistant",
                    content = content
                )
                chatMessageDao.insert(answerEntity)

                // Enforce max 10 messages
                enforceMaxMessages(sessionId)

                Result.success(answerEntity.toDomain())
            } else {
                Result.error("请求失败 (${response.code()}): ${response.errorBody()?.string()?.take(200) ?: ""}")
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Result.error("请求超时（30秒），请检查网络")
        } catch (e: Exception) {
            Result.error("网络请求失败：${e.localizedMessage ?: "未知错误"}")
        }
    }

    /**
     * Delete all messages for a session.
     */
    suspend fun clearSession(sessionId: Long) {
        chatMessageDao.deleteBySession(sessionId)
    }

    /**
     * Enforce max 10 messages by deleting oldest.
     */
    private suspend fun enforceMaxMessages(sessionId: Long) {
        var count = chatMessageDao.countBySession(sessionId)
        while (count > MAX_MESSAGES) {
            chatMessageDao.deleteOldest(sessionId)
            count = chatMessageDao.countBySession(sessionId)
        }
    }

    private fun ChatMessageEntity.toDomain(): ChatMessage {
        return ChatMessage(
            id = id,
            role = role,
            content = content,
            timestamp = timestamp
        )
    }
}
