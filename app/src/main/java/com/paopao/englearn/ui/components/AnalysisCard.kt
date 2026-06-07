package com.paopao.englearn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paopao.englearn.domain.model.BasicComponents
import com.paopao.englearn.domain.model.Modifiers
import com.paopao.englearn.domain.model.SentenceAnalysis
import com.paopao.englearn.ui.theme.Green500
import com.paopao.englearn.ui.theme.Orange500

/**
 * Full sentence analysis result card.
 * Mirrors the Chrome extension's analysis card with collapsible sections,
 * tables, clause cards, translation, and explanation.
 */
@Composable
fun AnalysisCard(
    analysis: SentenceAnalysis,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // ── Original Sentence (shown in parent screen) ─

        // Sentence type badge
        if (!analysis.sentenceType.isNullOrBlank()) {
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = analysis.sentenceType,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Basic Components ──────────────────────────
        if (analysis.basicComponents != null) {
            CollapsibleSection(title = "基本成分", initiallyExpanded = true) {
                ComponentsTable(analysis.basicComponents)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Modifiers ─────────────────────────────────
        if (analysis.modifiers != null) {
            CollapsibleSection(title = "修饰成分") {
                ModifiersTable(analysis.modifiers)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Clauses ───────────────────────────────────
        if (!analysis.clauses.isNullOrEmpty()) {
            CollapsibleSection(
                title = "从句分析（${analysis.clauses.size}）",
                initiallyExpanded = true
            ) {
                analysis.clauses.forEach { clause ->
                    ClauseItem(
                        type = clause.type,
                        text = clause.text,
                        function = clause.function
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Translation ───────────────────────────────
        if (!analysis.translation.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "中文翻译",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = analysis.translation,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Explanation ───────────────────────────────
        if (!analysis.explanation.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "构造解释",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = analysis.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Table displaying basic sentence components (subject, predicate, object).
 */
@Composable
private fun ComponentsTable(components: BasicComponents) {
    Column {
        ComponentRow("主语 (Subject)", components.subject)
        HorizontalDivider()
        ComponentRow("谓语 (Predicate)", components.predicate)
        if (!components.`object`.isNullOrBlank()) {
            HorizontalDivider()
            ComponentRow("宾语 (Object)", components.`object`)
        }
    }
}

/**
 * Table displaying modifier components.
 */
@Composable
private fun ModifiersTable(modifiers: Modifiers) {
    Column {
        if (!modifiers.attribute.isNullOrBlank()) {
            ComponentRow("定语 (Attribute)", modifiers.attribute)
            HorizontalDivider()
        }
        if (!modifiers.adverbial.isNullOrBlank()) {
            ComponentRow("状语 (Adverbial)", modifiers.adverbial)
            HorizontalDivider()
        }
        if (!modifiers.complement.isNullOrBlank()) {
            ComponentRow("补语 (Complement)", modifiers.complement)
        }
    }
}

@Composable
private fun ComponentRow(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f)
        )
    }
}

/**
 * Individual clause card with an orange left border accent.
 */
@Composable
private fun ClauseItem(type: String, text: String, function: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Orange accent bar (left border equivalent)
            Surface(
                modifier = Modifier
                    .width(4.dp)
                    .height(IntrinsicSize.Min),
                color = Orange500
            ) {}

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                if (type.isNotBlank()) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = type,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Orange500.copy(alpha = 0.15f),
                            labelColor = Orange500
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (text.isNotBlank()) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
                if (function.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = function,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
