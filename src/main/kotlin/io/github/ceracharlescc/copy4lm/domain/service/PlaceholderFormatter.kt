package io.github.ceracharlescc.copy4lm.domain.service

import io.github.ceracharlescc.copy4lm.domain.constant.Placeholders

internal object PlaceholderFormatter {
    fun format(
        template: String,
        projectName: String,
        relativePath: String? = null,
        directoryStructure: String? = null
    ): String {
        var out = template.replace(Placeholders.PROJECT_NAME, projectName)
        if (relativePath != null) {
            out = out.replace(Placeholders.FILE_PATH, relativePath)
        }
        out = if (directoryStructure != null) {
            out.replace(Placeholders.DIRECTORY_STRUCTURE, directoryStructure)
        } else {
            out.replace(Placeholders.DIRECTORY_STRUCTURE, "")
        }
        return out
    }
}
