package com.paopao.englearn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paopao.englearn.domain.model.WordResult

/**
 * Card displaying word lookup results.
 * Mirrors the Chrome extension's word card in popup.css.
 */
@Composable
fun WordCard(
    wordResult: WordResult,
    onSpeak: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Word header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = wordResult.word,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Speak button (TTS — Phase 3)
                    if (onSpeak != null) {
                        IconButton(onClick = onSpeak) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "发音",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    // Save to vocabulary (Phase 6)
                    if (onSave != null) {
                        IconButton(onClick = onSave) {
                            Icon(
                                Icons.Default.BookmarkBorder,
                                contentDescription = "收藏",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Part of speech badge
            if (!wordResult.pos.isNullOrBlank()) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = wordResult.pos,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Meaning
            if (!wordResult.meaning.isNullOrBlank()) {
                Text(
                    text = "释义",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = wordResult.meaning,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Example sentence
            if (!wordResult.example.isNullOrBlank()) {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "例句",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = wordResult.example,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!wordResult.exampleCn.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = wordResult.exampleCn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Empty state
            if (wordResult.pos.isNullOrBlank() && wordResult.meaning.isNullOrBlank()) {
                Text(
                    text = "未能获取到释义，请检查 API 配置或尝试其他单词",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
