package io.github.ceracharlescc.copy4lm.infrastructure.intellij

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VfsUtil
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
    private val repositoryRoot: VirtualFile?,
    private val logger: Logger
) : FileGateway {
    private val gitIgnoreCache = mutableMapOf<VirtualFile, Boolean>()

    override fun childrenOf(dir: FileRef): List<FileRef> {
        val vf = (dir as VirtualFileRef).virtualFile
        return vf.children.map { VirtualFileRef(it) }
    }

    override fun readText(file: FileRef, strictMemoryRead: Boolean): String {
        val vf = (file as VirtualFileRef).virtualFile
        return try {
            val cached = FileDocumentManager.getInstance().getCachedDocument(vf)?.text

            if (!strictMemoryRead) {
                cached ?: String(vf.contentsToByteArray(), Charsets.UTF_8)
            } else {
                val isOpen = FileEditorManager.getInstance(project).isFileOpen(vf)
                if (isOpen) cached ?: String(vf.contentsToByteArray(), Charsets.UTF_8)
                else String(vf.contentsToByteArray(), Charsets.UTF_8)
            }
        } catch (t: Throwable) {
            logger.error("Failed to read file contents for ${vf.path}: ${t.message}", t)
            ""
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
        return repositoryRoot?.let { VfsUtil.getRelativePath(vf, it, '/') } ?: vf.path
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
