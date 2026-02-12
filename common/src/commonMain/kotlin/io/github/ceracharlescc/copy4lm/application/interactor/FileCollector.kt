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
    private val seenRelativePaths = mutableSetOf<String>()
    private var fileLimitReached = false

    fun collect(files: List<FileRef>): CollectedFiles {
        val plannedFiles = collectPlannedFiles(files)
        return CollectedFiles(
            files = plannedFiles,
            fileLimitReached = fileLimitReached
        )
    }

    private fun collectPlannedFiles(files: List<FileRef>): List<PlannedFile> {
        val plannedFiles = mutableListOf<PlannedFile>()

        for (file in files) {
            collectFromFile(file, plannedFiles)
            if (fileLimitReached) break
        }

        return plannedFiles
    }

    private fun collectFromFile(file: FileRef, plannedFiles: MutableList<PlannedFile>) {
        if (shouldStop(plannedFiles.size)) return

        if (file.isDirectory) {
            if (options.respectGitIgnore && fileGateway.isGitIgnored(file)) {
                logger.info("Skipping directory: ${file.name} - Ignored by VCS ignore rules")
                return
            }
            for (child in fileGateway.childrenOf(file)) {
                collectFromFile(child, plannedFiles)
                if (fileLimitReached) return
            }
            return
        }

        collectSingleFile(file, plannedFiles)
    }

    private fun collectSingleFile(file: FileRef, plannedFiles: MutableList<PlannedFile>) {
        if (shouldStop(plannedFiles.size)) return

        if (!passesFilenameFilter(file)) {
            logger.info("Skipping file: ${file.name} - Extension does not match any filter")
            return
        }

        if (fileGateway.isBinary(file)) {
            logger.info("Skipping file: ${file.name} - Binary file")
            return
        }

        if (isTooLarge(file)) {
            logger.info("Skipping file: ${file.name} - Size limit exceeded")
            return
        }

        if (options.respectGitIgnore && fileGateway.isGitIgnored(file)) {
            logger.info("Skipping file: ${file.name} - Ignored by VCS ignore rules")
            return
        }

        val relativePath = fileGateway.relativePath(file)

        if (!seenRelativePaths.add(relativePath)) {
            logger.info("Skipping already copied file: $relativePath")
            return
        }

        plannedFiles.add(PlannedFile(file, relativePath))
    }

    private fun shouldStop(currentCount: Int): Boolean {
        if (!options.setMaxFileCount) return false
        if (currentCount < options.fileCountLimit) return false
        fileLimitReached = true
        return true
    }

    private fun passesFilenameFilter(file: FileRef): Boolean {
        if (!options.useFilenameFilters) return true
        return options.filenameFilters.any { filter -> file.name.endsWith(filter) }
    }

    private fun isTooLarge(file: FileRef): Boolean {
        val maxBytes = options.maxFileSizeKB.toLong() * 1024L
        return fileGateway.sizeBytes(file) > maxBytes
    }

}
