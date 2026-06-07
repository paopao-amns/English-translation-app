package com.paopao.englearn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.paopao.englearn.data.local.entity.WordCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordCacheDao {

    @Query("SELECT * FROM word_cache WHERE word = :word LIMIT 1")
    suspend fun getByWord(word: String): WordCacheEntity?

    @Query("SELECT * FROM word_cache WHERE article_id = :articleId")
    suspend fun getByArticle(articleId: String): List<WordCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(word: WordCacheEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordCacheEntity>)

    @Query("DELETE FROM word_cache WHERE article_id = :articleId")
    suspend fun deleteByArticle(articleId: String)

    @Query("SELECT * FROM word_cache ORDER BY cached_at DESC LIMIT :limit")
    fun getRecentWords(limit: Int = 50): Flow<List<WordCacheEntity>>
}
