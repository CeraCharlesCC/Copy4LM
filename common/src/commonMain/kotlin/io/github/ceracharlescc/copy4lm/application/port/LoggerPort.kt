package io.github.ceracharlescc.copy4lm.application.port

/**
 * Port for logging operations.
 */
interface LoggerPort {
    fun info(message: String)
    fun error(message: String, throwable: Throwable? = null)
}
