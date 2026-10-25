package io.github.ceracharlescc.copy4lm.application.interactor

import io.github.ceracharlescc.copy4lm.application.port.FileGateway
import io.github.ceracharlescc.copy4lm.application.port.FileRef
import io.github.ceracharlescc.copy4lm.domain.constant.Placeholders
import io.github.ceracharlescc.copy4lm.domain.service.ClipboardTextBuilder
import io.github.ceracharlescc.copy4lm.domain.service.DirectoryStructureBuilder
import io.github.ceracharlescc.copy4lm.domain.service.PlaceholderFormatter
import io.github.ceracharlescc.copy4lm.domain.vo.CopyOptions
import io.github.ceracharlescc.copy4lm.domain.vo.CopyResult
import io.github.ceracharlescc.copy4lm.domain.vo.CopyStats

internal class CopyFilesInteractor(
    private val fileGateway: FileGateway,
    private val options: CopyOptions,
    private val fileCollector: FileCollector
) {
    fun run(files: List<FileRef>): CopyResult {
        val collected = fileCollector.collect(files)
        var failedFileCount = 0
        val readableFiles = collected.files.asSequence().mapNotNull { planned ->
            val content = fileGateway.readText(planned.fileRef, options.strictMemoryRead)
            if (content == null) {
                failedFileCount++
                null
            } else {
                ReadableFile(planned.relativePath, content)
            }
        }

        // Most templates need no tree: read, format and release each file in turn.
        // Tree placeholders require buffering so unreadable files can be excluded.
        val needsTree = listOf(
            options.preText, options.postText, options.headerFormat, options.footerFormat, options.projectName
        ).any { Placeholders.DIRECTORY_STRUCTURE in it } ||
            collected.files.any { Placeholders.DIRECTORY_STRUCTURE in it.relativePath }
        val filesToCopy: Sequence<ReadableFile>
        val directoryStructure: String
        if (needsTree) {
            val buffered = readableFiles.toList()
            directoryStructure = DirectoryStructureBuilder.build(options.projectName, buffered.map { it.relativePath })
            filesToCopy = buffered.asSequence()
        } else {
            directoryStructure = ""
            filesToCopy = readableFiles
        }

        val textBuilder = ClipboardTextBuilder(
            preText = PlaceholderFormatter.format(options.preText, options.projectName, directoryStructure = directoryStructure),
            postText = PlaceholderFormatter.format(options.postText, options.projectName, directoryStructure = directoryStructure),
            addExtraLineBetweenFiles = options.addExtraLineBetweenFiles
        )

        var stats = CopyStats()
        var copiedFileCount = 0

        for (planned in filesToCopy) {
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

            textBuilder.addFile(header, planned.content, footer)
            stats = stats.plus(planned.content)
            copiedFileCount++
        }

        return CopyResult(
            clipboardText = textBuilder.build(),
            copiedFileCount = copiedFileCount,
            failedFileCount = failedFileCount,
            stats = stats,
            fileLimitReached = collected.fileLimitReached
        )
    }

    private data class ReadableFile(
        val relativePath: String,
        val content: String
    )
}
