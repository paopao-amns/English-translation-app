package com.paopao.englearn.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Objects for the OpenAI-compatible API.
 * Field names use snake_case to match the API JSON format.
 */

// ── Request bodies ──────────────────────────────────────────

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.3
)

@Serializable
data class ChatMessage(
    val role: String,   // "system" or "user"
    val content: String
)

// ── Response structures ─────────────────────────────────────

@Serializable
data class ChatCompletionResponse(
    val id: String? = null,
    val `object`: String? = null,
    val created: Long? = null,
    val model: String? = null,
    val choices: List<Choice>? = null,
    val usage: Usage? = null
)

@Serializable
data class Choice(
    val index: Int? = null,
    val message: ChoiceMessage? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class ChoiceMessage(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int? = null,
    @SerialName("completion_tokens")
    val completionTokens: Int? = null,
    @SerialName("total_tokens")
    val totalTokens: Int? = null
)

// ── Word Lookup JSON (returned in AI response content) ──────

@Serializable
data class WordLookupJson(
    val word: String? = null,
    val pos: String? = null,
    val meaning: String? = null,
    val example: String? = null,
    @SerialName("example_cn")
    val exampleCn: String? = null
)

// ── Sentence Analysis JSON (returned in AI response content) ─

@Serializable
data class SentenceAnalysisJson(
    @SerialName("sentence_type")
    val sentenceType: String? = null,
    @SerialName("basic_components")
    val basicComponents: BasicComponentsJson? = null,
    val modifiers: ModifiersJson? = null,
    val clauses: List<ClauseJson>? = null,
    val translation: String? = null,
    val explanation: String? = null
)

@Serializable
data class BasicComponentsJson(
    val subject: String? = null,
    val predicate: String? = null,
    val `object`: String? = null
)

@Serializable
data class ModifiersJson(
    val attribute: String? = null,
    val adverbial: String? = null,
    val complement: String? = null
)

@Serializable
data class ClauseJson(
    val type: String? = null,
    val text: String? = null,
    val function: String? = null
)

// ── Preload Identification ──────────────────────────────────

@Serializable
data class PreloadIdentificationJson(
    @SerialName("difficult_words")
    val difficultWords: List<String> = emptyList(),
    @SerialName("complex_sentences")
    val complexSentences: List<String> = emptyList()
)

// ── Batch Analysis (Strategy B) ─────────────────────────────

@Serializable
data class BatchAnalysisJson(
    val words: List<WordLookupJson> = emptyList(),
    val sentences: List<SentenceAnalysisEntry> = emptyList()
)

@Serializable
data class SentenceAnalysisEntry(
    val sentence: String? = null,
    val analysis: SentenceAnalysisJson? = null
)
