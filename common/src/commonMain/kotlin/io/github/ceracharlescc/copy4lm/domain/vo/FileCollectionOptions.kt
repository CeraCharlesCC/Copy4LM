package io.github.ceracharlescc.copy4lm.domain.vo

/**
 * Options for file collection (shared constraints between file content copy and directory structure copy).
 */
data class FileCollectionOptions(
    val fileCountLimit: Int = CopyDefaults.FILE_COUNT_LIMIT,
    val setMaxFileCount: Boolean = CopyDefaults.SET_MAX_FILE_COUNT,
    val filenameFilters: List<String> = emptyList(),
    val useFilenameFilters: Boolean = CopyDefaults.USE_FILENAME_FILTERS,
    val maxFileSizeKB: Int = CopyDefaults.MAX_FILE_SIZE_KB,
    val respectGitIgnore: Boolean = CopyDefaults.RESPECT_GIT_IGNORE
)
