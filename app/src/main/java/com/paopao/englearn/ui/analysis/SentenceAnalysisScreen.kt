package com.paopao.englearn.ui.analysis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paopao.englearn.data.remote.NetworkModule
import com.paopao.englearn.data.remote.SentenceAnalysisJson
import com.paopao.englearn.ui.components.AnalysisCard
import com.paopao.englearn.ui.components.LoadingOverlay
import kotlinx.serialization.encodeToString

/**
 * Full-screen sentence analysis view.
 * Supports:
 * - Manual sentence input with character counter (max 500)
 * - API-powered sentence analysis with structured display
 * - Navigation to chat Q&A (Phase 3)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceAnalysisScreen(
    initialSentence: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (sessionId: Long, sentenceText: String, analysisJson: String) -> Unit = { _, _, _ -> },
    viewModel: SentenceAnalysisViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember(initialSentence) { mutableStateOf(initialSentence ?: "") }
    val maxChars = 500

    // Auto-analyze if initial sentence provided
    LaunchedEffect(initialSentence) {
        if (!initialSentence.isNullOrBlank() && initialSentence != uiState.sentence) {
            inputText = initialSentence
            viewModel.analyzeSentence(initialSentence)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("句子分析") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (uiState.analysis != null) {
                        IconButton(onClick = {
                            uiState.analysis?.let { analysis ->
                                val json = NetworkModule.json.encodeToString(analysis)
                                onNavigateToChat(0L, uiState.sentence, json)
                            }
                        }) {
                            Icon(
                                Icons.Default.QuestionAnswer,
                                contentDescription = "追问"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Input area (only shown when no analysis yet or user wants to re-analyze) ─
            if (initialSentence == null) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        if (it.length <= maxChars) inputText = it
                    },
                    label = { Text("输入英文句子") },
                    placeholder = { Text("输入或粘贴英文句子...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                // Character counter
                Text(
                    text = "${inputText.length}/$maxChars",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (inputText.length > 450)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 4.dp),
                    textAlign = TextAlign.End
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.analyzeSentence(inputText) },
                    enabled = inputText.isNotBlank() && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(Icons.Default.Analytics, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("分析句子")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Error ──────────────────────────────────
            if (uiState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.error ?: "",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Loading ────────────────────────────────
            if (uiState.isLoading) {
                LoadingOverlay(message = "正在分析句子结构...")
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Analysis Result ────────────────────────
            if (uiState.analysis != null) {
                // Original sentence display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = uiState.sentence,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detailed analysis
                AnalysisCard(analysis = uiState.analysis!!)

                Spacer(modifier = Modifier.height(16.dp))

                // Ask a question button
                OutlinedButton(
                    onClick = {
                        uiState.analysis?.let { analysis ->
                            val json = NetworkModule.json.encodeToString(analysis)
                            onNavigateToChat(0L, uiState.sentence, json)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QuestionAnswer, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("追问关于这个句子的语法问题")
                }
            }
        }
    }
}
