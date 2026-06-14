package com.paopao.englearn.data.repository

import com.paopao.englearn.data.local.dao.SentenceCacheDao
import com.paopao.englearn.data.local.dao.WordCacheDao
import com.paopao.englearn.data.local.entity.SentenceCacheEntity
import com.paopao.englearn.data.local.entity.WordCacheEntity
import com.paopao.englearn.data.preferences.SettingsDataStore
import com.paopao.englearn.data.remote.*
import com.paopao.englearn.domain.model.PreloadStrategy
import com.paopao.englearn.domain.model.WordResult
import com.paopao.englearn.util.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

/**
 * Repository for Mode 2 pre-loading.
 * Implements three strategies: Individual, Batch, Hybrid.
 */
class PreloadRepository(
    private val settingsDataStore: SettingsDataStore,
    private val wordCacheDao: WordCacheDao,
    private val sentenceCacheDao: SentenceCacheDao,
    private val wordRepository: WordRepository,
    private val sentenceRepository: SentenceRepository
) {
    companion object {
        private const val TIMEOUT_MS = 60_000L
        private const val HYBRID_THRESHOLD = 2000  // chars
    }

    data class PreloadProgress(
        val current: Int,
        val total: Int,
        val message: String
    )

    /**
     * Execute preload based on the configured strategy.
     *
     * @param text The article text to pre-analyze
     * @param articleId Unique ID for this article/scan
     * @param onProgress Callback for progress updates
     * @param strategy The preload strategy to use
     * @return Pair of highlighted words set and sentence texts list
     */
    suspend fun preload(
        text: String,
        articleId: String,
        strategy: PreloadStrategy,
        onProgress: (PreloadProgress) -> Unit
    ): Result<Pair<Set<String>, List<String>>> {
        return try {
            when (strategy) {
                PreloadStrategy.INDIVIDUAL -> preloadIndividual(text, articleId, onProgress)
                PreloadStrategy.BATCH -> preloadBatch(text, articleId, onProgress)
                PreloadStrategy.HYBRID -> preloadHybrid(text, articleId, onProgress)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Result.error("预加载超时")
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.error("预加载失败：${e.message}")
        }
    }

    /**
     * Strategy A: Individual — identify then process one by one.
     */
    private suspend fun preloadIndividual(
        text: String,
        articleId: String,
        onProgress: (PreloadProgress) -> Unit
    ): Result<Pair<Set<String>, List<String>>> {
        // Step 1: Identify difficult items
        onProgress(PreloadProgress(0, 1, "正在识别难点词句..."))
        val identification = identifyDifficultItems(text)
        if (identification == null) return Result.error("识别难点失败")

        val words = identification.difficultWords
        val sentences = identification.complexSentences
        val totalItems = words.size + sentences.size

        // Step 2: Process each word
        val highlightedWords = mutableSetOf<String>()
        words.forEachIndexed { index, word ->
            onProgress(PreloadProgress(index + 1, totalItems, "分析单词: $word"))
            val result = wordRepository.lookupFromApi(word)
            if (result is Result.Success) {
                highlightedWords.add(word.lowercase())
            }
        }

        // Step 3: Process each sentence
        val processedSentences = mutableListOf<String>()
        sentences.forEachIndexed { index, sentence ->
            onProgress(
                PreloadProgress(
                    words.size + index + 1,
                    totalItems,
                    "分析句子..."
                )
            )
            val result = sentenceRepository.analyzeFromApi(sentence)
            if (result is Result.Success) {
                processedSentences.add(sentence)
            }
        }

        return Result.success(Pair(highlightedWords, processedSentences))
    }

    /**
     * Strategy B: Batch — single API call returns everything.
     */
    private suspend fun preloadBatch(
        text: String,
        articleId: String,
        onProgress: (PreloadProgress) -> Unit
    ): Result<Pair<Set<String>, List<String>>> {
        onProgress(PreloadProgress(0, 1, "批量分析中..."))

        return try {
            val (apiService, apiConfig) = NetworkModule.createApiService(settingsDataStore)
            val settings = settingsDataStore.settingsFlow.first()

            val request = ChatCompletionRequest(
                model = settings.modelName,
                messages = listOf(
                    ChatMessage(role = "system", content = PromptTemplates.BATCH_ANALYSIS_SYSTEM),
                    ChatMessage(role = "user", content = "分析以下文本：\n\n${text.take(3000)}")
                ),
                temperature = 0.3
            )

            val response = withTimeout(TIMEOUT_MS) {
                apiService.chatCompletion(apiConfig.path, request)
            }

            if (!response.isSuccessful) {
                return Result.error("批量分析请求失败 (${response.code()})")
            }

            val content = response.body()?.choices?.firstOrNull()?.message?.content
                ?: return Result.error("批量分析返回空内容")

            val json = extractJson(content)
            val batch = NetworkModule.json.decodeFromString<BatchAnalysisJson>(json)

            // Save words to cache
            val words = batch.words.map { w ->
                WordResult(
                    word = w.word ?: "",
                    pos = w.pos,
                    meaning = w.meaning,
                    example = w.example,
                    exampleCn = w.exampleCn
                )
            }
            wordRepository.saveToCache(words, articleId)

            // Save sentences to cache
            batch.sentences.forEach { entry ->
                if (entry.sentence != null && entry.analysis != null) {
                    val analysisJson = NetworkModule.json.encodeToString(
                        SentenceAnalysisJson.serializer(), entry.analysis
                    )
                    sentenceRepository.saveToCache(entry.sentence, analysisJson, articleId)
                }
            }

            val highlightedWords = words.map { it.word.lowercase() }.toSet()
            val sentences = batch.sentences.mapNotNull { it.sentence }

            onProgress(PreloadProgress(1, 1, "完成！"))
            Result.success(Pair(highlightedWords, sentences))

        } catch (e: Exception) {
            Result.error("批量分析失败：${e.message}")
        }
    }

    /**
     * Strategy C: Hybrid — short text: batch, long text: individual.
     */
    private suspend fun preloadHybrid(
        text: String,
        articleId: String,
        onProgress: (PreloadProgress) -> Unit
    ): Result<Pair<Set<String>, List<String>>> {
        return if (text.length <= HYBRID_THRESHOLD) {
            preloadBatch(text, articleId, onProgress)
        } else {
            // Split into chunks, batch each chunk
            val chunks = text.chunked(HYBRID_THRESHOLD)
            val allWords = mutableSetOf<String>()
            val allSentences = mutableListOf<String>()

            chunks.forEachIndexed { index, chunk ->
                onProgress(PreloadProgress(index + 1, chunks.size, "处理第 ${index + 1}/${chunks.size} 段..."))
                when (val result = preloadBatch(chunk, "$articleId-$index", onProgress)) {
                    is Result.Success -> {
                        allWords.addAll(result.data.first)
                        allSentences.addAll(result.data.second)
                    }
                    is Result.Error -> {
                        // Continue with next chunk on error
                    }
                    is Result.Loading -> {}
                }
            }

            onProgress(PreloadProgress(chunks.size, chunks.size, "完成！"))
            Result.success(Pair(allWords, allSentences))
        }
    }

    /**
     * Call AI to identify difficult words and complex sentences in text.
     */
    private suspend fun identifyDifficultItems(text: String): PreloadIdentificationJson? {
        return try {
            val (apiService, apiConfig) = NetworkModule.createApiService(settingsDataStore)
            val settings = settingsDataStore.settingsFlow.first()

            val request = ChatCompletionRequest(
                model = settings.modelName,
                messages = listOf(
                    ChatMessage(role = "system", content = PromptTemplates.PRELOAD_IDENTIFICATION_SYSTEM),
                    ChatMessage(role = "user", content = "分析文本：\n\n${text.take(3000)}")
                ),
                temperature = 0.3
            )

            val response = withTimeout(30_000L) {
                apiService.chatCompletion(apiConfig.path, request)
            }

            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                    ?: return null
                val json = extractJson(content)
                NetworkModule.json.decodeFromString<PreloadIdentificationJson>(json)
            } else null
        } catch (e: Exception) {
            null
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
