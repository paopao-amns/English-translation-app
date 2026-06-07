package com.paopao.englearn.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.paopao.englearn.ui.analysis.SentenceAnalysisScreen
import com.paopao.englearn.ui.chat.ChatScreen
import com.paopao.englearn.ui.home.HomeScreen
import com.paopao.englearn.ui.ocr.OcrScreen
import com.paopao.englearn.ui.settings.SettingsScreen
import com.paopao.englearn.ui.settings.VocabularyScreen
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Main navigation graph for the app.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // ── Home ──────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToOcr = {
                    navController.navigate(Screen.Ocr.route)
                },
                onNavigateToVocabulary = {
                    navController.navigate(Screen.Vocabulary.route)
                },
                onNavigateToManualLookup = {
                    navController.navigate("manual_lookup")
                },
                onNavigateToManualAnalysis = {
                    navController.navigate("manual_analysis")
                }
            )
        }

        // ── Settings ──────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Manual Word Lookup ────────────────────────
        composable("manual_lookup") {
            com.paopao.englearn.ui.lookup.ManualLookupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Manual Sentence Analysis ──────────────────
        composable("manual_analysis") {
            SentenceAnalysisScreen(
                initialSentence = null,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { sessionId, sentenceText, analysisJson ->
                    navController.navigate(
                        "chat/$sessionId?text=${URLEncoder.encode(sentenceText, "UTF-8")}&json=${URLEncoder.encode(analysisJson, "UTF-8")}"
                    )
                }
            )
        }

        // ── Sentence Analysis (from OCR selection) ────
        composable(
            route = "sentence_analysis?sentence={sentence}",
            arguments = listOf(
                navArgument("sentence") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val sentence = backStackEntry.arguments?.getString("sentence") ?: ""
            SentenceAnalysisScreen(
                initialSentence = sentence,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { sessionId, sentenceText, analysisJson ->
                    navController.navigate(
                        "chat/$sessionId?text=${URLEncoder.encode(sentenceText, "UTF-8")}&json=${URLEncoder.encode(analysisJson, "UTF-8")}"
                    )
                }
            )
        }

        // ── Chat Q&A ──────────────────────────────────
        composable(
            route = "chat/{sessionId}?text={sentenceText}&json={analysisJson}",
            arguments = listOf(
                navArgument("sessionId") { type = NavType.LongType },
                navArgument("sentenceText") { type = NavType.StringType; defaultValue = "" },
                navArgument("analysisJson") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            val sentenceText = backStackEntry.arguments?.getString("sentenceText")?.let {
                URLDecoder.decode(it, "UTF-8")
            } ?: ""
            val analysisJson = backStackEntry.arguments?.getString("analysisJson")?.let {
                URLDecoder.decode(it, "UTF-8")
            } ?: ""

            ChatScreen(
                sessionId = sessionId,
                sentenceText = sentenceText,
                analysisJson = analysisJson,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── OCR Screen ────────────────────────────────
        composable(Screen.Ocr.route) {
            OcrScreen(
                onNavigateBack = { navController.popBackStack() },
                onTextReady = { text ->
                    // Navigate to reading mode (Phase 5)
                    navController.navigate("reading_mode?text=${URLEncoder.encode(text, "UTF-8")}")
                }
            )
        }

        // ── Reading Mode ────────────────────────────
        composable(
            route = "reading_mode?text={text}",
            arguments = listOf(
                navArgument("text") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val text = backStackEntry.arguments?.getString("text")?.let {
                URLDecoder.decode(it, "UTF-8")
            } ?: ""

            ReadingScreen(
                articleText = text,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAnalysis = { sentence ->
                    navController.navigate(
                        "sentence_analysis?sentence=${URLEncoder.encode(sentence, "UTF-8")}"
                    )
                }
            )
        }

        composable(Screen.Vocabulary.route) {
            VocabularyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$title — 即将实现",
            style = MaterialTheme.typography.titleMedium
        )
    }
}
