package com.paopao.englearn.ui.ocr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paopao.englearn.ui.analysis.SentenceAnalysisScreen
import com.paopao.englearn.ui.components.WordCard
import com.paopao.englearn.ui.lookup.WordLookupViewModel
import com.paopao.englearn.util.TtsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    articleText: String,
    onNavigateBack: () -> Unit,
    onNavigateToAnalysis: (sentence: String) -> Unit,
    readingVM: ReadingViewModel = viewModel()
) {
    val readingState by readingVM.uiState.collectAsState()
    val wordLookupVM: WordLookupViewModel = viewModel()
    val wordUiState by wordLookupVM.uiState.collectAsState()
    val context = LocalContext.current
    val appContext = context.applicationContext
    val ttsManager = remember { TtsManager(appContext) }

    DisposableEffect(Unit) {
        onDispose {
            ttsManager.shutdown()
        }
    }

    var showWordSheet by remember { mutableStateOf(false) }
    var tappedWord by remember { mutableStateOf("") }
    var selectedText by remember { mutableStateOf("") }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(articleText)) }

    // Detect text selection changes
    val selectedSentence = textFieldValue.selection.let { sel ->
        if (sel.length > 0) {
            val start = minOf(sel.start, sel.end)
            val end = maxOf(sel.start, sel.end)
            articleText.substring(start, end).trim()
        } else ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("阅读文章") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // Mode toggle
                    FilterChip(
                        selected = readingState.mode == ReadingMode.PRELOAD,
                        onClick = {
                            readingVM.setMode(
                                if (readingState.mode == ReadingMode.ON_DEMAND)
                                    ReadingMode.PRELOAD else ReadingMode.ON_DEMAND
                            )
                        },
                        label = { Text(readingState.mode.label) },
                        leadingIcon = {
                            Icon(
                                if (readingState.mode == ReadingMode.PRELOAD)
                                    Icons.Default.Bolt else Icons.Default.TouchApp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            )
        },
        bottomBar = {
            // Show analyze button when text is selected
            if (selectedSentence.isNotBlank() && selectedSentence.length >= 2) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "已选择: «${selectedSentence.take(30)}${if (selectedSentence.length > 30) "..." else ""}»",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                onNavigateToAnalysis(selectedSentence)
                            }
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("分析句子")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Preload progress
            if (readingState.isPreloading) {
                LinearProgressIndicator(
                    progress = { readingState.preloadProgress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = readingState.preloadMessage,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Preload complete message
            if (!readingState.isPreloading && readingState.highlightedWords.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚡ ${readingState.preloadMessage}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Tappable article text
            TappableArticleText(
                text = articleText,
                highlightedWords = readingState.highlightedWords,
                onWordTap = { word ->
                    tappedWord = word
                    wordLookupVM.lookupWord(word)
                    showWordSheet = true
                },
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            )

            // Preload action bar (Mode 2)
            if (readingState.mode == ReadingMode.PRELOAD && !readingState.isPreloading) {
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "预加载模式：提前分析全文",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { readingVM.startPreload(articleText) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("预加载")
                    }
                }
            }

            HorizontalDivider()

            // Manual word input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = tappedWord,
                    onValueChange = { tappedWord = it },
                    placeholder = { Text("查单词...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                IconButton(
                    onClick = {
                        if (tappedWord.isNotBlank()) {
                            wordLookupVM.lookupWord(tappedWord.trim())
                            showWordSheet = true
                        }
                    },
                    enabled = tappedWord.isNotBlank()
                ) {
                    Icon(Icons.Default.Search, contentDescription = "查询")
                }
            }
        }
    }

    // Word lookup bottom sheet
    if (showWordSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWordSheet = false }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Word input in sheet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tappedWord,
                        onValueChange = { tappedWord = it },
                        placeholder = { Text("输入单词...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (tappedWord.isNotBlank()) {
                                wordLookupVM.lookupWord(tappedWord.trim())
                            }
                        },
                        enabled = tappedWord.isNotBlank() && !wordUiState.isLoading
                    ) {
                        Text("查询")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Error
                if (wordUiState.error != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = wordUiState.error ?: "",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Loading
                if (wordUiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "正在查询「${wordUiState.word}」...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Result
                wordUiState.result?.let { result ->
                    WordCard(
                        wordResult = result,
                        onSpeak = { ttsManager.speak(result.word) },
                        onSave = { /* TODO: Phase 6 */ }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
