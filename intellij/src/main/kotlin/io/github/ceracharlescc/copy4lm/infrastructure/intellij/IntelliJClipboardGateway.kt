package io.github.ceracharlescc.copy4lm.infrastructure.intellij

import com.intellij.openapi.ide.CopyPasteManager
import io.github.ceracharlescc.copy4lm.application.port.ClipboardGateway
import java.awt.datatransfer.StringSelection

/**
 * Implementation of [ClipboardGateway] for IntelliJ platform.
 */
internal class IntelliJClipboardGateway : ClipboardGateway {
    override fun copy(text: String) {
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }
}