package io.github.ceracharlescc.copy4lm.application.interactor

import io.github.ceracharlescc.copy4lm.application.port.FileRef

internal data class PlannedFile(
    val fileRef: FileRef,
    val relativePath: String
)

internal data class CollectedFiles(
    val files: List<PlannedFile>,
    val fileLimitReached: Boolean
) {
    val relativePaths: List<String>
        get() = files.map { it.relativePath }
}
