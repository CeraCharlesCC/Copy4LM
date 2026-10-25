@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package io.github.ceracharlescc.copy4lm.domain.vo

import kotlin.js.JsExport

@JsExport
data class CopyResult(
    val clipboardText: String,
    val copiedFileCount: Int,
    val failedFileCount: Int,
    val stats: CopyStats,
    val fileLimitReached: Boolean
)
