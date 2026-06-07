package com.paopao.englearn.util

/**
 * Utility for splitting text into words and sentences.
 * Used to make OCR-extracted text tappable.
 */
object TextTokenizer {

    /**
     * Split text into individual words with their character ranges.
     * A "word" is a sequence of letters (a-zA-Z), possibly with hyphens or apostrophes.
     */
    fun tokenizeWords(text: String): List<WordToken> {
        val regex = Regex("[a-zA-Z]+(?:[''-][a-zA-Z]+)*")
        return regex.findAll(text).map { match ->
            WordToken(
                word = match.value,
                start = match.range.first,
                end = match.range.last + 1
            )
        }.toList()
    }

    /**
     * Split text into sentences using punctuation boundaries.
     * Handles: . ! ? followed by space/capital letter
     */
    fun tokenizeSentences(text: String): List<SentenceToken> {
        val sentences = mutableListOf<SentenceToken>()
        val pattern = Regex("""[^.!?\n]+(?:[.!?]+|$)""")
        pattern.findAll(text).forEach { match ->
            val sentence = match.value.trim()
            if (sentence.isNotBlank()) {
                sentences.add(
                    SentenceToken(
                        text = sentence,
                        start = match.range.first,
                        end = match.range.last + 1
                    )
                )
            }
        }
        return sentences
    }

    /**
     * Get the word at a specific character position.
     * Returns null if no word exists at that position.
     */
    fun getWordAt(text: String, charIndex: Int): WordToken? {
        return tokenizeWords(text).find { charIndex in it.start until it.end }
    }

    data class WordToken(
        val word: String,
        val start: Int,
        val end: Int
    )

    data class SentenceToken(
        val text: String,
        val start: Int,
        val end: Int
    )
}
