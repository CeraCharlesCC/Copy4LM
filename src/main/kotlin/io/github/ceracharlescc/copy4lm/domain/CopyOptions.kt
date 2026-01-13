package io.github.ceracharlescc.copy4lm.domain

/**
 * Domain options for the copy operation.
 */
internal data class CopyOptions(
    val headerFormat: String = $$"```$FILE_PATH",
    val footerFormat: String = "```",
    val preText: String = $$"=====\n$PROJECT_NAME\n=====\n",
    val postText: String = "",
    val fileCountLimit: Int = 30,
    val setMaxFileCount: Boolean = true,
    val filenameFilters: List<String> = emptyList(),
    val useFilenameFilters: Boolean = false,
    val addExtraLineBetweenFiles: Boolean = true,
    val strictMemoryRead: Boolean = true,
    val maxFileSizeKB: Int = 500,
    val projectName: String = ""
)
