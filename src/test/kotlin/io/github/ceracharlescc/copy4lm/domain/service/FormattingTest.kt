package io.github.ceracharlescc.copy4lm.domain.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FormattingTest {

    @Test
    fun `formatter replaces placeholders correctly`() {
        val projectName = "MyProject"
        val filePath = "src/Main.kt"

        val placeholderTemplate = $$"Project: $PROJECT_NAME, File: $FILE_PATH"
        val placeholderResult = PlaceholderFormatter.format(placeholderTemplate, projectName, filePath)
        assertEquals("Project: MyProject, File: src/Main.kt", placeholderResult)
    }

    @Test
    fun `formatter returns template unchanged when it has no placeholders`() {
        val template = "No placeholders here."
        val result = PlaceholderFormatter.format(
            template = template,
            projectName = "MyProject",
            relativePath = "src/Main.kt",
            directoryStructure = "TREE"
        )
        assertEquals(template, result)
    }

    @Test
    fun `formatter replaces project name with empty string when projectName is empty`() {
        val template = $$"Project: $PROJECT_NAME"
        val result = PlaceholderFormatter.format(
            template = template,
            projectName = "",
            relativePath = "src/Main.kt"
        )
        assertEquals("Project: ", result)
    }

    @Test
    fun `formatter leaves file path placeholder intact when relativePath is null`() {
        val template = $$"File: $FILE_PATH"
        val result = PlaceholderFormatter.format(
            template = template,
            projectName = "MyProject",
            relativePath = null
        )
        // FILE_PATH is only replaced when a non-null relativePath is provided.
        assertEquals($$"File: $FILE_PATH", result)
    }

    @Test
    fun `formatter replaces file path placeholder when relativePath is empty string`() {
        val template = $$"File: $FILE_PATH"
        val result = PlaceholderFormatter.format(
            template = template,
            projectName = "MyProject",
            relativePath = ""
        )
        assertEquals("File: ", result)
    }

    @Test
    fun `formatter replaces multiple occurrences of the same placeholder`() {
        val template =
            $$"P: $PROJECT_NAME $PROJECT_NAME | F: $FILE_PATH $FILE_PATH | D: $DIRECTORY_STRUCTURE $DIRECTORY_STRUCTURE"

        val result = PlaceholderFormatter.format(
            template = template,
            projectName = "MyProject",
            relativePath = "src/Main.kt",
            directoryStructure = "TREE"
        )

        assertEquals("P: MyProject MyProject | F: src/Main.kt src/Main.kt | D: TREE TREE", result)
    }

    @Test
    fun `formatter removes directory structure placeholder when directoryStructure is null`() {
        val template = "Before\n" + $$"$DIRECTORY_STRUCTURE" + "\nAfter"
        val result = PlaceholderFormatter.format(
            template = template,
            projectName = "MyProject",
            relativePath = "src/Main.kt",
            directoryStructure = null
        )
        assertEquals("Before\n\nAfter", result)
    }

    @Test
    fun `formatter inserts directory structure when directoryStructure is provided`() {
        val template = "Before\n" + $$"$DIRECTORY_STRUCTURE" + "\nAfter"
        val result = PlaceholderFormatter.format(
            template = template,
            projectName = "MyProject",
            relativePath = "src/Main.kt",
            directoryStructure = "Directory structure:\n└── MyProject/"
        )
        assertEquals("Before\nDirectory structure:\n└── MyProject/\nAfter", result)
    }

    @Test
    fun `formatter can partially format when some placeholders are omitted`() {
        val template = $$"Project: $PROJECT_NAME; File: $FILE_PATH\n$DIRECTORY_STRUCTURE"
        val result = PlaceholderFormatter.format(
            template = template,
            projectName = "MyProject",
            relativePath = null,          // FILE_PATH should remain
            directoryStructure = null     // DIRECTORY_STRUCTURE should be removed
        )
        assertEquals($$"Project: MyProject; File: $FILE_PATH\n", result)
    }
}
