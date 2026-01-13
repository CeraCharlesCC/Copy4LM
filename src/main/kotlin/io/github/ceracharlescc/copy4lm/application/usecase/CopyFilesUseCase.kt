package io.github.ceracharlescc.copy4lm.application.usecase

import io.github.ceracharlescc.copy4lm.application.interactor.CopyFilesInteractor
import io.github.ceracharlescc.copy4lm.application.port.FileGateway
import io.github.ceracharlescc.copy4lm.application.port.FileRef
import io.github.ceracharlescc.copy4lm.application.port.LoggerPort
import io.github.ceracharlescc.copy4lm.domain.*

internal class CopyFilesUseCase(
    private val fileGateway: FileGateway,
    private val logger: LoggerPort
) {
    fun execute(files: List<FileRef>, options: CopyOptions): CopyResult {
        val interactor = CopyFilesInteractor(fileGateway, logger, options)
        return interactor.run(files)
    }
}
