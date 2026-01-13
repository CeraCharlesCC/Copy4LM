package io.github.ceracharlescc.copy4lm.domain.clipboard

internal class ClipboardTextBuilder(
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
