package com.paopao.englearn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.paopao.englearn.data.local.entity.VocabBookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabBookDao {

    @Query("SELECT * FROM vocab_book ORDER BY saved_at DESC")
    fun getAll(): Flow<List<VocabBookEntity>>

    @Query("SELECT * FROM vocab_book ORDER BY saved_at DESC")
    suspend fun getAllSuspend(): List<VocabBookEntity>

    @Query("SELECT * FROM vocab_book WHERE word = :word LIMIT 1")
    suspend fun getByWord(word: String): VocabBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(word: VocabBookEntity): Long

    @Query("DELETE FROM vocab_book WHERE word = :word")
    suspend fun deleteByWord(word: String)

    @Query("SELECT COUNT(*) FROM vocab_book")
    suspend fun count(): Int
}
