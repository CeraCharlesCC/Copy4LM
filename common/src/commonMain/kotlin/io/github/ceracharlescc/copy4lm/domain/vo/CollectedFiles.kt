package io.github.ceracharlescc.copy4lm.domain.vo

import io.github.ceracharlescc.copy4lm.application.port.FileRef

data class PlannedFile(
    val fileRef: FileRef,
    val relativePath: String
)

data class CollectedFiles(
    val files: List<PlannedFile>,
    val fileLimitReached: Boolean
) {
    val relativePaths: List<String>
        get() = files.map { it.relativePath }
}
