package io.github.ceracharlescc.copy4lm.domain.formatting

internal object FooterFormatter {
    fun format(footerFormat: String, relativePath: String): String {
        return footerFormat.replace($$"$FILE_PATH", relativePath)
    }
}