package io.github.ceracharlescc.copy4lm.domain.vo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CopyStatsTest {

    @Test
    fun `plus accumulates chars, lines, words, tokens`() {
        var stats = CopyStats()

        stats = stats.plus("one")
        stats = stats.plus("two\nthree")

        // chars: "one"(3) + "two\nthree"(9) = 12
        assertEquals(12, stats.totalChars)

        // lines: "one"(1) + "two\nthree"(2) = 3
        assertEquals(3, stats.totalLines)

        // words: 1 + 2 = 3
        assertEquals(3, stats.totalWords)

        // tokens: The token heuristic counts words + punctuation; no punctuation here => 1 + 2 = 3
        assertEquals(3, stats.totalTokens)
    }

    @Test
    fun `token estimator counts punctuation as tokens`() {
        // words: 1 (no whitespace)
        // punctuation matched by regex: ( ) { } ;  => 5
        // total tokens = 1 + 5 = 6
        assertEquals(6, CopyStats().plus("a(b){c};").totalTokens)
    }

    @Test
    fun `empty content adds zeros`() {
        val stats = CopyStats().plus("")
        assertEquals(0, stats.totalChars)
        assertEquals(0, stats.totalLines)
        assertEquals(0, stats.totalWords)
        assertEquals(0, stats.totalTokens)
    }

    @Test
    fun `counts whitespace and punctuation in one pass including Unicode separators`() {
        val text = " \talpha\r\nbeta\u00a0gamma\u3000delta;{()}[],\n"
        val stats = CopyStats().plus(text)
        assertEquals(text.length, stats.totalChars)
        assertEquals(3, stats.totalLines)
        assertEquals(4, stats.totalWords)
        assertEquals(12, stats.totalTokens)
        assertEquals(0, CopyStats().plus(" \t\r\n\u00a0\u3000").totalWords)
    }
}
