package io.github.ceracharlescc.copy4lm.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CopyStatsTest {

    @Test
    fun `add accumulates chars, lines, words, tokens`() {
        val stats = CopyStats()

        stats.add("one")
        stats.add("two\nthree")

        // chars: "one"(3) + "two\nthree"(9) = 12
        assertEquals(12, stats.totalChars)

        // lines: "one"(1) + "two\nthree"(2) = 3
        assertEquals(3, stats.totalLines)

        // words: 1 + 2 = 3
        assertEquals(3, stats.totalWords)

        // tokens: TokenEstimator counts words + punctuation; no punctuation here => 1 + 2 = 3
        assertEquals(3, stats.totalTokens)
    }

    @Test
    fun `token estimator counts punctuation as tokens`() {
        // words: 1 (no whitespace)
        // punctuation matched by regex: ( ) { } ;  => 5
        // total tokens = 1 + 5 = 6
        assertEquals(6, TokenEstimator.estimate("a(b){c};"))
    }

    @Test
    fun `empty content adds zeros`() {
        val stats = CopyStats()
        stats.add("")
        assertEquals(0, stats.totalChars)
        assertEquals(0, stats.totalLines)
        assertEquals(0, stats.totalWords)
        assertEquals(0, stats.totalTokens)
    }
}
