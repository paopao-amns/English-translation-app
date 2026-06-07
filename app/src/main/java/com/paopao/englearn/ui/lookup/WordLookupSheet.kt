package com.paopao.englearn.ui.lookup

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paopao.englearn.ui.components.LoadingOverlay
import com.paopao.englearn.ui.components.WordCard

/**
 * Bottom sheet for word lookup. Can be used:
 * 1. Manual entry: shows a text field + search button
 * 2. Auto-lookup: immediately looks up the provided word
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordLookupSheet(
    initialWord: String? = null,
    onDismiss: () -> Unit,
    viewModel: WordLookupViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember(initialWord) { mutableStateOf(initialWord ?: "") }

    // Auto-lookup if word is provided
    LaunchedEffect(initialWord) {
        if (!initialWord.isNullOrBlank() && initialWord != uiState.word) {
            viewModel.lookupWord(initialWord)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 32.dp)
    ) {
        // Handle bar
        HorizontalDivider(
            modifier = Modifier
                .width(40.dp)
                .align(Alignment.CenterHorizontally),
            thickness = 4.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Search bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("输入英文单词...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.lookupWord(inputText.trim())
                    }
                },
                enabled = inputText.isNotBlank() && !uiState.isLoading
            ) {
                Text("查询")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        if (uiState.error != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = uiState.error ?: "",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Loading / Result
        when {
            uiState.isLoading -> {
                LoadingOverlay(message = "正在查询「${uiState.word}」...")
            }
            uiState.result != null -> {
                WordCard(
                    wordResult = uiState.result!!,
                    onSpeak = { /* TODO: Phase 3 — TTS */ },
                    onSave = { /* TODO: Phase 6 — save to vocabulary */ }
                )
            }
            initialWord == null -> {
                // No search yet — show hint
                Text(
                    text = "输入单词或点击文章中的单词以查看释义",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
