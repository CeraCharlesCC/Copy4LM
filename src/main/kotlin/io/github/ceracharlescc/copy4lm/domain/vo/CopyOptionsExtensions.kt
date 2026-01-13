package io.github.ceracharlescc.copy4lm.domain.vo

internal fun CopyOptions.toFileCollectionOptions(): FileCollectionOptions =
    FileCollectionOptions(
        fileCountLimit = fileCountLimit,
        setMaxFileCount = setMaxFileCount,
        filenameFilters = filenameFilters,
        useFilenameFilters = useFilenameFilters,
        maxFileSizeKB = maxFileSizeKB
    )
