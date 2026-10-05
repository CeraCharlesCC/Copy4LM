package io.github.ceracharlescc.copy4lm.ui

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.CommonDataKeys
import io.github.ceracharlescc.copy4lm.CopyFileContentService

internal abstract class CopyPathsAction(private val absolutePaths: Boolean) : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
            ?: e.getData(CommonDataKeys.VIRTUAL_FILE)?.let { arrayOf(it) }
            ?: return
        CopyFileContentService.getInstance(project).copyPaths(files, absolutePaths)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null &&
            (!e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY).isNullOrEmpty() ||
                e.getData(CommonDataKeys.VIRTUAL_FILE) != null)
    }
}

internal class CopyRelativePathsAction : CopyPathsAction(absolutePaths = false)

internal class CopyAbsolutePathsAction : CopyPathsAction(absolutePaths = true)
