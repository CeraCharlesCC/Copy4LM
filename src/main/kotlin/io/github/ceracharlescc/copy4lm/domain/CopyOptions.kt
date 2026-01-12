package io.github.ceracharlescc.copy4lm.domain

/**
 * Domain options for the copy operation.
 */
data class CopyOptions(
    val headerFormat: String = "// file: \$FILE_PATH",
    val footerFormat: String = "",
    val preText: String = "",
    val postText: String = "",
    val fileCountLimit: Int = 30,
    val setMaxFileCount: Boolean = true,
    val filenameFilters: List<String> = emptyList(),
    val useFilenameFilters: Boolean = false,
    val addExtraLineBetweenFiles: Boolean = true,
    val strictMemoryRead: Boolean = true,
    val maxFileSizeKB: Int = 500
)
