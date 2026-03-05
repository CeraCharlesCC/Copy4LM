package io.github.ceracharlescc.copy4lm.infrastructure.intellij

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import io.github.ceracharlescc.copy4lm.application.port.FileGateway
import io.github.ceracharlescc.copy4lm.application.port.FileRef

/**
 * Wrapper around VirtualFile implementing FileRef.
 */
class VirtualFileRef(val virtualFile: VirtualFile) : FileRef {
    override val name: String get() = virtualFile.name
    override val path: String get() = virtualFile.path
    override val isDirectory: Boolean get() = virtualFile.isDirectory
}

/**
 * IntelliJ adapter for FileGateway port.
 * Wraps VirtualFile, FileTypeManager, FileDocumentManager, etc.
 */
internal class IntelliJFileGateway(
    private val project: Project,
    private val contentRoots: List<VirtualFile>,
    private val logger: Logger
) : FileGateway {
    private val gitIgnoreCache = mutableMapOf<VirtualFile, Boolean>()
    private val sortedContentRoots = contentRoots.sortedByDescending { it.path.length }

    override fun childrenOf(dir: FileRef): List<FileRef> {
        val vf = (dir as VirtualFileRef).virtualFile
        return vf.children.map { VirtualFileRef(it) }
    }

    override fun readText(file: FileRef, strictMemoryRead: Boolean): String? {
        val vf = (file as VirtualFileRef).virtualFile
        return try {
            val cached = FileDocumentManager.getInstance().getCachedDocument(vf)?.text
            val diskText = { String(vf.contentsToByteArray(), vf.charset) }

            if (!strictMemoryRead) {
                cached ?: diskText()
            } else {
                val isOpen = FileEditorManager.getInstance(project).isFileOpen(vf)
                if (isOpen) cached ?: diskText()
                else diskText()
            }
        } catch (t: Throwable) {
            logger.error("Failed to read file contents for ${vf.path}: ${t.message}", t)
            null
        }
    }

    override fun isBinary(file: FileRef): Boolean {
        val vf = (file as VirtualFileRef).virtualFile
        return FileTypeManager.getInstance().getFileTypeByFile(vf).isBinary
    }

    override fun sizeBytes(file: FileRef): Long {
        val vf = (file as VirtualFileRef).virtualFile
        return vf.length
    }

    override fun relativePath(file: FileRef): String {
        val vf = (file as VirtualFileRef).virtualFile
        val contentRoot = sortedContentRoots.firstOrNull { VfsUtilCore.isAncestor(it, vf, false) } ?: return vf.path
        val relativePath = VfsUtilCore.getRelativePath(vf, contentRoot, '/') ?: return vf.path
        return if (sortedContentRoots.size > 1) {
            "${contentRoot.name}/$relativePath"
        } else {
            relativePath
        }
    }

    override fun isGitIgnored(file: FileRef): Boolean {
        val vf = (file as VirtualFileRef).virtualFile
        return gitIgnoreCache.getOrPut(vf) {
            runCatching {
                ChangeListManager.getInstance(project).isIgnoredFile(vf)
            }.getOrElse { throwable ->
                logger.warn("Failed to resolve VCS ignore status for ${vf.path}: ${throwable.message}", throwable)
                false
            }
        }
    }

    companion object {
        /**
         * Converts an array of VirtualFiles to a list of FileRefs.
         */
        fun toFileRefs(files: Array<VirtualFile>): List<FileRef> {
            return files.map { VirtualFileRef(it) }
        }
    }
}
