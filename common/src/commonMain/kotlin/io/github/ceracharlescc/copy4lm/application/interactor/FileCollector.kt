package io.github.ceracharlescc.copy4lm.application.interactor

import io.github.ceracharlescc.copy4lm.application.port.FileGateway
import io.github.ceracharlescc.copy4lm.application.port.FileRef
import io.github.ceracharlescc.copy4lm.application.port.LoggerPort
import io.github.ceracharlescc.copy4lm.domain.vo.CollectedFiles
import io.github.ceracharlescc.copy4lm.domain.vo.FileCollectionOptions
import io.github.ceracharlescc.copy4lm.domain.vo.PlannedFile

class FileCollector(
    private val fileGateway: FileGateway,
    private val logger: LoggerPort,
    private val options: FileCollectionOptions
) {
    fun collect(files: List<FileRef>): CollectedFiles {
        // Keep traversal state local so a collector can safely be reused.
        val seenPaths = mutableSetOf<String>()
        val plannedFiles = mutableListOf<PlannedFile>()
        val pending = ArrayDeque<Iterator<FileRef>>()
        pending.addLast(files.iterator())
        val maxBytes = options.maxFileSizeKB.toLong() * 1024L
        var fileLimitReached = false

        // Iterators preserve depth-first selection order without recursive stack growth.
        while (pending.isNotEmpty()) {
            val iterator = pending.last()
            if (!iterator.hasNext()) {
                pending.removeLast()
                continue
            }
            val file = iterator.next()
            if (!seenPaths.add(file.path)) {
                logger.info("Skipping already copied file: ${file.path}")
                continue
            }
            if (options.setMaxFileCount && plannedFiles.size >= options.fileCountLimit) {
                fileLimitReached = true
                break
            }

            if (file.isDirectory) {
                if (options.respectGitIgnore && fileGateway.isGitIgnored(file)) {
                    logger.info("Skipping directory: ${file.name} - Ignored by VCS ignore rules")
                } else {
                    pending.addLast(fileGateway.childrenOf(file).iterator())
                }
                continue
            }

            if (options.useFilenameFilters && options.filenameFilters.none { file.name.endsWith(it) }) {
                logger.info("Skipping file: ${file.name} - Extension does not match any filter")
                continue
            }
            // Reject cheap metadata/ignore checks before opening a file to probe its contents.
            if (fileGateway.sizeBytes(file) > maxBytes) {
                logger.info("Skipping file: ${file.name} - Size limit exceeded")
                continue
            }
            if (options.respectGitIgnore && fileGateway.isGitIgnored(file)) {
                logger.info("Skipping file: ${file.name} - Ignored by VCS ignore rules")
                continue
            }
            if (fileGateway.isBinary(file)) {
                logger.info("Skipping file: ${file.name} - Binary file")
                continue
            }

            plannedFiles.add(PlannedFile(file, fileGateway.relativePath(file)))
        }

        return CollectedFiles(files = plannedFiles, fileLimitReached = fileLimitReached)
    }
}
