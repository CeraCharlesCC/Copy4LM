package com.github.mwguerra.copyfilecontent.domain

/**
 * Tracks statistics about copied file content.
 */
class CopyStats {
    var totalChars: Int = 0
        private set
    var totalLines: Int = 0
        private set
    var totalWords: Int = 0
        private set
    var totalTokens: Int = 0
        private set

    fun add(content: String) {
        totalChars += content.length
        totalLines += if (content.isEmpty()) 0 else content.count { it == '\n' } + 1
        totalWords += content.split(Regex("\\s+")).count { it.isNotBlank() }
        totalTokens += TokenEstimator.estimate(content)
    }
}

/**
 * Estimates token count for LLM context.
 * Considers words and punctuation as tokens.
 */
object TokenEstimator {
    fun estimate(content: String): Int {
        val words = content.split(Regex("\\s+")).count { it.isNotBlank() }
        val punctuation = Regex("[;{}()\\[\\],]").findAll(content).count()
        return words + punctuation
    }
}
