package io.github.ceracharlescc.copy4lm.ui

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import io.github.ceracharlescc.copy4lm.CopyFileContentService
import io.github.ceracharlescc.copy4lm.utils.NotificationUtil

class CopyFileContentAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: run {
            NotificationUtil.show(
                project = null,
                message = "No project found. Action cannot proceed.",
                type = NotificationType.ERROR
            )
            return
        }

        val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: emptyArray()
        if (selectedFiles.isEmpty()) {
            NotificationUtil.show(
                project = project,
                message = "No files selected.",
                type = NotificationType.ERROR
            )
            return
        }

        CopyFileContentService.getInstance(project).copy(selectedFiles)
    }
}
