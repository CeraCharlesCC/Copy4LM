package io.github.ceracharlescc.copy4lm.application.interactor

import io.github.ceracharlescc.copy4lm.domain.constant.Placeholders
import io.github.ceracharlescc.copy4lm.domain.service.DirectoryStructureBuilder
import io.github.ceracharlescc.copy4lm.domain.vo.CopyOptions
import io.github.ceracharlescc.copy4lm.domain.vo.toFileCollectionOptions
import io.github.ceracharlescc.copy4lm.testsupport.CapturingLogger
import io.github.ceracharlescc.copy4lm.testsupport.FakeFileGateway
import io.github.ceracharlescc.copy4lm.testsupport.FakeFileRef
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class CopyFilesInteractorTest {

    @Test
    fun `formats placeholders with directory structure in pre-post-header-footer and builds stats`() {
        val a = FakeFileRef("A.kt", "/repo/src/A.kt")
        val b = FakeFileRef("B.kt", "/repo/src/utils/B.kt")

        val gateway = FakeFileGateway(
            contents = mapOf(
                a.path to "aaa",
                b.path to "bbb"
            )
        )

        val options = CopyOptions(
            projectName = "MyProject",
            preText = "PRE\n${Placeholders.DIRECTORY_STRUCTURE}\nENDPRE",
            postText = "POST\n${Placeholders.DIRECTORY_STRUCTURE}\nENDPOST",
            headerFormat = "H:${Placeholders.FILE_PATH}\n${Placeholders.DIRECTORY_STRUCTURE}",
            footerFormat = "F:${Placeholders.FILE_PATH}",
            addExtraLineBetweenFiles = false,
            strictMemoryRead = true,
            useFilenameFilters = false,
            setMaxFileCount = false,
            maxFileSizeKB = 500
        )

        val collector = FileCollector(
            fileGateway = gateway,
            logger = CapturingLogger(),
            options = options.toFileCollectionOptions()
        )
        val interactor = CopyFilesInteractor(gateway, options, collector)

        val result = interactor.run(listOf(a, b))

        val directoryStructure = DirectoryStructureBuilder.build(
            rootName = options.projectName,
            relativePaths = listOf("src/A.kt", "src/utils/B.kt")
        )

        val expected = listOf(
            "PRE\n$directoryStructure\nENDPRE",
            "H:src/A.kt\n$directoryStructure",
            "aaa",
            "F:src/A.kt",
            "H:src/utils/B.kt\n$directoryStructure",
            "bbb",
            "F:src/utils/B.kt",
            "POST\n$directoryStructure\nENDPOST"
        ).joinToString("\n")

        assertEquals(2, result.copiedFileCount)
        assertFalse(result.fileLimitReached)
        assertEquals(expected, result.clipboardText)

        // Ensure placeholders are actually replaced (not left as literals)
        assertFalse(result.clipboardText.contains(Placeholders.DIRECTORY_STRUCTURE))

        // Stats reflect content only
        assertEquals(6, result.stats.totalChars)
        assertEquals(2, result.stats.totalLines)
        assertEquals(2, result.stats.totalWords)
        assertEquals(2, result.stats.totalTokens)

        // strictMemoryRead passed through
        assertEquals(listOf(a.path to true, b.path to true), gateway.readCalls)
    }

    @Test
    fun `returns empty clipboard text when no files are collected`() {
        val txt = FakeFileRef("note.txt", "/repo/note.txt")

        val gateway = FakeFileGateway(
            contents = mapOf(txt.path to "text")
        )

        val options = CopyOptions(
            projectName = "MyProject",
            preText = "PRE",
            postText = "POST",
            useFilenameFilters = true,
            filenameFilters = listOf(".kt"), // exclude .txt
            setMaxFileCount = false
        )

        val collector = FileCollector(
            fileGateway = gateway,
            logger = CapturingLogger(),
            options = options.toFileCollectionOptions()
        )
        val interactor = CopyFilesInteractor(gateway, options, collector)

        val result = interactor.run(listOf(txt))

        assertEquals(0, result.copiedFileCount)
        assertFalse(result.fileLimitReached)
        assertEquals("", result.clipboardText)
        assertEquals(0, result.stats.totalChars)
        assertEquals(0, result.stats.totalLines)
        assertEquals(0, result.stats.totalWords)
        assertEquals(0, result.stats.totalTokens)

        // No reads should happen because nothing is collected
        assertTrue(gateway.readCalls.isEmpty())
    }

    @Test
    fun `propagates fileLimitReached from collector`() {
        val a = FakeFileRef("A.kt", "/repo/A.kt")
        val b = FakeFileRef("B.kt", "/repo/B.kt")

        val gateway = FakeFileGateway(
            contents = mapOf(
                a.path to "A",
                b.path to "B"
            )
        )

        val options = CopyOptions(
            projectName = "MyProject",
            headerFormat = "H:${Placeholders.FILE_PATH}",
            footerFormat = "F:${Placeholders.FILE_PATH}",
            addExtraLineBetweenFiles = false,
            setMaxFileCount = true,
            fileCountLimit = 1, // should stop after first file
            useFilenameFilters = false
        )

        val collector = FileCollector(
            fileGateway = gateway,
            logger = CapturingLogger(),
            options = options.toFileCollectionOptions()
        )
        val interactor = CopyFilesInteractor(gateway, options, collector)

        val result = interactor.run(listOf(a, b))

        assertEquals(1, result.copiedFileCount)
        assertTrue(result.fileLimitReached)
        assertTrue(result.clipboardText.contains("A"))
        assertFalse(result.clipboardText.contains("B"))
    }
}
