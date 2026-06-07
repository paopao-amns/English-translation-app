package com.paopao.englearn.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.paopao.englearn.data.local.dao.*
import com.paopao.englearn.data.local.entity.*

/**
 * Room database for the English Learner app.
 * Stores cached word definitions, sentence analyses, chat messages, and vocabulary book.
 *
 * Schema versioning: fallbackToDestructiveMigration() during development;
 * proper Migration objects should be added before public release.
 */
@Database(
    entities = [
        WordCacheEntity::class,
        SentenceCacheEntity::class,
        ChatMessageEntity::class,
        VocabBookEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wordCacheDao(): WordCacheDao
    abstract fun sentenceCacheDao(): SentenceCacheDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun vocabBookDao(): VocabBookDao

    companion object {
        private const val DATABASE_NAME = "englearn.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
