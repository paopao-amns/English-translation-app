package com.paopao.englearn.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sentence_cache",
    indices = [Index(value = ["sentence_hash"], unique = true)]
)
data class SentenceCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "sentence_hash")
    val sentenceHash: String,

    @ColumnInfo(name = "sentence_text")
    val sentenceText: String,

    @ColumnInfo(name = "analysis_json")
    val analysisJson: String,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "article_id")
    val articleId: String? = null
)
