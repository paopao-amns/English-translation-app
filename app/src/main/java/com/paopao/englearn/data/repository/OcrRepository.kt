package com.paopao.englearn.data.repository

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.paopao.englearn.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for OCR text recognition using Google ML Kit.
 * On-device recognition — no network required, works offline.
 *
 * Now preserves paragraph structure: inter-paragraph gaps → double newlines,
 * intra-paragraph line breaks → single newlines,
 * first-line indent → leading spaces (when detectable from bounding boxes).
 */
class OcrRepository {

    /**
     * Recognize text from a bitmap image, preserving formatting.
     * Runs on IO dispatcher to avoid blocking main thread.
     */
    suspend fun recognizeText(bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionResult =
                com.google.android.gms.tasks.Tasks.await(recognizer.process(inputImage))

            val formatted = buildFormattedText(visionResult)
            if (formatted.isNotBlank()) {
                Result.success(formatted)
            } else {
                Result.error("未能识别到文字，请尝试调整角度或光线后重试")
            }
        } catch (e: Exception) {
            if (e.message?.contains("Waiting for the text recognition model") == true) {
                Result.error("正在下载识别模型，请稍后重试...")
            } else {
                Result.error("文字识别失败：${e.localizedMessage ?: "未知错误"}")
            }
        } finally {
            recognizer.close()
        }
    }

    // ── Formatting logic ────────────────────────────────────────────

    /**
     * Walk ML Kit's [TextBlock] → [Line] hierarchy and build a string
     * that preserves paragraph boundaries and first-line indentation.
     *
     * Heuristic:
     *  - Each [TextBlock] maps to one paragraph (ML Kit separates blocks by
     *    horizontal white-space gaps).
     *  - Block separation → two newlines.
     *  - Line separation within a block → one newline.
     *  - First-line indent: when the first line's left edge is ≥ 20 px
     *    right of the other lines in the *same* block, prepend four spaces.
     */
    private fun buildFormattedText(visionText: Text): String {
        val blocks = visionText.textBlocks
        if (blocks.isEmpty()) return visionText.text

        return buildString {
            for ((i, block) in blocks.withIndex()) {
                if (i > 0) append("\n\n")              // paragraph gap

                val lines = block.lines
                if (lines.isEmpty()) {
                    append(block.text.trimEnd())
                    continue
                }

                // ── Detect first-line indent ─────────────────────
                val firstLineLeft = lines.first().boundingBox?.left
                val otherLefts = lines.drop(1).mapNotNull { it.boundingBox?.left }

                val hasIndent = if (firstLineLeft != null && otherLefts.isNotEmpty()) {
                    val avgOtherLeft = otherLefts.average().toInt()
                    firstLineLeft - avgOtherLeft >= INDENT_THRESHOLD_PX
                } else false

                // ── Build block text ─────────────────────────────
                lines.forEachIndexed { j, line ->
                    if (j > 0) append("\n")             // intra-paragraph line break
                    if (hasIndent && j == 0) append("    ")  // 4-space indent
                    append(line.text.trimEnd())
                }
            }
        }
    }

    companion object {
        /** Pixels threshold for first-line indent detection. */
        private const val INDENT_THRESHOLD_PX = 20

        /**
         * Downscale a bitmap to a max dimension for faster ML Kit processing.
         */
        fun downscaleBitmap(bitmap: Bitmap, maxDimension: Int = 1024): Bitmap {
            val width = bitmap.width
            val height = bitmap.height
            if (width <= maxDimension && height <= maxDimension) return bitmap

            val ratio = minOf(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
            val newWidth = (width * ratio).toInt()
            val newHeight = (height * ratio).toInt()
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }
    }
}
