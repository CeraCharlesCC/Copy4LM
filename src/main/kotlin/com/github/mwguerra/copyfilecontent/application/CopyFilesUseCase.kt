package com.github.mwguerra.copyfilecontent.application

import com.github.mwguerra.copyfilecontent.application.port.FileGateway
import com.github.mwguerra.copyfilecontent.application.port.FileRef
import com.github.mwguerra.copyfilecontent.application.port.LoggerPort
import com.github.mwguerra.copyfilecontent.domain.ClipboardTextBuilder
import com.github.mwguerra.copyfilecontent.domain.CopyOptions
import com.github.mwguerra.copyfilecontent.domain.CopyResult
import com.github.mwguerra.copyfilecontent.domain.CopyStats
import com.github.mwguerra.copyfilecontent.domain.FooterFormatter
import com.github.mwguerra.copyfilecontent.domain.HeaderFormatter

class CopyFilesUseCase(
    private val fileGateway: FileGateway,
    private val logger: LoggerPort
) {
    fun execute(files: List<FileRef>, options: CopyOptions): CopyResult {
        val runner = CopyRunner(fileGateway, logger, options)
        return runner.run(files)
    }
}

private class CopyRunner(
    private val fileGateway: FileGateway,
    private val logger: LoggerPort,
    private val options: CopyOptions
) {
    private val textBuilder = ClipboardTextBuilder(
        preText = options.preText,
        postText = options.postText,
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

        // Skip already copied files
        if (!seenRelativePaths.add(relativePath)) {
            logger.info("Skipping already copied file: $relativePath")
            return
        }

        val content = fileGateway.readText(file, options.strictMemoryRead)
        val header = HeaderFormatter.format(options.headerFormat, relativePath)
        val footer = FooterFormatter.format(options.footerFormat, relativePath)

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
