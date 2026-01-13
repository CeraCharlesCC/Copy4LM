package io.github.ceracharlescc.copy4lm.domain.formatting

import org.junit.jupiter.api.Test

internal class FormattingTest {

    @Test
    fun `formatter replaces placeholders correctly`() {
        val projectName = "MyProject"
        val filePath = "src/Main.kt"

        val placeholderTemplate = $$"Project: $PROJECT_NAME, File: $FILE_PATH"
        val placeholderResult = PlaceholderFormatter.format(placeholderTemplate, projectName, filePath)
        assert(placeholderResult == "Project: MyProject, File: src/Main.kt")
    }
}