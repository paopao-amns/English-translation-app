package com.paopao.englearn.ui.navigation

/**
 * Type-safe navigation routes for the app.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object Ocr : Screen("ocr")
    data object Vocabulary : Screen("vocabulary")

    data object WordLookup : Screen("word_lookup/{word}") {
        fun createRoute(word: String) = "word_lookup/${java.net.URLEncoder.encode(word, "UTF-8")}"
    }

    data object SentenceAnalysis : Screen("sentence_analysis/{sentenceHash}") {
        fun createRoute(sentenceHash: String) = "sentence_analysis/$sentenceHash"
    }

    data object Chat : Screen("chat/{sentenceCacheId}") {
        fun createRoute(sentenceCacheId: Long) = "chat/$sentenceCacheId"
    }
}
