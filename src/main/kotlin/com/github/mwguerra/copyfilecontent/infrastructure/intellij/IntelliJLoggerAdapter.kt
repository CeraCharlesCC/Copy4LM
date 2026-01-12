package com.github.mwguerra.copyfilecontent.infrastructure.intellij

import com.github.mwguerra.copyfilecontent.application.port.LoggerPort
import com.intellij.openapi.diagnostic.Logger

/**
 * IntelliJ adapter for LoggerPort.
 */
class IntelliJLoggerAdapter(private val logger: Logger) : LoggerPort {
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
