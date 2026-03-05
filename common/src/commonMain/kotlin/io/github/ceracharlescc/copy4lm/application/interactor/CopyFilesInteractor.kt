package io.github.ceracharlescc.copy4lm.application.interactor

import io.github.ceracharlescc.copy4lm.application.port.FileGateway
import io.github.ceracharlescc.copy4lm.application.port.FileRef
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
        val readableFiles = mutableListOf<ReadableFile>()
        var failedFileCount = 0

        for (planned in collected.files) {
            val content = fileGateway.readText(planned.fileRef, options.strictMemoryRead)
            if (content == null) {
                failedFileCount++
                continue
            }
            readableFiles += ReadableFile(
                relativePath = planned.relativePath,
                content = content
            )
        }

        // Build directory structure only from files that will actually be copied.
        val directoryStructure = DirectoryStructureBuilder.build(
            rootName = options.projectName,
            relativePaths = readableFiles.map { it.relativePath }
        )

        val textBuilder = ClipboardTextBuilder(
            preText = PlaceholderFormatter.format(options.preText, options.projectName, directoryStructure = directoryStructure),
            postText = PlaceholderFormatter.format(options.postText, options.projectName, directoryStructure = directoryStructure),
            addExtraLineBetweenFiles = options.addExtraLineBetweenFiles
        )

        var stats = CopyStats()

        for (planned in readableFiles) {
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
        }

        return CopyResult(
            clipboardText = textBuilder.build(),
            copiedFileCount = readableFiles.size,
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
