package com.github.mwguerra.copyfilecontent.domain

data class CopyResult(
    val clipboardText: String,
    val copiedFileCount: Int,
    val stats: CopyStats,
    val fileLimitReached: Boolean
)
