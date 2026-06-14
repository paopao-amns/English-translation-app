package com.paopao.englearn.ui.ocr

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.paopao.englearn.util.TextTokenizer

/**
 * Renders article text as tappable content.
 * Each English word gets a StringAnnotation so taps can be detected.
 * Pre-cached words (Mode 2) are highlighted with a background color.
 */
@Composable
fun TappableArticleText(
    text: String,
    highlightedWords: Set<String> = emptySet(),
    onWordTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val wordTokens = remember(text) { TextTokenizer.tokenizeWords(text) }

    val highlightColor = MaterialTheme.colorScheme.tertiary

    val annotatedString = remember(text, highlightedWords, highlightColor) {
        buildAnnotatedString {
            append(text)

            // Add word-level annotations for click detection
            wordTokens.forEach { token ->
                val isHighlighted = token.word.lowercase() in highlightedWords
                addStringAnnotation(
                    tag = "WORD",
                    annotation = token.word,
                    start = token.start,
                    end = token.end
                )
                if (isHighlighted) {
                    addStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline,
                            color = highlightColor
                        ),
                        start = token.start,
                        end = token.end
                    )
                }
            }
        }
    }

    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations("WORD", offset, offset)
                .firstOrNull()?.let { annotation ->
                    onWordTap(annotation.item)
                }
        }
    )
}
