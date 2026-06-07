package com.paopao.englearn.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "word_cache",
    indices = [Index(value = ["word"], unique = true)]
)
data class WordCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "word")
    val word: String,

    val pos: String? = null,
    val meaning: String? = null,
    val example: String? = null,

    @ColumnInfo(name = "example_cn")
    val exampleCn: String? = null,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "article_id")
    val articleId: String? = null
)
