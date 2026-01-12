package com.github.mwguerra.copyfilecontent.application.port

/**
 * Port for clipboard operations.
 */
interface ClipboardGateway {
    /**
     * Copies text to the system clipboard.
     */
    fun copy(text: String)
}
