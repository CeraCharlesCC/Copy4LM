package io.github.ceracharlescc.copy4lm.domain.vo

internal data class ClipboardCopyOutcome(
    val text: String,
    val fileLimitReached: Boolean,
    val successNotifications: List<NotificationPayload>
)
