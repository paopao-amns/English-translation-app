package com.paopao.englearn.data.repository

import com.paopao.englearn.data.local.dao.WordCacheDao
import com.paopao.englearn.data.local.entity.WordCacheEntity
import com.paopao.englearn.data.preferences.SettingsDataStore
import com.paopao.englearn.data.remote.*
import com.paopao.englearn.domain.model.WordResult
import com.paopao.englearn.util.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

/**
 * Repository for word lookup with Room cache-first strategy.
 * - Check Room cache → return if found
 * - Call API → save to Room → return
 */
class WordRepository(
    private val settingsDataStore: SettingsDataStore,
    private val wordCacheDao: WordCacheDao? = null  // Nullable for backward compat
) {
    companion object {
        private const val TIMEOUT_MS = 30_000L
    }

    /**
     * Look up a word with cache-first strategy.
     */
    suspend fun lookup(word: String): Result<WordResult> {
        val normalized = word.trim().lowercase()

        // 1. Check Room cache
        wordCacheDao?.let { dao ->
            val cached = dao.getByWord(normalized)
            if (cached != null) {
                return Result.success(cached.toDomain())
            }
        }

        // 2. Cache miss — call API
        return lookupFromApi(normalized)
    }

    /**
     * Force API call (skip cache). Used in Mode 2 preload.
     */
    suspend fun lookupFromApi(word: String): Result<WordResult> {
        val normalized = word.trim().lowercase()

        return try {
            val (apiService, apiConfig) = NetworkModule.createApiService(settingsDataStore)
            val settings = settingsDataStore.settingsFlow.first()

            val request = ChatCompletionRequest(
                model = settings.modelName,
                messages = listOf(
                    ChatMessage(role = "system", content = PromptTemplates.WORD_LOOKUP_SYSTEM),
                    ChatMessage(role = "user", content = "查询单词：$normalized")
                ),
                temperature = 0.3
            )

            val response = withTimeout(TIMEOUT_MS) {
                apiService.chatCompletion(apiConfig.path, request)
            }

            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                    ?: return Result.error("API 返回了空内容，请检查模型名称是否正确")

                val json = extractJson(content)
                val dto = NetworkModule.json.decodeFromString<WordLookupJson>(json)

                val result = WordResult(
                    word = dto.word ?: normalized,
                    pos = dto.pos,
                    meaning = dto.meaning,
                    example = dto.example,
                    exampleCn = dto.exampleCn
                )

                // 3. Save to Room cache
                wordCacheDao?.insert(
                    WordCacheEntity(
                        word = normalized,
                        pos = result.pos,
                        meaning = result.meaning,
                        example = result.example,
                        exampleCn = result.exampleCn
                    )
                )

                Result.success(result)
            } else {
                Result.error(parseHttpError(response.code(), response.errorBody()?.string()))
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Result.error("请求超时（30秒），请检查网络或端点配置")
        } catch (e: Exception) {
            Result.error("网络请求失败：${e.localizedMessage ?: "未知错误"}")
        }
    }

    /**
     * Bulk save words to cache (used in Mode 2 batch preload).
     */
    suspend fun saveToCache(words: List<WordResult>, articleId: String? = null) {
        wordCacheDao?.insertAll(
            words.map { w ->
                WordCacheEntity(
                    word = w.word.lowercase(),
                    pos = w.pos,
                    meaning = w.meaning,
                    example = w.example,
                    exampleCn = w.exampleCn,
                    articleId = articleId
                )
            }
        )
    }

    private fun extractJson(raw: String): String {
        var text = raw.trim()
        val fenceRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```")
        val match = fenceRegex.find(text)
        if (match != null) {
            text = match.groupValues[1].trim()
        }
        return text
    }

    private fun parseHttpError(code: Int, errorBody: String?): String {
        return when (code) {
            401, 403 -> "API Key 无效或未授权 (${code})"
            404 -> "API 端点未找到 (404)"
            429 -> "请求过于频繁，请稍后重试 (429)"
            in 500..599 -> "服务器错误 (${code})"
            else -> "请求失败 (${code})"
        }
    }

    private fun WordCacheEntity.toDomain(): WordResult {
        return WordResult(
            word = word,
            pos = pos,
            meaning = meaning,
            example = example,
            exampleCn = exampleCn
        )
    }
}
