@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package io.github.ceracharlescc.copy4lm.domain.vo

import kotlin.js.JsExport

@JsExport
data class DirectoryStructureResult(
    val clipboardText: String,
    val collectedFileCount: Int,
    val fileLimitReached: Boolean
)
