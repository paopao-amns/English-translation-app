package com.paopao.englearn.data.repository

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.paopao.englearn.data.local.dao.VocabBookDao
import com.paopao.englearn.data.local.entity.VocabBookEntity
import com.paopao.englearn.domain.model.WordResult
import kotlinx.coroutines.flow.Flow
import java.io.File

class VocabBookRepository(private val dao: VocabBookDao) {

    fun getAll(): Flow<List<VocabBookEntity>> = dao.getAll()

    suspend fun isSaved(word: String): Boolean {
        return dao.getByWord(word.lowercase()) != null
    }

    suspend fun addWord(wordResult: WordResult) {
        dao.insert(
            VocabBookEntity(
                word = wordResult.word.lowercase(),
                pos = wordResult.pos,
                meaning = wordResult.meaning,
                example = wordResult.example,
                exampleCn = wordResult.exampleCn
            )
        )
    }

    suspend fun removeWord(word: String) {
        dao.deleteByWord(word.lowercase())
    }

    suspend fun toggleWord(wordResult: WordResult): Boolean {
        val existing = dao.getByWord(wordResult.word.lowercase())
        return if (existing != null) {
            dao.deleteByWord(wordResult.word.lowercase())
            false  // Now unsaved
        } else {
            addWord(wordResult)
            true  // Now saved
        }
    }

    /**
     * Export vocabulary to CSV and share.
     */
    suspend fun exportCsv(context: Context): File {
        val words = dao.getAllSuspend()

        val file = File(context.cacheDir, "vocabulary_${System.currentTimeMillis()}.csv")
        val csv = buildString {
            append("﻿")  // BOM for Excel Chinese support
            appendLine("Word,Part of Speech,Meaning,Example,Example (Chinese)")
            words.forEach { word ->
                appendLine(
                    listOf(
                        word.word,
                        word.pos ?: "",
                        word.meaning ?: "",
                        word.example ?: "",
                        word.exampleCn ?: ""
                    ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
                )
            }
        }
        file.writeText(csv)
        return file
    }

    /**
     * Export to Anki-compatible CSV format.
     * Format: Front,Back (word on front, meaning+example on back)
     */
    suspend fun exportAnki(context: Context): File {
        val words = dao.getAllSuspend()

        val file = File(context.cacheDir, "anki_vocabulary_${System.currentTimeMillis()}.csv")
        val csv = buildString {
            append("﻿")
            appendLine("Front,Back")
            words.forEach { word ->
                val front = word.word
                val back = buildString {
                    if (!word.pos.isNullOrBlank()) appendLine("【词性】${word.pos}")
                    if (!word.meaning.isNullOrBlank()) appendLine("【释义】${word.meaning}")
                    if (!word.example.isNullOrBlank()) appendLine("【例句】${word.example}")
                    if (!word.exampleCn.isNullOrBlank()) append("【翻译】${word.exampleCn}")
                }
                appendLine("\"${front.replace("\"", "\"\"")}\",\"${back.replace("\"", "\"\"")}\"")
            }
        }
        file.writeText(csv)
        return file
    }

    /**
     * Share a file via Intent.
     */
    fun shareFile(context: Context, file: File, mimeType: String = "text/csv") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "导出生词本"))
    }
}
