package io.github.ceracharlescc.copy4lm.domain

internal data class ClipboardCopyOutcome(
    val text: String,
    val fileLimitReached: Boolean,
    val successNotifications: List<NotificationPayload>
)
