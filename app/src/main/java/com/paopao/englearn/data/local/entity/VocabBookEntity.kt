package com.paopao.englearn.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vocab_book",
    indices = [Index(value = ["word"], unique = true)]
)
data class VocabBookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "word")
    val word: String,

    val pos: String? = null,
    val meaning: String? = null,
    val example: String? = null,

    @ColumnInfo(name = "example_cn")
    val exampleCn: String? = null,

    @ColumnInfo(name = "saved_at")
    val savedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "review_count")
    val reviewCount: Int = 0
)
