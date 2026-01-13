package io.github.ceracharlescc.copy4lm.domain.vo

import io.github.ceracharlescc.copy4lm.domain.service.TokenEstimator

/**
 * Tracks statistics about copied file content.
 */
internal data class CopyStats(
    val totalChars: Int = 0,
    val totalLines: Int = 0,
    val totalWords: Int = 0,
    val totalTokens: Int = 0
) {
    fun plus(content: String): CopyStats {
        val chars = content.length
        val lines = if (content.isEmpty()) 0 else content.count { it == '\n' } + 1
        val words = content.split(Regex("\\s+")).count { it.isNotBlank() }
        val tokens = TokenEstimator.estimate(content)

        return copy(
            totalChars = totalChars + chars,
            totalLines = totalLines + lines,
            totalWords = totalWords + words,
            totalTokens = totalTokens + tokens
        )
    }
}
