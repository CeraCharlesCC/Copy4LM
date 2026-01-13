package io.github.ceracharlescc.copy4lm.domain

/**
 * Options for file collection (shared constraints between file content copy and directory structure copy).
 */
internal data class FileCollectionOptions(
    val fileCountLimit: Int = 30,
    val setMaxFileCount: Boolean = true,
    val filenameFilters: List<String> = emptyList(),
    val useFilenameFilters: Boolean = false,
    val maxFileSizeKB: Int = 500
)
