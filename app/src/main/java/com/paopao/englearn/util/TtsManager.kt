package com.paopao.englearn.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Wraps Android's TextToSpeech engine for word pronunciation.
 * Supports both US and UK English voices.
 */
class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context) { status ->
            isInitialized = status == TextToSpeech.SUCCESS
            if (isInitialized) {
                tts?.language = Locale.US
            }
        }
    }

    /**
     * Speak a word aloud in English.
     */
    fun speak(text: String) {
        if (!isInitialized || tts == null) return

        // Use QUEUE_FLUSH to interrupt any current speech
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "englearn_tts_${System.currentTimeMillis()}")
    }

    /**
     * Stop any current speech.
     */
    fun stop() {
        tts?.stop()
    }

    /**
     * Check if TTS is ready.
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Release TTS resources. Call when no longer needed.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
