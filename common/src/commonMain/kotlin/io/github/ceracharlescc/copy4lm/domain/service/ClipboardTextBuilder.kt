package io.github.ceracharlescc.copy4lm.domain.service

internal class ClipboardTextBuilder(
    private val preText: String,
    private val postText: String,
    private val addExtraLineBetweenFiles: Boolean
) {
    private val text = StringBuilder()
    private var partCount = 0
    private var fileCount = 0

    private fun appendPart(part: String) {
        if (partCount++ > 0) text.append('\n')
        text.append(part)
    }

    fun addFile(header: String, content: String, footer: String = "") {
        if (fileCount == 0 && preText.isNotEmpty()) appendPart(preText)
        appendPart(header)
        appendPart(content)
        if (footer.isNotEmpty()) appendPart(footer)
        fileCount++
        if (addExtraLineBetweenFiles && content.isNotEmpty()) appendPart("")
    }

    fun build(): String = when {
        fileCount == 0 -> ""
        postText.isEmpty() -> text.toString()
        else -> "$text\n$postText"
    }
}
