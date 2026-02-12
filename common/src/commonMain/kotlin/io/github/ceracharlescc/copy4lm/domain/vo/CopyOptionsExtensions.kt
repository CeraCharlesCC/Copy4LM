package io.github.ceracharlescc.copy4lm.domain.vo

fun CopyOptions.toFileCollectionOptions(): FileCollectionOptions =
    FileCollectionOptions(
        fileCountLimit = fileCountLimit,
        setMaxFileCount = setMaxFileCount,
        filenameFilters = filenameFilters,
        useFilenameFilters = useFilenameFilters,
        maxFileSizeKB = maxFileSizeKB,
        respectGitIgnore = respectGitIgnore
    )
