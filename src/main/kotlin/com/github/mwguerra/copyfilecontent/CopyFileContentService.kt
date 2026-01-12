package com.github.mwguerra.copyfilecontent

import com.github.mwguerra.copyfilecontent.utils.ClipboardUtil
import com.github.mwguerra.copyfilecontent.utils.NotificationUtil
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class CopyFileContentService(private val project: Project) {

    private val logger = Logger.getInstance(CopyFileContentService::class.java)

    fun copy(files: Array<VirtualFile>) {
        val state = try {
            CopyFileContentSettings.getInstance(project).state
        } catch (t: Throwable) {
            NotificationUtil.show(project, "Failed to load settings.", NotificationType.ERROR)
            return
        }

        val repoRoot = ProjectRootManager.getInstance(project).contentRoots.firstOrNull()
        val context = CopyContext(project, state, repoRoot)

        val outcome = CopyRunner(context, logger).run(files)

        ClipboardUtil.copyToClipboard(outcome.clipboardText)

        if (outcome.fileLimitReached) {
            val msg = """
                <html>
                <b>File Limit Reached:</b> The file limit of ${state.fileCountLimit} files was reached.
                </html>
            """.trimIndent()
            NotificationUtil.showWithSettingsAction(project, msg, NotificationType.WARNING)
        }

        if (state.showCopyNotification) {
            val fileCountMessage =
                if (outcome.copiedFileCount == 1) "1 file copied." else "${outcome.copiedFileCount} files copied."

            val stats = outcome.stats
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

    companion object {
        fun getInstance(project: Project): CopyFileContentService = project.service()
    }
}

private data class CopyContext(
    val project: Project,
    val settings: CopyFileContentSettings.State,
    val repositoryRoot: VirtualFile?
)

private data class CopyOutcome(
    val clipboardText: String,
    val copiedFileCount: Int,
    val stats: CopyStats,
    val fileLimitReached: Boolean
)

private class CopyRunner(
    private val ctx: CopyContext,
    private val logger: Logger
) {
    private val settings = ctx.settings

    private val parts = mutableListOf<String>()
    private val seenRelativePaths = mutableSetOf<String>()

    private var copiedFileCount = 0
    private var fileLimitReached = false

    private val stats = CopyStats()

    fun run(files: Array<VirtualFile>): CopyOutcome {
        parts.add(settings.preText)

        for (vf in files) {
            visit(vf)
            if (fileLimitReached) break
        }

        parts.add(settings.postText)

        return CopyOutcome(
            clipboardText = parts.joinToString(separator = "\n"),
            copiedFileCount = copiedFileCount,
            stats = stats,
            fileLimitReached = fileLimitReached
        )
    }

    private fun visit(file: VirtualFile) {
        if (shouldStop()) return

        if (file.isDirectory) {
            for (child in file.children) {
                visit(child)
                if (fileLimitReached) return
            }
            return
        }

        copyFile(file)
    }

    private fun copyFile(file: VirtualFile) {
        if (shouldStop()) return

        if (!passesFilenameFilter(file)) {
            logger.info("Skipping file: ${file.name} - Extension does not match any filter")
            return
        }

        if (isBinaryFile(file) || isTooLarge(file)) {
            logger.info("Skipping file: ${file.name} - Binary or size limit exceeded")
            return
        }

        val relativePath = relativePathOf(file)

        // Skip already copied files
        if (!seenRelativePaths.add(relativePath)) {
            logger.info("Skipping already copied file: $relativePath")
            return
        }

        val content = readFileText(file)
        val header = settings.headerFormat.replace("\$FILE_PATH", relativePath)

        parts.add(header)
        parts.add(content)

        copiedFileCount++
        stats.add(content)

        if (settings.addExtraLineBetweenFiles && content.isNotEmpty()) {
            parts.add("")
        }
    }

    private fun shouldStop(): Boolean {
        if (!settings.setMaxFileCount) return false
        if (copiedFileCount < settings.fileCountLimit) return false
        fileLimitReached = true
        return true
    }

    private fun passesFilenameFilter(file: VirtualFile): Boolean {
        if (!settings.useFilenameFilters) return true
        return settings.filenameFilters.any { filter -> file.name.endsWith(filter) }
    }

    private fun isTooLarge(file: VirtualFile): Boolean {
        val maxBytes = settings.maxFileSizeKB.toLong() * 1024L
        return file.length > maxBytes
    }

    private fun isBinaryFile(file: VirtualFile): Boolean {
        return FileTypeManager.getInstance().getFileTypeByFile(file).isBinary
    }

    private fun relativePathOf(file: VirtualFile): String {
        val root = ctx.repositoryRoot
        return root?.let { VfsUtil.getRelativePath(file, it, '/') } ?: file.path
    }

    private fun readFileText(file: VirtualFile): String {
        return try {
            val cached = FileDocumentManager.getInstance().getCachedDocument(file)?.text

            if (!settings.strictMemoryRead) {
                cached ?: String(file.contentsToByteArray(), Charsets.UTF_8)
            } else {
                val open = FileEditorManager.getInstance(ctx.project).isFileOpen(file)
                if (open) cached ?: String(file.contentsToByteArray(), Charsets.UTF_8)
                else String(file.contentsToByteArray(), Charsets.UTF_8)
            }
        } catch (t: Throwable) {
            logger.error("Failed to read file contents for ${file.path}: ${t.message}", t)
            ""
        }
    }
}

private class CopyStats {
    var totalChars: Int = 0
        private set
    var totalLines: Int = 0
        private set
    var totalWords: Int = 0
        private set
    var totalTokens: Int = 0
        private set

    fun add(content: String) {
        totalChars += content.length
        totalLines += if (content.isEmpty()) 0 else content.count { it == '\n' } + 1
        totalWords += content.split(Regex("\\s+")).count { it.isNotBlank() }
        totalTokens += estimateTokens(content)
    }

    private fun estimateTokens(content: String): Int {
        val words = content.split(Regex("\\s+")).count { it.isNotBlank() }
        val punctuation = Regex("[;{}()\\[\\],]").findAll(content).count()
        return words + punctuation
    }
}
