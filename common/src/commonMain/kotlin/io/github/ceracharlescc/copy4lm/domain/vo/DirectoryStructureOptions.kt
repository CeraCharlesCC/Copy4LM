package io.github.ceracharlescc.copy4lm.domain.vo

data class DirectoryStructureOptions(
    val preText: String = CopyDefaults.EMPTY_TEXT,
    val postText: String = CopyDefaults.EMPTY_TEXT,
    val fileCountLimit: Int = CopyDefaults.FILE_COUNT_LIMIT,
    val setMaxFileCount: Boolean = CopyDefaults.SET_MAX_FILE_COUNT,
    val filenameFilters: List<String> = emptyList(),
    val useFilenameFilters: Boolean = CopyDefaults.USE_FILENAME_FILTERS,
    val respectGitIgnore: Boolean = CopyDefaults.RESPECT_GIT_IGNORE,
    val maxFileSizeKB: Int = CopyDefaults.MAX_FILE_SIZE_KB,
    val projectName: String = CopyDefaults.PROJECT_NAME
)

fun DirectoryStructureOptions.toFileCollectionOptions(): FileCollectionOptions =
    FileCollectionOptions(
        fileCountLimit = fileCountLimit,
        setMaxFileCount = setMaxFileCount,
        filenameFilters = filenameFilters,
        useFilenameFilters = useFilenameFilters,
        maxFileSizeKB = maxFileSizeKB,
        respectGitIgnore = respectGitIgnore
    )
