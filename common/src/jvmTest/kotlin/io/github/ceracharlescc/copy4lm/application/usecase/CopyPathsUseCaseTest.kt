package io.github.ceracharlescc.copy4lm.application.usecase

import io.github.ceracharlescc.copy4lm.domain.vo.PathListOptions
import io.github.ceracharlescc.copy4lm.testsupport.FakeFileRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CopyPathsUseCaseTest {
    private val useCase = CopyPathsUseCase { it.path.removePrefix("/workspace/") }

    @Test
    fun `copies only selected directories and files in selection order`() {
        val files = listOf(
            FakeFileRef("Copy4LM", "/workspace/Copy4LM", isDirectory = true),
            FakeFileRef("ContextTools-extension", "/workspace/ContextTools-extension", isDirectory = true),
            FakeFileRef("repozstd", "/workspace/repozstd", isDirectory = true),
            FakeFileRef("image.png", "/workspace/Copy4LM/image.png")
        )

        assertEquals(
            "[Copy4LM/, ContextTools-extension/, repozstd/, Copy4LM/image.png]",
            useCase.execute(files)
        )
    }

    @Test
    fun `removes duplicate selections by full path without conflating relative paths`() {
        val a = FakeFileRef("main.kt", "/one/main.kt")
        val b = FakeFileRef("main.kt", "/two/main.kt")
        val useCase = CopyPathsUseCase { it.name }

        assertEquals("[main.kt, main.kt]", useCase.execute(listOf(a, b, a)))
    }

    @Test
    fun `absolute mode never calls relative resolver and normalizes separators`() {
        val useCase = CopyPathsUseCase { error("Relative resolver must not run") }
        val files = listOf(
            FakeFileRef("src", "C:\\workspace\\src", isDirectory = true),
            FakeFileRef("main.kt", "C:\\workspace\\src\\main.kt"),
            FakeFileRef("root", "/", isDirectory = true)
        )

        assertEquals("[C:/workspace/src/, C:/workspace/src/main.kt, /]",
            useCase.execute(files, absolutePaths = true))
    }

    @Test
    fun `preserves literal custom text and newlines without adding whitespace`() {
        val files = listOf(FakeFileRef("a", "/workspace/a"), FakeFileRef("b", "/workspace/b"))
        val options = PathListOptions(start = "paths:\n$", end = "\nEND", delimiter = "\n---\n")

        assertEquals("paths:\n\$a\n---\nb\nEND",
            useCase.execute(files, options))
        assertEquals("ab", useCase.execute(files, PathListOptions("", "", "")))
    }

    @Test
    fun `supports workspace root and folders with existing trailing slash`() {
        val files = listOf(
            FakeFileRef("workspace", "/workspace", isDirectory = true),
            FakeFileRef("src", "/workspace/src/", isDirectory = true)
        )
        val useCase = CopyPathsUseCase { if (it.path == "/workspace") "." else "src/" }

        assertEquals("[./, src/]", useCase.execute(files))
    }

    @Test
    fun `empty selection contains only configured start and end`() {
        assertEquals("[]", useCase.execute(emptyList()))
        assertEquals("", useCase.execute(emptyList(), PathListOptions("", "", "\n")))
    }
}
