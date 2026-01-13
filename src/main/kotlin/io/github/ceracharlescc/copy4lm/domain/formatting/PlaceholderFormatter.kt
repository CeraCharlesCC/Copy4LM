package io.github.ceracharlescc.copy4lm.domain.formatting

object PlaceholderFormatter {
    fun format(template: String, projectName: String, relativePath: String? = null): String {
        var out = template.replace($$"$PROJECT_NAME", projectName)
        if (relativePath != null) {
            out = out.replace($$"$FILE_PATH", relativePath)
        }
        return out
    }
}