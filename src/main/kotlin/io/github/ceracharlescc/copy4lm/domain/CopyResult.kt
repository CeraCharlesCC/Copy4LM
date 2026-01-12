package io.github.ceracharlescc.copy4lm.domain

data class CopyResult(
    val clipboardText: String,
    val copiedFileCount: Int,
    val stats: CopyStats,
    val fileLimitReached: Boolean
)
