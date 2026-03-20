package com.example.logger

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Logger {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    enum class Level {
        INFO, WARN, ERROR, DEBUG
    }

    fun log(level: Level, message: String, throwable: Throwable? = null) {
        val timestamp = LocalDateTime.now().format(formatter)
        val threadName = Thread.currentThread().name
        val logMessage = "[$timestamp] [$threadName] [$level] $message"

        when (level) {
            Level.INFO -> println("\u001B[32m$logMessage\u001B[0m")
            Level.WARN -> println("\u001B[33m$logMessage\u001B[0m")
            Level.ERROR -> System.err.println("\u001B[31m$logMessage\u001B[0m")
            Level.DEBUG -> println("\u001B[36m$logMessage\u001B[0m")
        }

        throwable?.printStackTrace()
    }

    fun info(message: String) = log(Level.INFO, message)
    fun warn(message: String) = log(Level.WARN, message)
    fun error(message: String, throwable: Throwable? = null) = log(Level.ERROR, message, throwable)
    fun debug(message: String) = log(Level.DEBUG, message)
}