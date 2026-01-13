package io.github.ceracharlescc.copy4lm.application.interactor

import io.github.ceracharlescc.copy4lm.application.port.FileGateway
import io.github.ceracharlescc.copy4lm.application.port.FileRef
import io.github.ceracharlescc.copy4lm.application.port.LoggerPort
import io.github.ceracharlescc.copy4lm.domain.clipboard.ClipboardTextBuilder
import io.github.ceracharlescc.copy4lm.domain.CopyOptions
import io.github.ceracharlescc.copy4lm.domain.CopyResult
import io.github.ceracharlescc.copy4lm.domain.CopyStats
import io.github.ceracharlescc.copy4lm.domain.formatting.PlaceholderFormatter

internal class CopyFilesInteractor(
    private val fileGateway: FileGateway,
    private val logger: LoggerPort,
    private val options: CopyOptions
) {
    private val textBuilder = ClipboardTextBuilder(
        preText = PlaceholderFormatter.format(options.preText, options.projectName),
        postText = PlaceholderFormatter.format(options.postText, options.projectName),
        addExtraLineBetweenFiles = options.addExtraLineBetweenFiles
    )
    private val seenRelativePaths = mutableSetOf<String>()
    private var copiedFileCount = 0
    private var fileLimitReached = false
    private val stats = CopyStats()

    fun run(files: List<FileRef>): CopyResult {
        for (file in files) {
            visit(file)
            if (fileLimitReached) break
        }

        return CopyResult(
            clipboardText = textBuilder.build(),
            copiedFileCount = copiedFileCount,
            stats = stats,
            fileLimitReached = fileLimitReached
        )
    }

    private fun visit(file: FileRef) {
        if (shouldStop()) return

        if (file.isDirectory) {
            for (child in fileGateway.childrenOf(file)) {
                visit(child)
                if (fileLimitReached) return
            }
            return
        }

        copyFile(file)
    }

    private fun copyFile(file: FileRef) {
        if (shouldStop()) return

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

        val content = fileGateway.readText(file, options.strictMemoryRead)
        val header = PlaceholderFormatter.format(options.headerFormat, options.projectName, relativePath)
        val footer = PlaceholderFormatter.format(options.footerFormat, options.projectName, relativePath)

        textBuilder.addFile(header, content, footer)
        copiedFileCount++
        stats.add(content)
    }

    private fun shouldStop(): Boolean {
        if (!options.setMaxFileCount) return false
        if (copiedFileCount < options.fileCountLimit) return false
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