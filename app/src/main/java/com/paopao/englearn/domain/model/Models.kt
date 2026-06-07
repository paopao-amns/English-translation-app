package com.paopao.englearn.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain models for the English Learner app.
 * These mirror the JSON structures returned by the Chrome extension's API.
 * @Serializable allows passing analysis data between screens as JSON.
 */

@Serializable
data class WordResult(
    val word: String,
    val pos: String? = null,
    val meaning: String? = null,
    val example: String? = null,
    val exampleCn: String? = null
)

@Serializable
data class BasicComponents(
    val subject: String? = null,
    val predicate: String? = null,
    val `object`: String? = null
)

@Serializable
data class Modifiers(
    val attribute: String? = null,
    val adverbial: String? = null,
    val complement: String? = null
)

@Serializable
data class Clause(
    val type: String,
    val text: String,
    val function: String
)

@Serializable
data class SentenceAnalysis(
    val sentenceType: String? = null,
    val basicComponents: BasicComponents? = null,
    val modifiers: Modifiers? = null,
    val clauses: List<Clause>? = null,
    val translation: String? = null,
    val explanation: String? = null
)

data class ChatMessage(
    val id: Long = 0,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class OcrArticle(
    val id: String,
    val title: String,
    val text: String,
    val scannedAt: Long = System.currentTimeMillis()
)

enum class PreloadStrategy(val label: String, val description: String) {
    INDIVIDUAL("逐个分析", "先识别难点词句，再逐项调用API分析"),
    BATCH("批量分析", "全文一次性发送给AI，返回所有分析结果"),
    HYBRID("智能混合", "短文章批量分析，长文章分段处理")
}
