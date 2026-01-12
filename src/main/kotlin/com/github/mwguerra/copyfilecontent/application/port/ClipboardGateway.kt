package com.github.mwguerra.copyfilecontent.application.port

/**
 * Port for clipboard operations.
 * Abstracts AWT clipboard APIs.
 */
interface ClipboardGateway {
    /**
     * Copies text to the system clipboard.
     */
    fun copy(text: String)
}
