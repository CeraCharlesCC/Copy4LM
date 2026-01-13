package io.github.ceracharlescc.copy4lm.domain.clipboard

internal class ClipboardTextBuilder(
    private val preText: String,
    private val postText: String,
    private val addExtraLineBetweenFiles: Boolean
) {
    private val parts = mutableListOf<String>()
    private var fileCount = 0

    fun addFile(header: String, content: String, footer: String = "") {
        if (fileCount == 0 && preText.isNotEmpty()) {
            parts.add(preText)
        }
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
        if (fileCount == 0) return ""

        if (postText.isNotEmpty()) {
            parts.add(postText)
        }
        return parts.joinToString(separator = "\n")
    }
}