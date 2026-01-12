package io.github.ceracharlescc.copy4lm.utils

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

object ClipboardUtil {
    fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }
}
