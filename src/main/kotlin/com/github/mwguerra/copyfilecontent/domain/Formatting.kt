package com.github.mwguerra.copyfilecontent.domain

/**
 * Formats file headers for clipboard output.
 * Pure domain helper with no framework dependencies.
 */
object HeaderFormatter {
    /**
     * Replaces $FILE_PATH placeholder with the actual file path.
     */
    fun format(headerFormat: String, relativePath: String): String {
        return headerFormat.replace("\$FILE_PATH", relativePath)
    }
}

/**
 * Formats file footers for clipboard output.
 * Pure domain helper with no framework dependencies.
 */
object FooterFormatter {
    /**
     * Replaces $FILE_PATH placeholder with the actual file path.
     */
    fun format(footerFormat: String, relativePath: String): String {
        return footerFormat.replace("\$FILE_PATH", relativePath)
    }
}

/**
 * Builds the clipboard text from file contents.
 * Pure domain helper with no framework dependencies.
 */
class ClipboardTextBuilder(
    private val preText: String,
    private val postText: String,
    private val addExtraLineBetweenFiles: Boolean
) {
    private val parts = mutableListOf<String>()
    private var fileCount = 0

    init {
        if (preText.isNotEmpty()) {
            parts.add(preText)
        }
    }

    fun addFile(header: String, content: String, footer: String = "") {
        parts.add(header)
        parts.add(content)
        if (footer.isNotEmpty()) {
            parts.add(footer)
        }
        fileCount++

        if (addExtraLineBetweenFiles && content.isNotEmpty()) {
            parts.add("")
        }
    }

    fun build(): String {
        if (postText.isNotEmpty()) {
            parts.add(postText)
        }
        return parts.joinToString(separator = "\n")
    }
}
