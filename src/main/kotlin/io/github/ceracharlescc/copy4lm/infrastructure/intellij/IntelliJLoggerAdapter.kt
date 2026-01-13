package io.github.ceracharlescc.copy4lm.infrastructure.intellij

import com.intellij.openapi.diagnostic.Logger
import io.github.ceracharlescc.copy4lm.application.port.LoggerPort

/**
 * IntelliJ adapter for LoggerPort.
 */
internal class IntelliJLoggerAdapter(private val logger: Logger) : LoggerPort {
    override fun info(message: String) {
        logger.info(message)
    }

    override fun error(message: String, throwable: Throwable?) {
        if (throwable != null) {
            logger.error(message, throwable)
        } else {
            logger.error(message)
        }
    }
}
