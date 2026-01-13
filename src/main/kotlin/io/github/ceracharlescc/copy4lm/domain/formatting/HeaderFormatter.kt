package io.github.ceracharlescc.copy4lm.domain.formatting

internal object HeaderFormatter {
    fun format(headerFormat: String, relativePath: String): String {
        return headerFormat.replace($$"$FILE_PATH", relativePath)
    }
}