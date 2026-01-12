package io.github.ceracharlescc.copy4lm.infrastructure.awt

import io.github.ceracharlescc.copy4lm.application.port.ClipboardGateway
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * AWT adapter for ClipboardGateway port.
 */
class AwtClipboardGateway : ClipboardGateway {
    override fun copy(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }
}
