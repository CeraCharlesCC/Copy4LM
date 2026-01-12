package com.github.mwguerra.copyfilecontent.application.port

/**
 * Port for logging operations.
 * Abstracts IntelliJ's Logger.
 */
interface LoggerPort {
    fun info(message: String)
    fun error(message: String, throwable: Throwable? = null)
}
