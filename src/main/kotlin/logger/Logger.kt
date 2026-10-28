package com.example.logger

import org.slf4j.LoggerFactory

object Logger {
    private val logger = LoggerFactory.getLogger("GameLogger")

    fun info(message: String) = logger.info(message)
    fun warn(message: String) = logger.warn(message)
    fun error(message: String, throwable: Throwable? = null) = logger.error(message, throwable)
    fun debug(message: String) = logger.debug(message)
}
