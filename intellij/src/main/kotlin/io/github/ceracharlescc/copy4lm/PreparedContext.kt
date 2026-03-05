package io.github.ceracharlescc.copy4lm

import io.github.ceracharlescc.copy4lm.application.port.FileGateway
import io.github.ceracharlescc.copy4lm.application.port.FileRef
import io.github.ceracharlescc.copy4lm.application.port.LoggerPort

internal data class PreparedContext(
    val state: Copy4LMSettings.State,
    val projectName: String,
    val fileGateway: FileGateway,
    val loggerPort: LoggerPort,
    val fileRefs: List<FileRef>
)
