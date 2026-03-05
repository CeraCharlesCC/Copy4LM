package io.github.ceracharlescc.copy4lm.application.usecase

import io.github.ceracharlescc.copy4lm.application.interactor.FileCollector
import io.github.ceracharlescc.copy4lm.application.port.FileGateway
import io.github.ceracharlescc.copy4lm.application.port.FileRef
import io.github.ceracharlescc.copy4lm.application.port.LoggerPort
import io.github.ceracharlescc.copy4lm.domain.service.DirectoryStructureBuilder
import io.github.ceracharlescc.copy4lm.domain.service.DirectoryStructureTextFormatter
import io.github.ceracharlescc.copy4lm.domain.vo.DirectoryStructureOptions
import io.github.ceracharlescc.copy4lm.domain.vo.DirectoryStructureResult
import io.github.ceracharlescc.copy4lm.domain.vo.toFileCollectionOptions

class CopyDirectoryStructureUseCase(
    private val fileGateway: FileGateway,
    private val logger: LoggerPort
) {
    fun execute(files: List<FileRef>, options: DirectoryStructureOptions): DirectoryStructureResult {
        val collector = FileCollector(fileGateway, logger, options.toFileCollectionOptions())
        val collected = collector.collect(files)
        val directoryStructure = DirectoryStructureBuilder.build(
            rootName = options.projectName,
            relativePaths = collected.relativePaths
        )
        return DirectoryStructureResult(
            clipboardText = DirectoryStructureTextFormatter.format(
                preText = options.preText,
                postText = options.postText,
                projectName = options.projectName,
                directoryStructure = directoryStructure
            ),
            collectedFileCount = collected.files.size,
            fileLimitReached = collected.fileLimitReached
        )
    }
}
