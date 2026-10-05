package io.github.ceracharlescc.copy4lm.application.usecase

import io.github.ceracharlescc.copy4lm.application.port.FileRef
import io.github.ceracharlescc.copy4lm.domain.vo.PathListOptions

/** Copies the selected entries themselves, preserving order without traversing directories. */
class CopyPathsUseCase(private val relativePath: (FileRef) -> String) {
    fun execute(
        files: List<FileRef>,
        options: PathListOptions = PathListOptions(),
        absolutePaths: Boolean = false
    ): String = files.distinctBy { it.path }.joinToString(
        separator = options.delimiter,
        prefix = options.start,
        postfix = options.end
    ) { file ->
        val path = (if (absolutePaths) file.path else relativePath(file)).replace('\\', '/')
        if (file.isDirectory && !path.endsWith('/')) "$path/" else path
    }
}
