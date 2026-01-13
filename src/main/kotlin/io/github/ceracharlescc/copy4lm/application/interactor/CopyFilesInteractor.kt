package io.github.ceracharlescc.copy4lm.application.interactor

import io.github.ceracharlescc.copy4lm.application.port.FileGateway
import io.github.ceracharlescc.copy4lm.application.port.FileRef
import io.github.ceracharlescc.copy4lm.application.port.LoggerPort
import io.github.ceracharlescc.copy4lm.domain.clipboard.ClipboardTextBuilder
import io.github.ceracharlescc.copy4lm.domain.CopyOptions
import io.github.ceracharlescc.copy4lm.domain.CopyResult
import io.github.ceracharlescc.copy4lm.domain.CopyStats
import io.github.ceracharlescc.copy4lm.domain.directory.DirectoryStructureBuilder
import io.github.ceracharlescc.copy4lm.domain.formatting.PlaceholderFormatter

internal class CopyFilesInteractor(
    private val fileGateway: FileGateway,
    private val logger: LoggerPort,
    private val options: CopyOptions
) {
    private val seenRelativePaths = mutableSetOf<String>()
    private var fileLimitReached = false

    fun run(files: List<FileRef>): CopyResult {
        // Phase 1: Collect planned files
        val plannedFiles = collectPlannedFiles(files)

        // Build directory structure from collected files
        val directoryStructure = DirectoryStructureBuilder.build(
            rootName = options.projectName,
            relativePaths = plannedFiles.map { it.relativePath }
        )

        // Phase 2: Render clipboard text with directory structure context
        val textBuilder = ClipboardTextBuilder(
            preText = PlaceholderFormatter.format(options.preText, options.projectName, directoryStructure = directoryStructure),
            postText = PlaceholderFormatter.format(options.postText, options.projectName, directoryStructure = directoryStructure),
            addExtraLineBetweenFiles = options.addExtraLineBetweenFiles
        )

        val stats = CopyStats()

        for (planned in plannedFiles) {
            val content = fileGateway.readText(planned.fileRef, options.strictMemoryRead)
            val header = PlaceholderFormatter.format(
                options.headerFormat,
                options.projectName,
                planned.relativePath,
                directoryStructure
            )
            val footer = PlaceholderFormatter.format(
                options.footerFormat,
                options.projectName,
                planned.relativePath,
                directoryStructure
            )

            textBuilder.addFile(header, content, footer)
            stats.add(content)
        }

        return CopyResult(
            clipboardText = textBuilder.build(),
            copiedFileCount = plannedFiles.size,
            stats = stats,
            fileLimitReached = fileLimitReached
        )
    }

    /**
     * Collects files that would be copied, returning only relative paths.
     * Used by copyDirectoryStructure to get the structure without reading file contents.
     */
    fun collectFilePaths(files: List<FileRef>): CollectedFiles {
        val plannedFiles = collectPlannedFiles(files)
        return CollectedFiles(
            relativePaths = plannedFiles.map { it.relativePath },
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

    private data class PlannedFile(
        val fileRef: FileRef,
        val relativePath: String
    )
}

internal data class CollectedFiles(
    val relativePaths: List<String>,
    val fileLimitReached: Boolean
)
