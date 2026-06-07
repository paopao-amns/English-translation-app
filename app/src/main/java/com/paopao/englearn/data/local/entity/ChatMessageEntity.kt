package com.paopao.englearn.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Chat message persisted in Room.
 * Scoped to a specific sentence analysis session via sentenceCacheId.
 * Max 10 messages per session — enforced at the Repository layer.
 */
@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["sentence_cache_id"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "sentence_cache_id")
    val sentenceCacheId: Long,

    @ColumnInfo(name = "sentence_text")
    val sentenceText: String,

    @ColumnInfo(name = "analysis_json")
    val analysisJson: String,

    @ColumnInfo(name = "role")
    val role: String,           // "user" or "assistant"

    val content: String,

    val timestamp: Long = System.currentTimeMillis()
)
