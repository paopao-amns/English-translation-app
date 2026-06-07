package com.paopao.englearn.data.repository

import com.paopao.englearn.data.local.dao.SentenceCacheDao
import com.paopao.englearn.data.local.entity.SentenceCacheEntity
import com.paopao.englearn.data.preferences.SettingsDataStore
import com.paopao.englearn.data.remote.*
import com.paopao.englearn.domain.model.*
import com.paopao.englearn.util.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import java.security.MessageDigest

class SentenceRepository(
    private val settingsDataStore: SettingsDataStore,
    private val sentenceCacheDao: SentenceCacheDao? = null
) {
    companion object {
        private const val TIMEOUT_MS = 30_000L
        private const val MAX_CHARS = 500
    }

    /**
     * Analyze sentence with cache-first strategy.
     * Returns the analysis AND the raw JSON for chat context.
     */
    suspend fun analyze(sentence: String): Result<SentenceAnalysis> {
        val trimmed = sentence.trim()
        if (trimmed.isEmpty()) return Result.error("请输入要分析的句子")
        val limited = if (trimmed.length > MAX_CHARS) trimmed.take(MAX_CHARS) else trimmed
        val hash = hashSentence(limited)

        // 1. Check Room cache
        sentenceCacheDao?.let { dao ->
            val cached = dao.getByHash(hash)
            if (cached != null) {
                return parseAnalysisJson(cached.analysisJson)
            }
        }

        // 2. Cache miss — call API
        return analyzeFromApi(limited)
    }

    suspend fun analyzeFromApi(sentence: String): Result<SentenceAnalysis> {
        val trimmed = sentence.trim()
        val limited = if (trimmed.length > MAX_CHARS) trimmed.take(MAX_CHARS) else trimmed
        val hash = hashSentence(limited)

        return try {
            val (apiService, apiConfig) = NetworkModule.createApiService(settingsDataStore)
            val settings = settingsDataStore.settingsFlow.first()

            val request = ChatCompletionRequest(
                model = settings.modelName,
                messages = listOf(
                    ChatMessage(role = "system", content = PromptTemplates.SENTENCE_ANALYSIS_SYSTEM),
                    ChatMessage(role = "user", content = "分析句子：$limited")
                ),
                temperature = 0.3
            )

            val response = withTimeout(TIMEOUT_MS) {
                apiService.chatCompletion(apiConfig.path, request)
            }

            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                    ?: return Result.error("API 返回了空内容")

                val json = extractJson(content)

                // 3. Save to Room cache
                sentenceCacheDao?.insert(
                    SentenceCacheEntity(
                        sentenceHash = hash,
                        sentenceText = limited,
                        analysisJson = json
                    )
                )

                parseAnalysisJson(json)
            } else {
                Result.error("请求失败 (${response.code()})")
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Result.error("请求超时（30秒）")
        } catch (e: Exception) {
            Result.error("网络请求失败：${e.localizedMessage ?: "未知错误"}")
        }
    }

    suspend fun saveToCache(
        sentence: String,
        analysisJson: String,
        articleId: String? = null
    ) {
        sentenceCacheDao?.insert(
            SentenceCacheEntity(
                sentenceHash = hashSentence(sentence),
                sentenceText = sentence,
                analysisJson = analysisJson,
                articleId = articleId
            )
        )
    }

    fun hashSentence(sentence: String): String {
        val normalized = sentence.trim().lowercase().replace(Regex("\\s+"), " ")
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(normalized.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
    }

    private fun parseAnalysisJson(json: String): Result<SentenceAnalysis> {
        return try {
            val dto = NetworkModule.json.decodeFromString<SentenceAnalysisJson>(json)
            Result.success(
                SentenceAnalysis(
                    sentenceType = dto.sentenceType,
                    basicComponents = dto.basicComponents?.let {
                        BasicComponents(subject = it.subject, predicate = it.predicate, `object` = it.`object`)
                    },
                    modifiers = dto.modifiers?.let {
                        Modifiers(attribute = it.attribute, adverbial = it.adverbial, complement = it.complement)
                    },
                    clauses = dto.clauses?.map {
                        Clause(type = it.type ?: "", text = it.text ?: "", function = it.function ?: "")
                    },
                    translation = dto.translation,
                    explanation = dto.explanation
                )
            )
        } catch (e: Exception) {
            Result.error("解析分析结果失败：${e.message}")
        }
    }

    private fun extractJson(raw: String): String {
        var text = raw.trim()
        val fenceRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```")
        val match = fenceRegex.find(text)
        if (match != null) text = match.groupValues[1].trim()
        return text
    }
}
