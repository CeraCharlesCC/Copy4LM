package io.github.ceracharlescc.copy4lm.domain.vo

data class DirectoryStructureResult(
    val clipboardText: String,
    val collectedFileCount: Int,
    val fileLimitReached: Boolean
)
