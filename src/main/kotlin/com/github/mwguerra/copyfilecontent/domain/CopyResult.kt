package com.github.mwguerra.copyfilecontent.domain

/**
 * Result of a copy operation.
 * Pure domain object with no framework dependencies.
 */
data class CopyResult(
    val clipboardText: String,
    val copiedFileCount: Int,
    val stats: CopyStats,
    val fileLimitReached: Boolean
)
