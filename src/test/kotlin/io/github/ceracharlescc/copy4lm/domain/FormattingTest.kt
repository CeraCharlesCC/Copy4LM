package io.github.ceracharlescc.copy4lm.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FormattingTest {

    @Test
    fun `clipboard builder applies pre post and extra line between files`() {
        val b = ClipboardTextBuilder(
            preText = "PRE",
            postText = "POST",
            addExtraLineBetweenFiles = true
        )

        b.addFile(header = "H:a", content = "one", footer = "F:a")
        b.addFile(header = "H:b", content = "two\nthree", footer = "F:b")

        val expected = listOf(
            "PRE",
            "H:a",
            "one",
            "F:a",
            "",          // extra line
            "H:b",
            "two\nthree",
            "F:b",
            "",          // extra line
            "POST"
        ).joinToString("\n")

        assertEquals(expected, b.build())
    }

    @Test
    fun `clipboard builder does not add extra line when content empty`() {
        val b = ClipboardTextBuilder(
            preText = "",
            postText = "",
            addExtraLineBetweenFiles = true
        )

        b.addFile(header = "H", content = "", footer = "F")

        // extra line should NOT be added because content is empty
        assertEquals(listOf("H", "", "F").joinToString("\n"), b.build())
    }
}
