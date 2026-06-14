package com.paopao.englearn.data.repository

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.paopao.englearn.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for OCR text recognition using Google ML Kit.
 * On-device recognition — no network required, works offline.
 */
class OcrRepository {

    /**
     * Recognize text from a bitmap image.
     * Runs on IO dispatcher to avoid blocking main thread.
     */
    suspend fun recognizeText(bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionResult = com.google.android.gms.tasks.Tasks.await(recognizer.process(inputImage))

            val text = visionResult.text
            if (text.isNotBlank()) {
                Result.success(text)
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

    companion object {
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
