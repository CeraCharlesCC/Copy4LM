package io.github.ceracharlescc.copy4lm.application.usecase
import io.github.ceracharlescc.copy4lm.domain.CopyOptions
import io.github.ceracharlescc.copy4lm.testsupport.CapturingLogger
import io.github.ceracharlescc.copy4lm.testsupport.FakeFileGateway
import io.github.ceracharlescc.copy4lm.testsupport.FakeFileRef
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class CopyFilesUseCaseTest {

    @Test
    fun `copies files recursively from directories, formats headers and footers, builds stats`() {
        val dir = FakeFileRef("src", "/repo/src", isDirectory = true)
        val a = FakeFileRef("A.kt", "/repo/src/A.kt")
        val b = FakeFileRef("B.kt", "/repo/src/B.kt")

        val gateway = FakeFileGateway(
            children = mapOf("/repo/src" to listOf(a, b)),
            contents = mapOf(
                "/repo/src/A.kt" to "one",
                "/repo/src/B.kt" to "two\nthree"
            )
        )
        val logger = CapturingLogger()
        val useCase = CopyFilesUseCase(gateway, logger)

        val options = CopyOptions(
            headerFormat = $$"H:$FILE_PATH",
            footerFormat = $$"F:$FILE_PATH",
            preText = "PRE",
            postText = "POST",
            addExtraLineBetweenFiles = true,
            setMaxFileCount = true,
            fileCountLimit = 30,
            useFilenameFilters = false,
            strictMemoryRead = true,
            maxFileSizeKB = 500
        )

        val result = useCase.execute(listOf(dir), options)

        assertEquals(2, result.copiedFileCount)
        assertFalse(result.fileLimitReached)

        val expectedClipboard = listOf(
            "PRE",
            "H:src/A.kt",
            "one",
            "F:src/A.kt",
            "",
            "H:src/B.kt",
            "two\nthree",
            "F:src/B.kt",
            "",
            "POST"
        ).joinToString("\n")

        assertEquals(expectedClipboard, result.clipboardText)

        // Stats should reflect only content (not headers/footers)
        assertEquals(12, result.stats.totalChars)
        assertEquals(3, result.stats.totalLines)
        assertEquals(3, result.stats.totalWords)
        assertEquals(3, result.stats.totalTokens)

        // Ensure strictMemoryRead flag was passed through to gateway
        assertTrue(gateway.readCalls.all { it.second })
    }

    @Test
    fun `skips binary files`() {
        val a = FakeFileRef("A.kt", "/repo/A.kt")
        val b = FakeFileRef("B.kt", "/repo/B.kt")

        val gateway = FakeFileGateway(
            contents = mapOf(
                "/repo/A.kt" to "keep",
                "/repo/B.kt" to "skip"
            ),
            binaryPaths = setOf("/repo/B.kt")
        )
        val logger = CapturingLogger()
        val useCase = CopyFilesUseCase(gateway, logger)

        val result = useCase.execute(
            files = listOf(a, b),
            options = CopyOptions(
                headerFormat = $$"H:$FILE_PATH",
                footerFormat = $$"F:$FILE_PATH",
                useFilenameFilters = false,
                maxFileSizeKB = 500
            )
        )

        assertEquals(1, result.copiedFileCount)
        assertTrue(result.clipboardText.contains("keep"))
        assertFalse(result.clipboardText.contains("skip"))
        assertTrue(logger.infos.any { it.contains("Binary file") })
    }

    @Test
    fun `skips files exceeding max size`() {
        val big = FakeFileRef("Big.kt", "/repo/Big.kt")

        val gateway = FakeFileGateway(
            contents = mapOf("/repo/Big.kt" to "content"),
            sizesBytes = mapOf("/repo/Big.kt" to 10_000L) // 10KB
        )
        val logger = CapturingLogger()
        val useCase = CopyFilesUseCase(gateway, logger)

        val result = useCase.execute(
            files = listOf(big),
            options = CopyOptions(
                maxFileSizeKB = 1, // 1KB
                useFilenameFilters = false
            )
        )

        assertEquals(0, result.copiedFileCount)
        assertTrue(result.clipboardText.isEmpty())
        assertTrue(logger.infos.any { it.contains("Size limit exceeded") })
    }

    @Test
    fun `applies filename extension filters`() {
        val kt = FakeFileRef("A.kt", "/repo/A.kt")
        val txt = FakeFileRef("note.txt", "/repo/note.txt")

        val gateway = FakeFileGateway(
            contents = mapOf(
                "/repo/A.kt" to "kotlin",
                "/repo/note.txt" to "text"
            )
        )
        val logger = CapturingLogger()
        val useCase = CopyFilesUseCase(gateway, logger)

        val result = useCase.execute(
            files = listOf(kt, txt),
            options = CopyOptions(
                headerFormat = $$"H:$FILE_PATH",
                footerFormat = $$"F:$FILE_PATH",
                useFilenameFilters = true,
                filenameFilters = listOf(".kt")
            )
        )

        assertEquals(1, result.copiedFileCount)
        assertTrue(result.clipboardText.contains("kotlin"))
        assertFalse(result.clipboardText.contains("text"))
    }

    @Test
    fun `deduplicates by relative path`() {
        val a1 = FakeFileRef("A.kt", "/repo/src/A.kt")
        val a2 = FakeFileRef("A.kt", "/repo/src/A.kt") // same path

        val gateway = FakeFileGateway(
            contents = mapOf("/repo/src/A.kt" to "same")
        )
        val logger = CapturingLogger()
        val useCase = CopyFilesUseCase(gateway, logger)

        val result = useCase.execute(
            files = listOf(a1, a2),
            options = CopyOptions(
                headerFormat = $$"H:$FILE_PATH",
                footerFormat = $$"F:$FILE_PATH"
            )
        )

        assertEquals(1, result.copiedFileCount)
        assertTrue(logger.infos.any { it.contains("already copied") })
    }

    @Test
    fun `enforces file count limit and sets fileLimitReached`() {
        val a = FakeFileRef("A.kt", "/repo/A.kt")
        val b = FakeFileRef("B.kt", "/repo/B.kt")

        val gateway = FakeFileGateway(
            contents = mapOf(
                "/repo/A.kt" to "A",
                "/repo/B.kt" to "B"
            )
        )
        val logger = CapturingLogger()
        val useCase = CopyFilesUseCase(gateway, logger)

        val result = useCase.execute(
            files = listOf(a, b),
            options = CopyOptions(
                headerFormat = $$"H:$FILE_PATH",
                footerFormat = $$"F:$FILE_PATH",
                setMaxFileCount = true,
                fileCountLimit = 1
            )
        )

        assertEquals(1, result.copiedFileCount)
        assertTrue(result.fileLimitReached)
        assertTrue(result.clipboardText.contains("A"))
        assertFalse(result.clipboardText.contains("B"))
    }

    @Test
    fun `passes strictMemoryRead flag to gateway`() {
        val a = FakeFileRef("A.kt", "/repo/A.kt")

        val gateway = FakeFileGateway(
            contents = mapOf("/repo/A.kt" to "A")
        )
        val logger = CapturingLogger()
        val useCase = CopyFilesUseCase(gateway, logger)

        useCase.execute(
            files = listOf(a),
            options = CopyOptions(strictMemoryRead = false)
        )

        assertEquals(listOf("/repo/A.kt" to false), gateway.readCalls)
    }
}
