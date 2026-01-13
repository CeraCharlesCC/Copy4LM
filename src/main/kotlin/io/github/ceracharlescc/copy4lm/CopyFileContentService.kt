package io.github.ceracharlescc.copy4lm

import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import io.github.ceracharlescc.copy4lm.application.interactor.CopyFilesInteractor
import io.github.ceracharlescc.copy4lm.application.usecase.CopyFilesUseCase
import io.github.ceracharlescc.copy4lm.domain.directory.DirectoryStructureBuilder
import io.github.ceracharlescc.copy4lm.infrastructure.intellij.IntelliJClipboardGateway
import io.github.ceracharlescc.copy4lm.infrastructure.intellij.IntelliJFileGateway
import io.github.ceracharlescc.copy4lm.infrastructure.intellij.IntelliJLoggerAdapter
import io.github.ceracharlescc.copy4lm.infrastructure.intellij.IntelliJSettingsMapper
import io.github.ceracharlescc.copy4lm.utils.NotificationUtil


@Service(Service.Level.PROJECT)
internal class CopyFileContentService(private val project: Project) {

    private val logger = Logger.getInstance(CopyFileContentService::class.java)
    private val clipboardGateway =
        IntelliJClipboardGateway()

    fun copy(files: Array<VirtualFile>) {
        // Load settings
        val state = try {
            CopyFileContentSettings.getInstance(project).state
        } catch (_: Throwable) {
            NotificationUtil.show(project, "Failed to load settings.", NotificationType.ERROR)
            return
        }

        // Resolve repository root
        val repoRoot = ProjectRootManager.getInstance(project).contentRoots.firstOrNull()

        // Build adapters
        val fileGateway = IntelliJFileGateway(project, repoRoot, logger)
        val loggerPort = IntelliJLoggerAdapter(logger)

        // Map settings to domain options
        val options = IntelliJSettingsMapper.toCopyOptions(state)
            .copy(projectName = repoRoot?.name ?: project.name)

        // Convert VirtualFiles to FileRefs
        val fileRefs = IntelliJFileGateway.toFileRefs(files)

        // Execute use case
        val useCase = CopyFilesUseCase(fileGateway, loggerPort)
        val result = useCase.execute(fileRefs, options)

        // Copy to clipboard
        clipboardGateway.copy(result.clipboardText)

        // Handle notifications
        if (result.fileLimitReached) {
            val msg = """
                <html>
                <b>File Limit Reached:</b> The file limit of ${state.fileCountLimit} files was reached.
                </html>
            """.trimIndent()
            NotificationUtil.showWithSettingsAction(project, msg, NotificationType.WARNING)
        }

        if (state.showCopyNotification) {
            val fileCountMessage =
                if (result.copiedFileCount == 1) "1 file copied." else "${result.copiedFileCount} files copied."

            val stats = result.stats
            val statisticsMessage = """
                <html>
                Total characters: ${stats.totalChars}<br>
                Total lines: ${stats.totalLines}<br>
                Total words: ${stats.totalWords}<br>
                Estimated tokens: ${stats.totalTokens}
                </html>
            """.trimIndent()

            NotificationUtil.show(project, statisticsMessage, NotificationType.INFORMATION)
            NotificationUtil.show(project, "<html><b>$fileCountMessage</b></html>", NotificationType.INFORMATION)
        }
    }

    fun copyDirectoryStructure(files: Array<VirtualFile>) {
        // Load settings
        val state = try {
            CopyFileContentSettings.getInstance(project).state
        } catch (_: Throwable) {
            NotificationUtil.show(project, "Failed to load settings.", NotificationType.ERROR)
            return
        }

        // Resolve repository root
        val repoRoot = ProjectRootManager.getInstance(project).contentRoots.firstOrNull()

        // Build adapters
        val fileGateway = IntelliJFileGateway(project, repoRoot, logger)
        val loggerPort = IntelliJLoggerAdapter(logger)

        // Map settings to domain options (use same filters/limits for consistency)
        val options = IntelliJSettingsMapper.toCopyOptions(state)
            .copy(projectName = repoRoot?.name ?: project.name)

        // Convert VirtualFiles to FileRefs
        val fileRefs = IntelliJFileGateway.toFileRefs(files)

        // Collect file paths using same logic as copy
        val interactor = CopyFilesInteractor(fileGateway, loggerPort, options)
        val collected = interactor.collectFilePaths(fileRefs)

        // Build directory structure
        val directoryStructure = DirectoryStructureBuilder.build(
            rootName = repoRoot?.name ?: project.name,
            relativePaths = collected.relativePaths
        )

        // Copy to clipboard
        clipboardGateway.copy(directoryStructure)

        // Show notification
        NotificationUtil.show(
            project,
            "<html><b>Directory structure copied.</b></html>",
            NotificationType.INFORMATION
        )

        if (collected.fileLimitReached) {
            val msg = """
                <html>
                <b>File Limit Reached:</b> The file limit of ${state.fileCountLimit} files was reached.
                </html>
            """.trimIndent()
            NotificationUtil.showWithSettingsAction(project, msg, NotificationType.WARNING)
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): CopyFileContentService =
            project.getService(CopyFileContentService::class.java)
    }
}
