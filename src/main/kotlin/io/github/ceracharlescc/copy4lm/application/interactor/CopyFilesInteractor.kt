package io.github.ceracharlescc.copy4lm.application.interactor

import io.github.ceracharlescc.copy4lm.application.port.FileGateway
import io.github.ceracharlescc.copy4lm.application.port.FileRef
import io.github.ceracharlescc.copy4lm.domain.service.ClipboardTextBuilder
import io.github.ceracharlescc.copy4lm.domain.vo.CopyOptions
import io.github.ceracharlescc.copy4lm.domain.vo.CopyResult
import io.github.ceracharlescc.copy4lm.domain.vo.CopyStats
import io.github.ceracharlescc.copy4lm.domain.service.DirectoryStructureBuilder
import io.github.ceracharlescc.copy4lm.domain.service.PlaceholderFormatter

internal class CopyFilesInteractor(
    private val fileGateway: FileGateway,
    private val options: CopyOptions,
    private val fileCollector: FileCollector
) {
    fun run(files: List<FileRef>): CopyResult {
        val collected = fileCollector.collect(files)
        val plannedFiles = collected.files

        // Build directory structure from collected files
        val directoryStructure = DirectoryStructureBuilder.build(
            rootName = options.projectName,
            relativePaths = collected.relativePaths
        )

        // Phase 2: Render clipboard text with directory structure context
        val textBuilder = ClipboardTextBuilder(
            preText = PlaceholderFormatter.format(options.preText, options.projectName, directoryStructure = directoryStructure),
            postText = PlaceholderFormatter.format(options.postText, options.projectName, directoryStructure = directoryStructure),
            addExtraLineBetweenFiles = options.addExtraLineBetweenFiles
        )

        var stats = CopyStats()

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
            stats = stats.plus(content)
        }

        return CopyResult(
            clipboardText = textBuilder.build(),
            copiedFileCount = plannedFiles.size,
            stats = stats,
            fileLimitReached = collected.fileLimitReached
        )
    }
}
