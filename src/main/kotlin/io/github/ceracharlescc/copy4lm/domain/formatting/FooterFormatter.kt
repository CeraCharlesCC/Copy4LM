package io.github.ceracharlescc.copy4lm.domain.formatting

object FooterFormatter {
    fun format(footerFormat: String, relativePath: String): String {
        return footerFormat.replace($$"$FILE_PATH", relativePath)
    }
}