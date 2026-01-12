package io.github.ceracharlescc.copy4lm.utils

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project

object NotificationUtil {
    private const val GROUP_ID = "Copy File Content"
    private const val SETTINGS_NAME = "Copy File Content Settings"

    fun show(project: Project?, message: String, type: NotificationType): Notification {
        val group = NotificationGroupManager.getInstance().getNotificationGroup(GROUP_ID)
        val notification = group.createNotification(message, type).setImportant(true)
        notification.notify(project)
        return notification
    }

    fun showWithSettingsAction(project: Project?, message: String, type: NotificationType): Notification {
        val group = NotificationGroupManager.getInstance().getNotificationGroup(GROUP_ID)
        val notification = group.createNotification(message, type).setImportant(true)

        notification.addAction(
            NotificationAction.createSimple("Go to Settings") {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, SETTINGS_NAME)
            }
        )

        notification.notify(project)
        return notification
    }
}
