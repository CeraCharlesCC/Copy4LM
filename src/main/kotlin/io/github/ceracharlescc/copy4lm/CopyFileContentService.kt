package io.github.ceracharlescc.copy4lm

import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import io.github.ceracharlescc.copy4lm.application.interactor.FileCollector
import io.github.ceracharlescc.copy4lm.application.usecase.CopyFilesUseCase
import io.github.ceracharlescc.copy4lm.domain.service.DirectoryStructureBuilder
import io.github.ceracharlescc.copy4lm.domain.vo.ClipboardCopyOutcome
import io.github.ceracharlescc.copy4lm.domain.vo.NotificationKind
import io.github.ceracharlescc.copy4lm.domain.vo.NotificationPayload
import io.github.ceracharlescc.copy4lm.infrastructure.intellij.IntelliJClipboardGateway
import io.github.ceracharlescc.copy4lm.infrastructure.intellij.IntelliJFileGateway
import io.github.ceracharlescc.copy4lm.infrastructure.intellij.IntelliJLoggerAdapter
import io.github.ceracharlescc.copy4lm.infrastructure.intellij.IntelliJSettingsMapper
import io.github.ceracharlescc.copy4lm.utils.NotificationUtil


@Service(Service.Level.PROJECT)
internal class CopyFileContentService(private val project: Project) {

    private val logger = Logger.getInstance(CopyFileContentService::class.java)
    private val clipboardGateway = IntelliJClipboardGateway()

    fun copy(files: Array<VirtualFile>) {
        val context = prepareContext(files) ?: return
        val options = IntelliJSettingsMapper.toCopyOptions(context.state, context.projectName)
        val useCase = CopyFilesUseCase(context.fileGateway, context.loggerPort)
        val result = useCase.execute(context.fileRefs, options)
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

        val outcome = ClipboardCopyOutcome(
            text = result.clipboardText,
            fileLimitReached = result.fileLimitReached,
            successNotifications = listOf(
                NotificationPayload(statisticsMessage, NotificationKind.Information),
                NotificationPayload("<html><b>$fileCountMessage</b></html>", NotificationKind.Information)
            )
        )

        copyToClipboardAndNotify(outcome, context.state)
    }

    fun copyDirectoryStructure(files: Array<VirtualFile>) {
        val context = prepareContext(files) ?: return
        val collectionOptions = IntelliJSettingsMapper.toFileCollectionOptions(context.state)
        val collector = FileCollector(context.fileGateway, context.loggerPort, collectionOptions)
        val collected = collector.collect(context.fileRefs)
        val directoryStructure = DirectoryStructureBuilder.build(
            rootName = context.projectName,
            relativePaths = collected.relativePaths
        )
        val finalText = IntelliJSettingsMapper.formatDirectoryStructureText(
            state = context.state,
            projectName = context.projectName,
            directoryStructure = directoryStructure
        )
        val outcome = ClipboardCopyOutcome(
            text = finalText,
            fileLimitReached = collected.fileLimitReached,
            successNotifications = listOf(
                NotificationPayload("<html><b>Directory structure copied.</b></html>", NotificationKind.Information)
            )
        )
        copyToClipboardAndNotify(outcome, context.state)
    }

    private fun prepareContext(files: Array<VirtualFile>): PreparedContext? {
        val state = try {
            Copy4LMSettings.getInstance(project).state
        } catch (_: Throwable) {
            NotificationUtil.show(project, "Failed to load settings.", NotificationType.ERROR)
            return null
        }

        val repoRoot = ProjectRootManager.getInstance(project).contentRoots.firstOrNull()
        val projectName = repoRoot?.name ?: project.name
        val fileGateway = IntelliJFileGateway(project, repoRoot, logger)
        val loggerPort = IntelliJLoggerAdapter(logger)
        val fileRefs = IntelliJFileGateway.toFileRefs(files)

        return PreparedContext(
            state = state,
            repoRoot = repoRoot,
            projectName = projectName,
            fileGateway = fileGateway,
            loggerPort = loggerPort,
            fileRefs = fileRefs
        )
    }

    private fun copyToClipboardAndNotify(outcome: ClipboardCopyOutcome, state: Copy4LMSettings.State) {
        clipboardGateway.copy(outcome.text)
        notifyLimitReachedIfNeeded(state, outcome.fileLimitReached)
        if (!state.common.showCopyNotification) return
        for (notification in outcome.successNotifications) {
            NotificationUtil.show(project, notification.message, toNotificationType(notification.kind))
        }
    }

    private fun notifyLimitReachedIfNeeded(state: Copy4LMSettings.State, fileLimitReached: Boolean) {
        if (!fileLimitReached) return
        val msg = """
            <html>
            <b>File Limit Reached:</b> The file limit of ${state.common.fileCountLimit} files was reached.
            </html>
        """.trimIndent()
        NotificationUtil.showWithSettingsAction(project, msg, NotificationType.WARNING)
    }

    private fun toNotificationType(kind: NotificationKind): NotificationType =
        when (kind) {
            NotificationKind.Information -> NotificationType.INFORMATION
            NotificationKind.Warning -> NotificationType.WARNING
            NotificationKind.Error -> NotificationType.ERROR
        }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): CopyFileContentService =
            project.getService(CopyFileContentService::class.java)
    }
}
