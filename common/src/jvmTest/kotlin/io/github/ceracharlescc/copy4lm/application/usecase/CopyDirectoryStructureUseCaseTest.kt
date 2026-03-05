package io.github.ceracharlescc.copy4lm.application.usecase

import io.github.ceracharlescc.copy4lm.domain.constant.Placeholders
import io.github.ceracharlescc.copy4lm.domain.vo.DirectoryStructureOptions
import io.github.ceracharlescc.copy4lm.testsupport.CapturingLogger
import io.github.ceracharlescc.copy4lm.testsupport.FakeFileGateway
import io.github.ceracharlescc.copy4lm.testsupport.FakeFileRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

internal class CopyDirectoryStructureUseCaseTest {

    @Test
    fun `formats directory structure text in common module`() {
        val dir = FakeFileRef("src", "/repo/src", isDirectory = true)
        val file = FakeFileRef("A.kt", "/repo/src/A.kt")
        val gateway = FakeFileGateway(
            children = mapOf(dir.path to listOf(file)),
            contents = mapOf(file.path to "aaa")
        )
        val useCase = CopyDirectoryStructureUseCase(gateway, CapturingLogger())

        val result = useCase.execute(
            files = listOf(dir),
            options = DirectoryStructureOptions(
                preText = "PRE",
                postText = "POST\n${Placeholders.PROJECT_NAME}",
                projectName = "MyProject",
                setMaxFileCount = false
            )
        )

        assertEquals(1, result.collectedFileCount)
        assertFalse(result.fileLimitReached)
        assertEquals(
            listOf(
                "PRE",
                "Directory structure:",
                "└── MyProject/",
                "    └── src/",
                "        └── A.kt",
                "POST",
                "MyProject"
            ).joinToString("\n"),
            result.clipboardText
        )
    }
}
