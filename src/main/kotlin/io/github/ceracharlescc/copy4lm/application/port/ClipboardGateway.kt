package io.github.ceracharlescc.copy4lm.application.port

/**
 * Port for clipboard operations.
 */
internal interface ClipboardGateway {
    /**
     * Copies text to the system clipboard.
     */
    fun copy(text: String)
}
