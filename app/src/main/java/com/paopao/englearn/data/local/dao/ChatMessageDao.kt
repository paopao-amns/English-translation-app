package com.paopao.englearn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.paopao.englearn.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages WHERE sentence_cache_id = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySession(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sentence_cache_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesList(sessionId: Long): List<ChatMessageEntity>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE sentence_cache_id = :sessionId")
    suspend fun countBySession(sessionId: Long): Int

    @Insert
    suspend fun insert(message: ChatMessageEntity): Long

    /**
     * Delete the oldest message(s) for a session — used to enforce max 10 limit.
     */
    @Query("""
        DELETE FROM chat_messages
        WHERE id = (
            SELECT id FROM chat_messages
            WHERE sentence_cache_id = :sessionId
            ORDER BY timestamp ASC LIMIT 1
        )
    """)
    suspend fun deleteOldest(sessionId: Long)

    @Query("DELETE FROM chat_messages WHERE sentence_cache_id = :sessionId")
    suspend fun deleteBySession(sessionId: Long)
}
