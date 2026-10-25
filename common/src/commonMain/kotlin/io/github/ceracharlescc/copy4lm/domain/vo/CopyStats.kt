@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package io.github.ceracharlescc.copy4lm.domain.vo

import kotlin.js.JsExport

/** Tracks statistics about copied file content without allocating a list of words. */
@JsExport
data class CopyStats(
    val totalChars: Int = 0,
    val totalLines: Int = 0,
    val totalWords: Int = 0,
    val totalTokens: Int = 0
) {
    fun plus(content: String): CopyStats {
        var lines = if (content.isEmpty()) 0 else 1
        var words = 0
        var punctuation = 0
        var inWord = false

        for (char in content) {
            if (char == '\n') lines++
            if (char.isWhitespace()) {
                inWord = false
            } else if (!inWord) {
                words++
                inWord = true
            }
            when (char) {
                ';', '{', '}', '(', ')', '[', ']', ',' -> punctuation++
            }
        }

        return CopyStats(
            totalChars = totalChars + content.length,
            totalLines = totalLines + lines,
            totalWords = totalWords + words,
            totalTokens = totalTokens + words + punctuation
        )
    }
}
