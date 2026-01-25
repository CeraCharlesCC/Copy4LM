package io.github.ceracharlescc.copy4lm.application.usecase

import io.github.ceracharlescc.copy4lm.application.interactor.CopyFilesInteractor
import io.github.ceracharlescc.copy4lm.application.interactor.FileCollector
import io.github.ceracharlescc.copy4lm.application.port.FileGateway
import io.github.ceracharlescc.copy4lm.application.port.FileRef
import io.github.ceracharlescc.copy4lm.application.port.LoggerPort
import io.github.ceracharlescc.copy4lm.domain.vo.CopyOptions
import io.github.ceracharlescc.copy4lm.domain.vo.CopyResult
import io.github.ceracharlescc.copy4lm.domain.vo.toFileCollectionOptions

class CopyFilesUseCase(
    private val fileGateway: FileGateway,
    private val logger: LoggerPort
) {
    fun execute(files: List<FileRef>, options: CopyOptions): CopyResult {
        val collector = FileCollector(fileGateway, logger, options.toFileCollectionOptions())
        val interactor = CopyFilesInteractor(fileGateway, options, collector)
        return interactor.run(files)
    }
}
