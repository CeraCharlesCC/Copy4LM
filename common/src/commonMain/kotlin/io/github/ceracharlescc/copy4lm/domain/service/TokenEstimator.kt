package io.github.ceracharlescc.copy4lm.domain.service

/**
 * Estimates token count for LLM context.
 * Considers words and punctuation as tokens.
 */
internal object TokenEstimator {
    fun estimate(content: String): Int {
        val words = content.split(Regex("\\s+")).count { it.isNotBlank() }
        val punctuation = Regex("[;{}()\\[\\],]").findAll(content).count()
        return words + punctuation
    }
}
