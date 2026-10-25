package io.github.ceracharlescc.copy4lm.application.interactor

import io.github.ceracharlescc.copy4lm.application.port.FileGateway
import io.github.ceracharlescc.copy4lm.application.port.FileRef
import io.github.ceracharlescc.copy4lm.domain.vo.FileCollectionOptions
import io.github.ceracharlescc.copy4lm.testsupport.CapturingLogger
import io.github.ceracharlescc.copy4lm.testsupport.FakeFileGateway
import io.github.ceracharlescc.copy4lm.testsupport.FakeFileRef
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class FileCollectorTest {

    @Test
    fun `collects files recursively from directories and returns planned files with relative paths`() {
        val dir = FakeFileRef("src", "/repo/src", isDirectory = true)
        val a = FakeFileRef("A.kt", "/repo/src/A.kt")
        val utils = FakeFileRef("utils", "/repo/src/utils", isDirectory = true)
        val b = FakeFileRef("B.kt", "/repo/src/utils/B.kt")

        val gateway = FakeFileGateway(
            children = mapOf(
                "/repo/src" to listOf(a, utils),
                "/repo/src/utils" to listOf(b)
            ),
            contents = mapOf(
                a.path to "a",
                b.path to "b"
            )
        )
        val logger = CapturingLogger()

        val collector = FileCollector(
            fileGateway = gateway,
            logger = logger,
            options = FileCollectionOptions(
                setMaxFileCount = false,
                useFilenameFilters = false,
                maxFileSizeKB = 500
            )
        )

        val collected = collector.collect(listOf(dir))

        assertFalse(collected.fileLimitReached)
        assertEquals(listOf("src/A.kt", "src/utils/B.kt"), collected.relativePaths)
        assertEquals(2, collected.files.size)
        assertEquals("src/A.kt", collected.files[0].relativePath)
        assertEquals("src/utils/B.kt", collected.files[1].relativePath)
    }

    @Test
    fun `skips files that do not pass filename filter`() {
        val txt = FakeFileRef("note.txt", "/repo/note.txt")

        val gateway = FakeFileGateway(
            contents = mapOf(txt.path to "text")
        )
        val logger = CapturingLogger()

        val collector = FileCollector(
            fileGateway = gateway,
            logger = logger,
            options = FileCollectionOptions(
                useFilenameFilters = true,
                filenameFilters = listOf(".kt"),
                setMaxFileCount = false,
                maxFileSizeKB = 500
            )
        )

        val collected = collector.collect(listOf(txt))

        assertEquals(0, collected.files.size)
        assertTrue(logger.infos.any { it.contains("Extension does not match any filter") })
    }

    @Test
    fun `skips binary files`() {
        val bin = FakeFileRef("file.dat", "/repo/file.dat")

        val gateway = FakeFileGateway(
            contents = mapOf(bin.path to "ignored"),
            binaryPaths = setOf(bin.path)
        )
        val logger = CapturingLogger()

        val collector = FileCollector(
            fileGateway = gateway,
            logger = logger,
            options = FileCollectionOptions(
                setMaxFileCount = false,
                useFilenameFilters = false,
                maxFileSizeKB = 500
            )
        )

        val collected = collector.collect(listOf(bin))

        assertEquals(0, collected.files.size)
        assertTrue(logger.infos.any { it.contains("Binary file") })
    }

    @Test
    fun `skips files exceeding max size and includes boundary size equal to limit`() {
        val within = FakeFileRef("Within.kt", "/repo/Within.kt")
        val tooBig = FakeFileRef("TooBig.kt", "/repo/TooBig.kt")

        val gateway = FakeFileGateway(
            contents = mapOf(
                within.path to "x",
                tooBig.path to "y"
            ),
            sizesBytes = mapOf(
                within.path to 1024L,  // exactly 1KB
                tooBig.path to 1025L   // just over 1KB
            )
        )
        val logger = CapturingLogger()

        val collector = FileCollector(
            fileGateway = gateway,
            logger = logger,
            options = FileCollectionOptions(
                setMaxFileCount = false,
                useFilenameFilters = false,
                maxFileSizeKB = 1
            )
        )

        val collected = collector.collect(listOf(within, tooBig))

        assertEquals(listOf("Within.kt"), collected.relativePaths)
        assertTrue(logger.infos.any { it.contains("Size limit exceeded") })
    }

    @Test
    fun `deduplicates by relative path and logs already copied`() {
        val a1 = FakeFileRef("A.kt", "/repo/src/A.kt")
        val a2 = FakeFileRef("A.kt", "/repo/src/A.kt") // same path => same relativePath

        val gateway = FakeFileGateway(
            contents = mapOf(a1.path to "same")
        )
        val logger = CapturingLogger()

        val collector = FileCollector(
            fileGateway = gateway,
            logger = logger,
            options = FileCollectionOptions(
                setMaxFileCount = false,
                useFilenameFilters = false,
                maxFileSizeKB = 500
            )
        )

        val collected = collector.collect(listOf(a1, a2))

        assertEquals(1, collected.files.size)
        assertEquals("src/A.kt", collected.files.single().relativePath)
        assertTrue(logger.infos.any { it.contains("already copied") })
    }

    @Test
    fun `skips gitignored file when respectGitIgnore is enabled`() {
        val ignored = FakeFileRef("secret.txt", "/repo/secret.txt")

        val gateway = FakeFileGateway(
            contents = mapOf(ignored.path to "sensitive"),
            ignoredPaths = setOf(ignored.path)
        )
        val logger = CapturingLogger()

        val collector = FileCollector(
            fileGateway = gateway,
            logger = logger,
            options = FileCollectionOptions(
                setMaxFileCount = false,
                useFilenameFilters = false,
                maxFileSizeKB = 500,
                respectGitIgnore = true
            )
        )

        val collected = collector.collect(listOf(ignored))

        assertTrue(collected.files.isEmpty())
        assertTrue(logger.infos.any { it.contains("Ignored by VCS ignore rules") })
    }

    @Test
    fun `includes gitignored file when respectGitIgnore is disabled`() {
        val ignored = FakeFileRef("secret.txt", "/repo/secret.txt")

        val gateway = FakeFileGateway(
            contents = mapOf(ignored.path to "sensitive"),
            ignoredPaths = setOf(ignored.path)
        )
        val logger = CapturingLogger()

        val collector = FileCollector(
            fileGateway = gateway,
            logger = logger,
            options = FileCollectionOptions(
                setMaxFileCount = false,
                useFilenameFilters = false,
                maxFileSizeKB = 500,
                respectGitIgnore = false
            )
        )

        val collected = collector.collect(listOf(ignored))

        assertEquals(listOf("secret.txt"), collected.relativePaths)
    }

    @Test
    fun `skips gitignored directory subtree when respectGitIgnore is enabled`() {
        val ignoredDir = FakeFileRef("build", "/repo/build", isDirectory = true)
        val ignoredFile = FakeFileRef("generated.kt", "/repo/build/generated.kt")
        val keep = FakeFileRef("main.kt", "/repo/main.kt")

        val gateway = FakeFileGateway(
            children = mapOf(
                "/repo/build" to listOf(ignoredFile)
            ),
            contents = mapOf(
                ignoredFile.path to "ignored",
                keep.path to "keep"
            ),
            ignoredPaths = setOf(ignoredDir.path)
        )
        val logger = CapturingLogger()

        val collector = FileCollector(
            fileGateway = gateway,
            logger = logger,
            options = FileCollectionOptions(
                setMaxFileCount = false,
                useFilenameFilters = false,
                maxFileSizeKB = 500,
                respectGitIgnore = true
            )
        )

        val collected = collector.collect(listOf(ignoredDir, keep))

        assertEquals(listOf("main.kt"), collected.relativePaths)
        assertTrue(logger.infos.any { it.contains("Skipping directory") && it.contains("Ignored by VCS ignore rules") })
    }

    @Test
    fun `enforces file count limit and stops traversal`() {
        val dir = FakeFileRef("src", "/repo/src", isDirectory = true)
        val a = FakeFileRef("A.kt", "/repo/src/A.kt")
        val b = FakeFileRef("B.kt", "/repo/src/B.kt")
        val c = FakeFileRef("C.kt", "/repo/src/C.kt")

        val gateway = FakeFileGateway(
            children = mapOf("/repo/src" to listOf(a, b, c)),
            contents = mapOf(
                a.path to "A",
                b.path to "B",
                c.path to "C"
            )
        )
        val logger = CapturingLogger()

        val collector = FileCollector(
            fileGateway = gateway,
            logger = logger,
            options = FileCollectionOptions(
                setMaxFileCount = true,
                fileCountLimit = 2,
                useFilenameFilters = false,
                maxFileSizeKB = 500
            )
        )

        val collected = collector.collect(listOf(dir))

        assertTrue(collected.fileLimitReached)
        assertEquals(2, collected.files.size)
        assertEquals(listOf("src/A.kt", "src/B.kt"), collected.relativePaths)
    }

    @Test
    fun `collector reuse does not retain paths or the previous limit flag`() {
        val a = FakeFileRef("A.kt", "/repo/A.kt")
        val b = FakeFileRef("B.kt", "/repo/B.kt")
        val collector = FileCollector(FakeFileGateway(), CapturingLogger(),
            FileCollectionOptions(fileCountLimit = 1))

        assertTrue(collector.collect(listOf(a, b)).fileLimitReached)
        val second = collector.collect(listOf(a))
        assertEquals(listOf("A.kt"), second.relativePaths)
        assertFalse(second.fileLimitReached)
    }

    @Test
    fun `overlapping selections only traverse and probe each path once`() {
        val dir = FakeFileRef("src", "/repo/src", isDirectory = true)
        val a = FakeFileRef("A.kt", "/repo/src/A.kt")
        val gateway = FakeFileGateway(children = mapOf(dir.path to listOf(a)))
        val collector = FileCollector(gateway, CapturingLogger(), FileCollectionOptions(setMaxFileCount = false))

        val result = collector.collect(listOf(dir, dir, a))

        assertEquals(listOf("src/A.kt"), result.relativePaths)
        assertEquals(listOf(dir.path), gateway.childrenCalls)
        assertEquals(listOf(a.path), gateway.binaryCalls)
    }

    @Test
    fun `distinct physical paths with matching display paths are both collected`() {
        val a = FakeFileRef("A.kt", "/one/A.kt")
        val b = FakeFileRef("A.kt", "/two/A.kt")
        val gateway = object : FileGateway by FakeFileGateway() {
            override fun relativePath(file: FileRef) = file.name
        }
        val result = FileCollector(gateway, CapturingLogger(), FileCollectionOptions(setMaxFileCount = false))
            .collect(listOf(a, b))

        assertEquals(listOf(a, b), result.files.map { it.fileRef })
    }

    @Test
    fun `deep and cyclic directory graphs do not overflow the stack or repeat traversal`() {
        val depth = 10000
        val leaf = FakeFileRef("A.kt", "/repo/A.kt")
        fun directory(index: Int) = FakeFileRef("$index", "/repo/$index", isDirectory = true)
        var visits = 0
        val gateway = object : FileGateway by FakeFileGateway() {
            override fun childrenOf(dir: FileRef): List<FileRef> {
                visits++
                val index = dir.name.toInt()
                return if (index == depth) listOf(leaf) else listOf(dir, directory(index + 1))
            }
        }
        val result = FileCollector(gateway, CapturingLogger(), FileCollectionOptions(setMaxFileCount = false))
            .collect(listOf(directory(0)))

        assertEquals(listOf("A.kt"), result.relativePaths)
        assertEquals(depth + 1, visits)
    }

    @Test
    fun `oversized and ignored files are rejected before binary probing`() {
        val large = FakeFileRef("large.kt", "/repo/large.kt")
        val ignored = FakeFileRef("ignored.kt", "/repo/ignored.kt")
        val gateway = FakeFileGateway(sizesBytes = mapOf(large.path to 1025L),
            ignoredPaths = setOf(ignored.path))
        val result = FileCollector(gateway, CapturingLogger(), FileCollectionOptions(maxFileSizeKB = 1))
            .collect(listOf(large, ignored))

        assertTrue(result.files.isEmpty())
        assertTrue(gateway.binaryCalls.isEmpty())
    }
}
