package com.paopao.englearn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.paopao.englearn.data.local.entity.SentenceCacheEntity

@Dao
interface SentenceCacheDao {

    @Query("SELECT * FROM sentence_cache WHERE sentence_hash = :hash LIMIT 1")
    suspend fun getByHash(hash: String): SentenceCacheEntity?

    @Query("SELECT * FROM sentence_cache WHERE article_id = :articleId")
    suspend fun getByArticle(articleId: String): List<SentenceCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sentence: SentenceCacheEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sentences: List<SentenceCacheEntity>)

    @Query("DELETE FROM sentence_cache WHERE article_id = :articleId")
    suspend fun deleteByArticle(articleId: String)
}
