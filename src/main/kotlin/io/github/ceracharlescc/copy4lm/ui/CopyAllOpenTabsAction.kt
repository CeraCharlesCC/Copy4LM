package io.github.ceracharlescc.copy4lm.ui

import io.github.ceracharlescc.copy4lm.CopyFileContentService
import io.github.ceracharlescc.copy4lm.utils.NotificationUtil
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager

class CopyAllOpenTabsAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val openFiles = FileEditorManager.getInstance(project).openFiles
        if (openFiles.isEmpty()) {
            NotificationUtil.show(
                project = project,
                message = "No open tabs found to copy.",
                type = NotificationType.INFORMATION
            )
            return
        }

        CopyFileContentService.getInstance(project).copy(openFiles)
    }
}
