package io.github.ceracharlescc.copy4lm.domain

internal enum class NotificationKind {
    Information,
    Warning,
    Error
}

internal data class NotificationPayload(
    val message: String,
    val kind: NotificationKind
)
